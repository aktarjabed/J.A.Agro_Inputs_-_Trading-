import re

with open('app/src/main/java/com/aktarjabed/inbusiness/data/repository/InvoiceRepository.kt', 'r') as f:
    content = f.read()

# Add TransactionAbortException
if 'TransactionAbortException' not in content:
    content = re.sub(
        r'import java\.util\.UUID\n(.*?)@Singleton',
        r'import java.util.UUID\n\nprivate class TransactionAbortException(val result: com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult) : Exception()\n\n\1@Singleton',
        content,
        flags=re.DOTALL
    )

# Find the createInvoice method and rewrite it
# First find the boundary
start_idx = content.find('suspend fun createInvoice(')

# Simple string replacement for the logic part.
old_try_block = """        try {
            database.withTransaction {
                val businessData = businessDao.getBusinessDataById(businessId)
                    ?: return@withTransaction InvoiceCreationResult.InvalidRequest("Business data not found")

                val sellerName = businessData.name
                val sellerAddress = businessData.address
                val sellerGSTIN = businessData.gstin

                val calcResult = try {
                    calculateInvoiceTotalsUseCase(items, supplyType, amountPaid)
                } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                return@withTransaction InvoiceCreationResult.InvalidRequest(e.message ?: "Invalid calculation")
                }

                val requestFingerprint = com.aktarjabed.inbusiness.utils.RequestFingerprint.generate(
                    businessId = businessId,
                    sellerName = sellerName,
                    sellerAddress = sellerAddress,
                    sellerGSTIN = sellerGSTIN,
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    supplyType = supplyType,
                    subtotal = calcResult.subtotal,
                    totalAmount = calcResult.totalAmount,
                    taxAmount = calcResult.taxAmount,
                    items = calcResult.processedItems,
                    amountPaid = calcResult.amountPaid,
                    paymentMethod = paymentMethod
                )"""

new_try_block = """        var requestFingerprint: String? = null

        try {
            database.withTransaction {
                val businessData = businessDao.getBusinessDataById(businessId)
                    ?: throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Business data not found"))

                val sellerName = businessData.name
                val sellerAddress = businessData.address
                val sellerGSTIN = businessData.gstin

                val calcResult = try {
                    calculateInvoiceTotalsUseCase(items, supplyType, amountPaid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    throw TransactionAbortException(InvoiceCreationResult.InvalidRequest(e.message ?: "Invalid calculation"))
                }

                requestFingerprint = com.aktarjabed.inbusiness.utils.RequestFingerprint.generate(
                    businessId = businessId,
                    sellerName = sellerName,
                    sellerAddress = sellerAddress,
                    sellerGSTIN = sellerGSTIN,
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    supplyType = supplyType,
                    subtotal = calcResult.subtotal,
                    totalAmount = calcResult.totalAmount,
                    taxAmount = calcResult.taxAmount,
                    items = calcResult.processedItems,
                    amountPaid = calcResult.amountPaid,
                    paymentMethod = paymentMethod
                )"""

content = content.replace(old_try_block, new_try_block)

old_return_1 = """                if (idempotencyKey != null) {
                    val existingInvoice = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                    if (existingInvoice != null) {
                        if (existingInvoice.requestFingerprint == requestFingerprint) {
                            return@withTransaction InvoiceCreationResult.IdempotentReplay(existingInvoice.id, existingInvoice.invoiceNumber)
                        } else {
                            return@withTransaction InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload")
                        }
                    }
                }"""

new_return_1 = """                if (idempotencyKey != null) {
                    val existingInvoice = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                    if (existingInvoice != null) {
                        if (existingInvoice.requestFingerprint == requestFingerprint) {
                            throw TransactionAbortException(InvoiceCreationResult.IdempotentReplay(existingInvoice.id, existingInvoice.invoiceNumber))
                        } else {
                            throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload"))
                        }
                    }
                }"""
content = content.replace(old_return_1, new_return_1)

old_return_2 = """                val verdict = quotaGate.assertQuota(userId, consume = true)
                if (verdict !is QuotaVerdict.Allowed) {
                    return@withTransaction InvoiceCreationResult.QuotaExceeded(
                        required = 1,
                        available = 0 // Approximate for now, could be enhanced
                    )
                }"""

new_return_2 = """                val verdict = quotaGate.assertQuota(userId, consume = true)
                if (verdict !is QuotaVerdict.Allowed) {
                    throw TransactionAbortException(InvoiceCreationResult.QuotaExceeded(
                        required = 1,
                        available = 0 // Approximate for now, could be enhanced
                    ))
                }"""
content = content.replace(old_return_2, new_return_2)


old_return_3 = """                    if (item.productId != null) {
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
                    }"""

new_return_3 = """                    if (item.productId != null) {
                        val product = productDao.getProductById(item.productId, businessId)
                            ?: throw TransactionAbortException(InvoiceCreationResult.ProductNotFound(item.productId))

                        val affectedRows = productDao.deductStock(item.productId, businessId, item.quantity)
                        if (affectedRows == 0) {
                            // Rollback and return InsufficientStock
                            throw TransactionAbortException(InvoiceCreationResult.InsufficientStock(
                                productId = item.productId,
                                productName = product.name,
                                requested = item.quantity,
                                available = product.availableStock
                            ))
                        }
                    }"""
content = content.replace(old_return_3, new_return_3)


old_catch_blocks = """        } catch (e: android.database.sqlite.SQLiteConstraintException) {
             // 5. Concurrency fallback for Idempotency (Slow path - unique constraint collision)
             // Transaction has rolled back by now
             Log.e(TAG, "Idempotency constraint conflict", e)
             if (idempotencyKey != null) {
                 val existing = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                 if (existing != null) {
                     // In a true fallback we should re-generate fingerprint, but since we just had a constraint conflict,
                     // returning an unexpected failure or doing our best is fine if fingerprint isn't in scope.
                     // Wait, we need the request fingerprint to verify. Let's just generate it here again or we could pass it out.
                     // Actually, we can regenerate it if needed, or assume it's just a general failure if we can't.

                     // Let's re-generate fingerprint here since requestFingerprint is no longer in scope
                     val businessData = businessDao.getBusinessDataById(businessId)
                     if (businessData != null) {
                         val calcResult = try {
                             calculateInvoiceTotalsUseCase(items, supplyType, amountPaid)
                         } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) { throw e }
                             null
                         }

                         if (calcResult != null) {
                             val requestFingerprint = com.aktarjabed.inbusiness.utils.RequestFingerprint.generate(
                                 businessId = businessId,
                                 sellerName = businessData.name,
                                 sellerAddress = businessData.address,
                                 sellerGSTIN = businessData.gstin,
                                 customerName = customerName,
                                 customerGSTIN = customerGSTIN,
                                 buyerAddress = buyerAddress,
                                 supplyType = supplyType,
                                 subtotal = calcResult.subtotal,
                                 totalAmount = calcResult.totalAmount,
                                 taxAmount = calcResult.taxAmount,
                                 items = calcResult.processedItems,
                                 amountPaid = calcResult.amountPaid,
                                 paymentMethod = paymentMethod
                             )
                             if (existing.requestFingerprint == requestFingerprint) {
                                 return@withContext InvoiceCreationResult.IdempotentReplay(existing.id, existing.invoiceNumber)
                             } else {
                                 return@withContext InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload")
                             }
                         }
                     }
                 }
             }
             return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
             throw e // Explicitly rethrow CancellationException
        } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to create invoice", e)
            return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        }
    }
}"""

new_catch_blocks = """        } catch (e: TransactionAbortException) {
            return@withContext e.result
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
             // 5. Concurrency fallback for Idempotency (Slow path - unique constraint collision)
             // Transaction has rolled back by now
             Log.e(TAG, "Idempotency constraint conflict", e)
             if (idempotencyKey != null) {
                 val existing = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                 if (existing != null) {
                     if (existing.requestFingerprint == requestFingerprint) {
                         return@withContext InvoiceCreationResult.IdempotentReplay(existing.id, existing.invoiceNumber)
                     } else {
                         return@withContext InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload")
                     }
                 }
             }
             // No matching invoice exists, propagate the original DB failure
             return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
             throw e // Explicitly rethrow CancellationException
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to create invoice", e)
            return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        }
    }
}"""
content = content.replace(old_catch_blocks, new_catch_blocks)

with open('app/src/main/java/com/aktarjabed/inbusiness/data/repository/InvoiceRepository.kt', 'w') as f:
    f.write(content)
