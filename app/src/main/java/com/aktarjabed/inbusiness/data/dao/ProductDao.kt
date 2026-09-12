package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aktarjabed.inbusiness.data.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: Product): Long

    @Query("""
        UPDATE products
        SET name = :name,
            brand = :brand,
            category = :category,
            unitType = :unitType,
            pricePerUnit = :pricePerUnit,
            availableStock = :availableStock,
            batchNumber = :batchNumber,
            isWholesaleOnly = :isWholesaleOnly,
            gstPercentage = :gstPercentage
        WHERE id = :id
          AND businessId = :businessId
    """)
    suspend fun updateProduct(
        id: Long,
        businessId: String,
        name: String,
        brand: String,
        category: String,
        unitType: String,
        pricePerUnit: Double,
        availableStock: Double,
        batchNumber: String,
        isWholesaleOnly: Boolean,
        gstPercentage: Double
    ): Int

    @Query("DELETE FROM products WHERE id = :id AND businessId = :businessId")
    suspend fun deleteProduct(id: Long, businessId: String): Int

    @Query("SELECT * FROM products WHERE id = :id AND businessId = :businessId")
    suspend fun getProductById(id: Long, businessId: String): Product?

    @Query("SELECT * FROM products WHERE businessId = :businessId ORDER BY name ASC")
    fun getAllProducts(businessId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND (name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(businessId: String, query: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE businessId = :businessId AND category = :category AND (name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProductsByCategory(businessId: String, query: String, category: String): Flow<List<Product>>

    @Query("""
        UPDATE products
        SET availableStock = availableStock - :quantity
        WHERE id = :productId
          AND businessId = :businessId
          AND availableStock >= :quantity
    """)
    suspend fun deductStock(productId: Long, businessId: String, quantity: Double): Int

    @Query("""
        UPDATE products
        SET availableStock = availableStock + :quantity
        WHERE id = :productId
          AND businessId = :businessId
    """)
    suspend fun addStock(productId: Long, businessId: String, quantity: Double): Int

    @Query("SELECT DISTINCT category FROM products WHERE businessId = :businessId ORDER BY category ASC")
    fun getUniqueCategories(businessId: String): Flow<List<String>>

    @Query("SELECT DISTINCT unitType FROM products WHERE businessId = :businessId ORDER BY unitType ASC")
    fun getUniqueUnitTypes(businessId: String): Flow<List<String>>
}
