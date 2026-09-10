package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val brand: String,
    val category: String,
    val unitType: String,

    val pricePerUnit: Double,
    val availableStock: Double,

    val batchNumber: String? = null,
    val isWholesaleOnly: Boolean = false
)
