package com.aktarjabed.inbusiness.presentation.screens.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.inbusiness.presentation.components.SearchableDropdownField
import com.aktarjabed.inbusiness.presentation.viewmodel.ProductViewModel
import com.aktarjabed.inbusiness.presentation.viewmodel.SaveProductState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEntryScreen(
    productId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val editingProduct by viewModel.editingProduct.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    val existingCategories by viewModel.existingCategories.collectAsState()
    val existingUnitTypes by viewModel.existingUnitTypes.collectAsState()

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unitType by remember { mutableStateOf("") }
    var pricePerUnitStr by remember { mutableStateOf("") }
    var availableStockStr by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var isWholesaleOnly by remember { mutableStateOf(false) }
    var gstPercentageStr by remember { mutableStateOf("") }
    var gstError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProductForEditing(productId)
        } else {
            viewModel.clearEditingProduct()
        }
    }

    LaunchedEffect(editingProduct) {
        editingProduct?.let {
            name = it.name
            brand = it.brand
            category = it.category
            unitType = it.unitType
            pricePerUnitStr = it.pricePerUnit.toString()
            availableStockStr = it.availableStock.toString()
            batchNumber = it.batchNumber
            isWholesaleOnly = it.isWholesaleOnly
            gstPercentageStr = if (it.gstPercentage > 0.0) it.gstPercentage.toString() else ""
        }
    }

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveProductState.Success -> {
                viewModel.resetSaveState()
                onNavigateBack()
            }
            is SaveProductState.Error -> {
                val error = (saveState as SaveProductState.Error).message
                snackbarHostState.showSnackbar(error)
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Add Product" else "Edit Product") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SearchableDropdownField(
                value = category,
                onValueChange = { category = it },
                label = "Category",
                suggestions = existingCategories
            )

            SearchableDropdownField(
                value = unitType,
                onValueChange = { unitType = it },
                label = "Unit Type (e.g., kg, L, bag)",
                suggestions = existingUnitTypes
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = pricePerUnitStr,
                    onValueChange = { pricePerUnitStr = it },
                    label = { Text("Price per Unit") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = availableStockStr,
                    onValueChange = { availableStockStr = it },
                    label = { Text("Initial Stock") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }


            OutlinedTextField(
                value = batchNumber,
                onValueChange = { batchNumber = it },
                label = { Text("Batch Number (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = gstPercentageStr,
                onValueChange = {
                    gstPercentageStr = it
                    gstError = null
                    val d = it.toDoubleOrNull()
                    if (it.isNotBlank()) {
                        if (d == null || !d.isFinite()) {
                            gstError = "Enter a valid GST percentage."
                        } else if (d < 0) {
                            gstError = "GST percentage cannot be negative."
                        }
                    }
                },
                label = { Text("GST Percentage (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = gstError != null,
                supportingText = { if (gstError != null) Text(gstError!!) }
            )


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isWholesaleOnly,
                    onCheckedChange = { isWholesaleOnly = it }
                )
                Text("Wholesale Only")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.saveProduct(
                        id = productId ?: 0L,
                        name = name,
                        brand = brand,
                        category = category,
                        unitType = unitType,
                        pricePerUnit = pricePerUnitStr.toDoubleOrNull() ?: 0.0,
                        availableStock = availableStockStr.toDoubleOrNull() ?: 0.0,
                        batchNumber = batchNumber,
                        isWholesaleOnly = isWholesaleOnly,
                        gstPercentage = gstPercentageStr.takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = saveState !is SaveProductState.Loading && gstError == null
            ) {
                if (saveState is SaveProductState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Product")
                }
            }
        }
    }
}
