package com.aktarjabed.inbusiness.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
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
    private val quotaGate: QuotaGate
) {

    companion object {
        private const val TAG = "InvoiceRepository"
    }

    suspend fun createInvoice(
        userId: String,
        businessId: String,
        customerName: String,
        totalAmount: Double,
        taxRate: Double,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null
    ): QuotaVerdict = withContext(Dispatchers.IO) {

        try {
            database.withTransaction {
                // 1. Idempotency Check
                if (idempotencyKey != null) {
                    val existingInvoice = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey)
                    if (existingInvoice != null) {
                        return@withTransaction QuotaVerdict.Allowed(0)
                    }
                }

                // 2. Consume Quota within the transaction
                val verdict = quotaGate.assertQuota(userId, consume = true)
                if (verdict !is QuotaVerdict.Allowed) {
                    return@withTransaction verdict
                }

                // 3. Sequence logic entirely within transaction
                val currentSeq = invoiceDao.getInvoiceSequence(businessId)
                val nextSeqNumber = (currentSeq?.lastSequenceNumber ?: 0) + 1
                val nextInvoiceNumber = "INV-${String.format("%05d", nextSeqNumber)}"
                val newSequence = InvoiceSequence(businessId, nextSeqNumber)

                val invoiceId = UUID.randomUUID().toString()
                val invoice = Invoice(
                    id = invoiceId,
                    businessId = businessId,
                    idempotencyKey = idempotencyKey,
                    invoiceNumber = nextInvoiceNumber,
                    customerId = "",
                    customerName = customerName,
                    totalAmount = totalAmount,
                    taxAmount = totalAmount * (taxRate / 100.0),
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )

                val updatedItems = items.map { it.copy(invoiceId = invoiceId, id = UUID.randomUUID().toString()) }

                invoiceDao.createInvoiceTransactionally(invoice, updatedItems, newSequence)

                return@withTransaction verdict
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create invoice", e)
            throw e
        }
    }
}
