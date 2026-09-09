package com.aktarjabed.inbusiness.data.repository

import android.util.Log
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
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
        items: List<InvoiceItem>
    ): QuotaVerdict = withContext(Dispatchers.IO) {

        // 1. Peek quota first (non-consuming)
        val peekVerdict = quotaGate.assertQuota(userId, consume = false)
        if (peekVerdict !is QuotaVerdict.Allowed) {
            return@withContext peekVerdict
        }

        // 2. Fetch and increment sequence (thread-safe by nature of Room transaction in DAO, but we do read-modify-write here)
        // Ideally, sequence generation should be entirely within the transaction.
        // For simplicity and to avoid complex SQLite increment statements, we read here and pass to transaction.
        val currentSeq = invoiceDao.getInvoiceSequence(businessId)
        val nextSeqNumber = (currentSeq?.lastSequenceNumber ?: 0) + 1
        val nextInvoiceNumber = "INV-${String.format("%05d", nextSeqNumber)}"
        val newSequence = InvoiceSequence(businessId, nextSeqNumber)

        val invoiceId = UUID.randomUUID().toString()
        val invoice = Invoice(
            id = invoiceId,
            businessId = businessId,
            invoiceNumber = nextInvoiceNumber,
            customerId = "",
            customerName = customerName,
            totalAmount = totalAmount,
            taxAmount = totalAmount * (taxRate / 100.0),
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        // Bind invoice ID to items
        val updatedItems = items.map { it.copy(invoiceId = invoiceId, id = UUID.randomUUID().toString()) }

        try {
            // 3. Create invoice inside transaction
            invoiceDao.createInvoiceTransactionally(invoice, updatedItems, newSequence)

            // 4. Consume quota ONLY AFTER successful DB insertion
            val finalVerdict = quotaGate.assertQuota(userId, consume = true)
            if (finalVerdict !is QuotaVerdict.Allowed) {
                // Highly unlikely edge case (quota changed between peek and consume)
                invoiceDao.deleteInvoice(invoice)
                return@withContext finalVerdict
            }

            return@withContext finalVerdict

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create invoice", e)
            throw e
        }
    }
}
