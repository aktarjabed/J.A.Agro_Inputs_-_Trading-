package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aktarjabed.inbusiness.data.entities.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :categoryName ORDER BY name ASC")
    fun getProductsByCategory(categoryName: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :searchQuery || '%' OR brand LIKE '%' || :searchQuery || '%'")
    fun searchProducts(searchQuery: String): Flow<List<Product>>

    @Query("UPDATE products SET availableStock = availableStock - :quantitySold WHERE id = :productId")
    suspend fun deductStock(productId: Long, quantitySold: Double)

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getUniqueCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT unitType FROM products ORDER BY unitType ASC")
    fun getUniqueUnitTypes(): Flow<List<String>>
}
