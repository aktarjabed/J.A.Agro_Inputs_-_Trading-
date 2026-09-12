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

                // Track descriptions added from history to prevent duplicates
                val historyMap = mutableMapOf<String, InvoiceItem>()
                for (item in historicalItems) {
                    val key = item.description.trim().lowercase()
                    if (!historyMap.containsKey(key)) {
                        historyMap[key] = item
                        // We will add them to suggestions later to maintain sort or precedence
                    }
                }

                val processedCatalogKeys = mutableSetOf<String>()

                // Add current catalog products, overriding price/gst with historical if available
                for (product in products) {
                    val key = product.name.trim().lowercase()
                    val historicalMatch = historyMap[key]

                    if (historicalMatch != null) {
                        // Historical sale values take precedence
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
                        // No history, use catalog defaults
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
                    processedCatalogKeys.add(key)
                }

                // Add historical items that don't match any catalog product name
                for ((key, item) in historyMap) {
                    if (!processedCatalogKeys.contains(key)) {
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
                }

                suggestions.sortedBy { it.description }
            }
        }
    }
