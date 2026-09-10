package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getAllInvoices(businessId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    suspend fun getAllInvoicesOnce(businessId: String): List<Invoice>

    @Query("SELECT * FROM invoices WHERE id = :id AND businessId = :businessId LIMIT 1")
    suspend fun getInvoiceById(id: String, businessId: String): Invoice?

    @Query("SELECT * FROM invoices WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getInvoiceByIdempotencyKey(idempotencyKey: String): Invoice?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InvoiceItem)

    @Transaction
    suspend fun updateInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        updateInvoice(invoice)
        deleteItemsForInvoice(invoice.id)
        items.forEach { insertItem(it) }
    }

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE businessId = :businessId AND (invoiceNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%')")
    fun searchInvoices(businessId: String, query: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentInvoicesByBusiness(businessId: String, limit: Int): List<Invoice>

    @Query("SELECT * FROM invoice_sequence WHERE businessId = :businessId LIMIT 1")
    suspend fun getInvoiceSequence(businessId: String): InvoiceSequence?

    @Query("UPDATE invoice_sequence SET lastSequenceNumber = lastSequenceNumber + 1 WHERE businessId = :businessId")
    suspend fun incrementSequence(businessId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSequence(sequence: InvoiceSequence): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSequence(sequence: InvoiceSequence)

    @Transaction
    suspend fun createInvoiceTransactionally(
        invoice: Invoice,
        items: List<InvoiceItem>,
        sequence: InvoiceSequence
    ) {
        insertInvoice(invoice)
        items.forEach { insertItem(it) }
        insertOrUpdateSequence(sequence)
    }
}
