package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "calculation_results",
    foreignKeys = [
        ForeignKey(
            entity = BusinessData::class,
            parentColumns = ["id"],
            childColumns = ["businessDataId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("businessDataId")]
)
data class CalculationResult(
    @PrimaryKey val id: String = "",
    val businessDataId: String = "",
    val grossProfit: Double = 0.0,
    val ebitda: Double = 0.0,
    val netProfit: Double = 0.0,
    val gstPayable: Double = 0.0,
    val breakEvenPoint: Double = 0.0,
    val cashFlow: Double = 0.0,
    val grossMargin: Double = 0.0,
    val netMargin: Double = 0.0,
    val operatingMargin: Double = 0.0,
    val roi: Double = 0.0,
    val createdAt: Instant = Instant.now()
)
