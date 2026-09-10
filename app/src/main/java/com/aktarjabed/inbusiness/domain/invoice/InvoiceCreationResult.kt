package com.aktarjabed.inbusiness.domain.invoice

sealed class InvoiceCreationResult {
    data class Success(val invoiceId: String, val invoiceNumber: String) : InvoiceCreationResult()
    data class IdempotentReplay(val invoiceId: String, val invoiceNumber: String) : InvoiceCreationResult()
    data class QuotaExceeded(val required: Int, val available: Int) : InvoiceCreationResult()
    data class InsufficientStock(
        val productId: Long,
        val productName: String,
        val requested: Double,
        val available: Double
    ) : InvoiceCreationResult()
    data class ProductNotFound(val productId: Long) : InvoiceCreationResult()
    data class InvalidRequest(val message: String) : InvoiceCreationResult()
    data class UnexpectedFailure(val cause: Throwable) : InvoiceCreationResult()
}
