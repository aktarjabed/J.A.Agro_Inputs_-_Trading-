package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.CalculationResult
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {

    @Query("SELECT * FROM business_data LIMIT 1")
    suspend fun getBusinessData(): BusinessData?

    @Query("SELECT * FROM business_data LIMIT 1")
    fun getBusinessDataFlow(): Flow<BusinessData?>

    @Query("SELECT * FROM business_data")
    fun getAllBusinessData(): Flow<List<BusinessData>>

    @Query("SELECT * FROM business_data WHERE id = :id")
    suspend fun getBusinessDataById(id: String): BusinessData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(businessData: BusinessData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessData(businessData: BusinessData)

    @Update
    suspend fun update(businessData: BusinessData)

    @Delete
    suspend fun delete(businessData: BusinessData)

    @Delete
    suspend fun deleteBusinessData(businessData: BusinessData)

    @Query("SELECT * FROM calculation_results WHERE businessDataId = :businessId ORDER BY createdAt DESC")
    fun getCalculationResults(businessId: String): Flow<List<CalculationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculationResult(result: CalculationResult)

    @Delete
    suspend fun deleteCalculationResult(result: CalculationResult)

    @Query("DELETE FROM calculation_results WHERE businessDataId = :businessId")
    suspend fun deleteAllCalculationResults(businessId: String)
}
