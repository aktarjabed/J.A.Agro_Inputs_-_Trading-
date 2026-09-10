package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

sealed class StockDeductionResult {
    object Success : StockDeductionResult()
    object InsufficientStock : StockDeductionResult()
    object NotFound : StockDeductionResult()
}

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val businessContext: BusinessContext
) {
    fun getAllProducts(): Flow<List<Product>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.getAllProducts(businessId)
    }

    fun searchProducts(query: String): Flow<List<Product>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.searchProducts(businessId, query)
    }

    fun searchProductsByCategory(query: String, category: String): Flow<List<Product>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.searchProductsByCategory(businessId, query, category)
    }

    fun getUniqueCategories(): Flow<List<String>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.getUniqueCategories(businessId)
    }

    fun getUniqueUnitTypes(): Flow<List<String>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.getUniqueUnitTypes(businessId)
    }

    suspend fun getProductById(id: Long, businessId: String): Product? {
        return productDao.getProductById(id, businessId)
    }

    suspend fun saveProduct(
        businessId: String,
        id: Long,
        name: String,
        brand: String,
        category: String,
        unitType: String,
        pricePerUnit: Double,
        availableStock: Double,
        batchNumber: String?,
        isWholesaleOnly: Boolean
    ): Long {
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(brand.isNotBlank()) { "Brand cannot be blank" }
        require(category.isNotBlank()) { "Category cannot be blank" }
        require(unitType.isNotBlank()) { "Unit type cannot be blank" }
        require(pricePerUnit >= 0) { "Price cannot be negative" }
        require(availableStock >= 0) { "Stock cannot be negative" }

        val trimmedName = name.trim()
        val trimmedBrand = brand.trim()
        val trimmedCategory = category.trim()
        val trimmedUnitType = unitType.trim()
        val trimmedBatchNumber = batchNumber?.trim()?.takeIf { it.isNotBlank() } ?: ""

        return if (id == 0L) {
            val product = Product(
                id = id,
                businessId = businessId,
                name = trimmedName,
                brand = trimmedBrand,
                category = trimmedCategory,
                unitType = trimmedUnitType,
                pricePerUnit = pricePerUnit,
                availableStock = availableStock,
                batchNumber = trimmedBatchNumber,
                isWholesaleOnly = isWholesaleOnly
            )
            productDao.insertProduct(product)
        } else {
            val rowsAffected = productDao.updateProduct(
                id = id,
                businessId = businessId,
                name = trimmedName,
                brand = trimmedBrand,
                category = trimmedCategory,
                unitType = trimmedUnitType,
                pricePerUnit = pricePerUnit,
                availableStock = availableStock,
                batchNumber = trimmedBatchNumber,
                isWholesaleOnly = isWholesaleOnly
            )
            if (rowsAffected == 0) {
                throw IllegalStateException("Failed to update product. It may not exist or belongs to another business.")
            }
            id
        }
    }

    suspend fun deductStock(productId: Long, businessId: String, quantity: Double): StockDeductionResult {
        require(quantity > 0) { "Deduction quantity must be strictly positive" }
        val affectedRows = productDao.deductStock(productId, businessId, quantity)
        return if (affectedRows > 0) {
            StockDeductionResult.Success
        } else {
            val product = productDao.getProductById(productId, businessId)
            if (product == null) {
                StockDeductionResult.NotFound
            } else {
                StockDeductionResult.InsufficientStock
            }
        }
    }

    suspend fun deleteProduct(productId: Long, businessId: String) {
        val rowsAffected = productDao.deleteProduct(productId, businessId)
        if (rowsAffected == 0) {
             throw IllegalStateException("Failed to delete product. It may not exist or belongs to another business.")
        }
    }
}
