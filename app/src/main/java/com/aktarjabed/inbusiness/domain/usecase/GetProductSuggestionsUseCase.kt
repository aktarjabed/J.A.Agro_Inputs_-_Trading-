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
        return businessContext.activeBusinessId.flatMapLatest { businessId ->
            combine(
                productRepository.getAllProducts(),
                invoiceRepository.getHistoricalInvoiceItems(businessId)
            ) { products, historicalItems ->
                val suggestions = mutableListOf<ProductSuggestion>()

                // Add current catalog products first (they take precedence)
                val catalogDescriptions = mutableSetOf<String>()
                for (product in products) {
                    suggestions.add(
                        ProductSuggestion(
                            description = product.name,
                            unitType = product.unitType,
                            pricePerUnit = product.pricePerUnit,
                            gstPercentage = product.gstPercentage,
                            product = product
                        )
                    )
                    catalogDescriptions.add(product.name.lowercase())
                }

                // Add historical items that don't match any catalog product name
                for (item in historicalItems) {
                    if (!catalogDescriptions.contains(item.description.lowercase())) {
                        suggestions.add(
                            ProductSuggestion(
                                description = item.description,
                                unitType = item.unitType,
                                pricePerUnit = item.pricePerUnit,
                                gstPercentage = item.gstPercentage,
                                product = null
                            )
                        )
                        catalogDescriptions.add(item.description.lowercase()) // Prevent duplicates in history itself
                    }
                }
                suggestions.sortedBy { it.description }
            }
        }
    }
}
