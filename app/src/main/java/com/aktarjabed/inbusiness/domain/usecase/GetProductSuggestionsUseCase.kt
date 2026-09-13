package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.data.repository.ProductRepository
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

data class ProductSuggestion(
    val description: String,
    val unitType: String,
    val pricePerUnit: Double,
    val gstPercentage: Double,
    val product: Product? // Non-null if it comes from the current Product catalog
)

class GetProductSuggestionsUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val invoiceRepository: InvoiceRepository,
    private val businessContext: BusinessContext
) {
    operator fun invoke(): Flow<List<ProductSuggestion>> {
        return combine(
            productRepository.getAllProducts(),
            invoiceRepository.getHistoricalInvoiceItems()
        ) { products, historicalItems ->
                val suggestions = mutableListOf<ProductSuggestion>()

                val historyByProductId = mutableMapOf<Long, InvoiceItem>()
                val historyByDescription = mutableMapOf<String, InvoiceItem>()

                for (item in historicalItems) {
                    if (item.productId != null) {
                        if (!historyByProductId.containsKey(item.productId)) {
                            historyByProductId[item.productId] = item
                        }
                    } else {
                        val key = item.description.trim().lowercase()
                        if (!historyByDescription.containsKey(key)) {
                            historyByDescription[key] = item
                        }
                    }
                }

                // Add current catalog products, overriding price/gst only if there is a linked historical match (by productId)
                for (product in products) {
                    val historicalMatch = historyByProductId[product.id]

                    if (historicalMatch != null) {
                        // Linked historical sale values take precedence
                        suggestions.add(
                            ProductSuggestion(
                                description = product.name,
                                unitType = historicalMatch.unitType,
                                pricePerUnit = historicalMatch.pricePerUnit,
                                gstPercentage = historicalMatch.gstPercentage,
                                product = product
                            )
                        )
                    } else {
                        // No linked history, use catalog defaults
                        suggestions.add(
                            ProductSuggestion(
                                description = product.name,
                                unitType = product.unitType,
                                pricePerUnit = product.pricePerUnit,
                                gstPercentage = product.gstPercentage,
                                product = product
                            )
                        )
                    }
                }

                // Add ad-hoc historical items (productId == null)
                for ((_, item) in historyByDescription) {
                    suggestions.add(
                        ProductSuggestion(
                            description = item.description,
                            unitType = item.unitType,
                            pricePerUnit = item.pricePerUnit,
                            gstPercentage = item.gstPercentage,
                            product = null
                        )
                    )
                }

                suggestions.sortedBy { it.description }
            }
        }
    }
