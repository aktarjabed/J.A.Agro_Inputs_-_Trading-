with open('./app/src/main/java/com/aktarjabed/inbusiness/data/repository/InvoiceRepository.kt', 'r') as f:
    content = f.read()

import re
# The repo you showed above does not match what I wrote in step 3. It got reverted somehow. I'll write the proper version now.
new_repo = """package com.aktarjabed.inbusiness.data.repository

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
        idempotencyKey: String? = null
    ): InvoiceCreationResult = withContext(Dispatchers.IO) {

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
                val rowsUpdated = invoiceDao.incrementSequence(businessId)
                if (rowsUpdated == 0) {
                    invoiceDao.insertSequence(InvoiceSequence(businessId, 1))
                }
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
             Log.e(TAG, "Idempotency constraint conflict", e)
             if (idempotencyKey != null) {
                 val existing = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey)
                 if (existing != null) {
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
"""
with open('./app/src/main/java/com/aktarjabed/inbusiness/data/repository/InvoiceRepository.kt', 'w') as f:
    f.write(new_repo)
