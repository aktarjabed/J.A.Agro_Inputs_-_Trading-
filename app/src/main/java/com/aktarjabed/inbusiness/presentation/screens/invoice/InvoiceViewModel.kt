package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.repository.ProductRepository
import com.aktarjabed.inbusiness.data.repository.BusinessRepository
import com.aktarjabed.inbusiness.domain.usecase.CreateInvoiceUseCase
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.invoice.GstCalculator
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val quotaGate: QuotaGate,
    private val createInvoiceUseCase: CreateInvoiceUseCase,
    private val calculateInvoiceTotalsUseCase: CalculateInvoiceTotalsUseCase,
    private val businessContext: BusinessContext,
    private val productRepository: ProductRepository,
    private val businessRepository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Initial)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    // UI state for inputs
    val customerName = MutableStateFlow("")
    val customerGSTIN = MutableStateFlow("")
    val buyerAddress = MutableStateFlow("")

    val sellerName = MutableStateFlow("")
    val sellerAddress = MutableStateFlow("")
    val sellerGstin = MutableStateFlow("")
    val supplyType = MutableStateFlow(SupplyType.UNKNOWN)

    // Items state
    private val _invoiceItems = MutableStateFlow<List<InvoiceItemInput>>(emptyList())
    val invoiceItems = _invoiceItems.asStateFlow()

    // Products for dropdown
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products = _products.asStateFlow()

    val amountPaid = MutableStateFlow(0.0)
    val paymentMethod = MutableStateFlow("NONE")

    init {
        // Load products for dropdown
        viewModelScope.launch {
            productRepository.getAllProducts().collect { productList ->
                _products.value = productList
            }
        }

        // Observe business data for Seller GSTIN
        viewModelScope.launch {
            try {
                val currentBusinessId = businessContext.activeBusinessId.first()
                val businessData = businessRepository.getBusinessDataById(currentBusinessId)
                if (businessData != null) {
                    sellerName.value = businessData.name
                    sellerAddress.value = businessData.address
                    if (!businessData.gstin.isNullOrBlank()) {
                        sellerGstin.value = businessData.gstin
                    }

                    // Re-evaluate supply type if customer GSTIN is already provided
                    if (customerGSTIN.value.isNotBlank() && sellerGstin.value.isNotBlank()) {
                        supplyType.value = GstCalculator.determineSupplyType(sellerGstin.value, customerGSTIN.value)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load business data for GSTIN", e)
            }
        }
    }

    fun checkQuotaAndPrepare() {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading

            try {
                val currentUserId = businessContext.currentUserId.first()

                // Peek without consuming
                val verdict = quotaGate.assertQuota(currentUserId, consume = false)

                when (verdict) {
                    is QuotaVerdict.Allowed -> {
                        val nextInvoiceNumber = "Invoice number will be assigned when saved"
                        _uiState.value = InvoiceUiState.CreateAllowed(
                            remainingToday = verdict.remaining,
                            invoiceNumber = nextInvoiceNumber
                        )
                    }
                    is QuotaVerdict.DailyCap -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    is QuotaVerdict.MonthlyCap -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    is QuotaVerdict.FreeExpired -> _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking quota", e)
                _uiState.value = InvoiceUiState.Error("Failed to check quota: ${e.message}")
            }
        }
    }

    fun updateCustomerData(name: String, gstin: String, address: String) {
        customerName.value = name
        customerGSTIN.value = gstin
        buyerAddress.value = address

        // Auto-detect supply type
        if (sellerGstin.value.isNotBlank() && gstin.isNotBlank()) {
            supplyType.value = GstCalculator.determineSupplyType(sellerGstin.value, gstin)
        }
    }

    fun setSupplyType(type: SupplyType) {
        supplyType.value = type
        recalculateItems()
    }

    fun addItem(item: InvoiceItemInput) {
        val currentItems = _invoiceItems.value.toMutableList()
        currentItems.add(item)
        _invoiceItems.value = currentItems
        recalculateItems()
    }

    fun removeItem(index: Int) {
        val currentItems = _invoiceItems.value.toMutableList()
        if (index in currentItems.indices) {
            currentItems.removeAt(index)
            _invoiceItems.value = currentItems
        }
    }

    private fun recalculateItems() {
        val currentSupplyType = supplyType.value
        if (currentSupplyType == SupplyType.UNKNOWN) return

        val updatedItems = _invoiceItems.value.map { input ->
            val taxResult = GstCalculator.calculateItemTaxes(quantity = input.quantity, unitPrice = input.pricePerUnit, gstPercentage = input.gstPercentage, supplyType = currentSupplyType)
            input.copy(taxResult = taxResult)
        }
        _invoiceItems.value = updatedItems
    }

    fun createInvoice() {
        viewModelScope.launch {
            if (_invoiceItems.value.isEmpty()) {
                _uiState.value = InvoiceUiState.Error("Please add at least one item")
                return@launch
            }

            if (supplyType.value == SupplyType.UNKNOWN) {
                _uiState.value = InvoiceUiState.Error("Please select a valid Supply Type (Intra/Inter State)")
                return@launch
            }

            _uiState.value = InvoiceUiState.Loading

            try {
                val idempotencyKey = java.util.UUID.randomUUID().toString()

                val rawItems = _invoiceItems.value.map { input ->
                    InvoiceItem(
                        description = input.description,
                        quantity = input.quantity,
                        pricePerUnit = input.pricePerUnit,
                        unitType = input.unitType,
                        gstPercentage = input.gstPercentage,
                        productId = input.productId
                    )
                }

                val calculationResult = calculateInvoiceTotalsUseCase(
                    items = rawItems,
                    supplyType = supplyType.value,
                    amountPaid = amountPaid.value
                )

                val result = createInvoiceUseCase(
                    sellerName = sellerName.value,
                    sellerAddress = sellerAddress.value,
                    sellerGSTIN = sellerGstin.value.takeIf { it.isNotBlank() },
                    customerName = customerName.value,
                    customerGSTIN = customerGSTIN.value,
                    buyerAddress = buyerAddress.value,
                    supplyType = supplyType.value,
                    subtotal = calculationResult.subtotal,
                    totalAmount = calculationResult.totalAmount,
                    taxAmount = calculationResult.taxAmount,
                    totalCgst = calculationResult.totalCgst,
                    totalSgst = calculationResult.totalSgst,
                    totalIgst = calculationResult.totalIgst,
                    items = calculationResult.processedItems,
                    idempotencyKey = idempotencyKey,
                    amountPaid = calculationResult.amountPaid,
                    paymentMethod = paymentMethod.value
                )

                when(result) {
                    is InvoiceCreationResult.Success -> {
                        _uiState.value = InvoiceUiState.Success(
                            invoiceId = result.invoiceId,
                            message = "Invoice ${result.invoiceNumber} created successfully"
                        )
                    }
                    is InvoiceCreationResult.IdempotentReplay -> {
                         _uiState.value = InvoiceUiState.Success(
                            invoiceId = result.invoiceId,
                            message = "Invoice already created"
                        )
                    }
                    is InvoiceCreationResult.QuotaExceeded -> {
                         _uiState.value = InvoiceUiState.Error("Quota Exceeded") // Using Error for now
                    }
                    is InvoiceCreationResult.InsufficientStock -> {
                         _uiState.value = InvoiceUiState.Error("Insufficient stock for ${result.productName}. Requested: ${result.requested}, Available: ${result.available}")
                    }
                    is InvoiceCreationResult.ProductNotFound -> {
                        _uiState.value = InvoiceUiState.Error("Product not found")
                    }
                    is InvoiceCreationResult.InvalidRequest -> {
                        _uiState.value = InvoiceUiState.Error(result.message)
                    }
                    is InvoiceCreationResult.UnexpectedFailure -> {
                        _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${result.cause.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invoice", e)
                _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = InvoiceUiState.Initial
    }

    companion object {
        private const val TAG = "InvoiceViewModel"
    }
}

// Temporary data class for UI input before mapping to domain InvoiceItem
data class InvoiceItemInput(
    val description: String,
    val quantity: Double,
    val pricePerUnit: Double,
    val gstPercentage: Double,
    val unitType: String = "",
    val productId: Long? = null, // null for ad-hoc
    val taxResult: GstCalculator.ItemTaxResult? = null
) {
    val isAdHoc: Boolean get() = productId == null
}
