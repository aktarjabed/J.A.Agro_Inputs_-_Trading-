package com.aktarjabed.inbusiness.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val database: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val quotaGate: QuotaGate
) {

    companion object {
        private const val TAG = "InvoiceRepository"
    }

    suspend fun createInvoice(
        userId: String,
        businessId: String,
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        totalAmount: Double,
        taxAmount: Double,
        totalCgst: Double,
        totalSgst: Double,
        totalIgst: Double,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null,
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ): InvoiceCreationResult = withContext(Dispatchers.IO) {
        val balanceDue = totalAmount - amountPaid
        if (amountPaid > totalAmount) {
            return@withContext InvoiceCreationResult.InvalidRequest("Amount paid cannot exceed total amount")
        }

        try {
            database.withTransaction {
                // 1. Idempotency Check (Fast path)
                if (idempotencyKey != null) {
                    val existingInvoice = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey)
                    if (existingInvoice != null) {
                        return@withTransaction InvoiceCreationResult.IdempotentReplay(existingInvoice.id, existingInvoice.invoiceNumber)
                    }
                }

                // 2. Consume Quota within the transaction
                val verdict = quotaGate.assertQuota(userId, consume = true)
                if (verdict !is QuotaVerdict.Allowed) {
                    return@withTransaction InvoiceCreationResult.QuotaExceeded(
                        required = 1,
                        available = 0 // Approximate for now, could be enhanced
                    )
                }

                // 3. Stock Deductions for linked products
                for (item in items) {
                    if (item.productId != null) {
                        val product = productDao.getProductById(item.productId, businessId)
                            ?: return@withTransaction InvoiceCreationResult.ProductNotFound(item.productId)

                        val affectedRows = productDao.deductStock(item.productId, businessId, item.quantity)
                        if (affectedRows == 0) {
                            // Rollback and return InsufficientStock
                            return@withTransaction InvoiceCreationResult.InsufficientStock(
                                productId = item.productId,
                                productName = product.name,
                                requested = item.quantity,
                                available = product.availableStock
                            )
                        }
                    }
                }

                // 4. Atomic Sequence logic
                // Ensure the sequence row exists safely
                invoiceDao.insertSequence(InvoiceSequence(businessId, 0))

                // Atomically increment
                invoiceDao.incrementSequence(businessId)

                // Read the resulting value
                val currentSeq = invoiceDao.getInvoiceSequence(businessId)
                val nextSeqNumber = currentSeq?.lastSequenceNumber ?: 1

                val nextInvoiceNumber = "INV-${String.format(java.util.Locale.US, "%05d", nextSeqNumber)}"

                val invoiceId = UUID.randomUUID().toString()
                val invoice = Invoice(
                    id = invoiceId,
                    businessId = businessId,
                    idempotencyKey = idempotencyKey,
                    invoiceNumber = nextInvoiceNumber,
                    customerId = "", // Reserved for full customer management
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    totalAmount = totalAmount,
                    taxAmount = taxAmount,
                    totalCgst = totalCgst,
                    totalSgst = totalSgst,
                    totalIgst = totalIgst,
                    supplyType = supplyType.name,
                    amountPaid = amountPaid,
                    balanceDue = balanceDue,
                    paymentMethod = paymentMethod,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                val updatedItems = items.map {
                    it.copy(
                        invoiceId = invoiceId,
                        id = UUID.randomUUID().toString()
                    )
                }

                invoiceDao.insertInvoice(invoice)
                updatedItems.forEach { invoiceDao.insertItem(it) }

                return@withTransaction InvoiceCreationResult.Success(invoiceId, nextInvoiceNumber)
            }
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
             // 5. Concurrency fallback for Idempotency (Slow path - unique constraint collision)
             // Transaction has rolled back by now
             Log.e(TAG, "Idempotency constraint conflict", e)
             if (idempotencyKey != null) {
                 val existing = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey)
                 if (existing != null) {
                     // Verify payload consistency
                     if (existing.totalAmount != totalAmount || existing.customerName != customerName) {
                         return@withContext InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload")
                     }
                     return@withContext InvoiceCreationResult.IdempotentReplay(existing.id, existing.invoiceNumber)
                 }
             }
             return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create invoice", e)
            return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        }
    }
}
