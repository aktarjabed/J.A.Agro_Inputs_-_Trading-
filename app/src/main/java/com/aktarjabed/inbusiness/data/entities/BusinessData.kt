package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_data")
data class BusinessData(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val gstin: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val scenarioName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Double = 0.0,
    val rawMaterialsCost: Double = 0.0,
    val supplierCosts: Double = 0.0,
    val monthlyRent: Double = 0.0,
    val transportCosts: Double = 0.0,
    val labourCosts: Double = 0.0,
    val utilityCosts: Double = 0.0,
    val marketingCosts: Double = 0.0,
    val insuranceCosts: Double = 0.0,
    val interestCosts: Double = 0.0,
    val depreciation: Double = 0.0,
    val incomeTaxSlab: Double = 0.0,
    val tdsAmount: Double = 0.0,
    val otherIncome: Double = 0.0,
    val outputGst: Double = 0.0,
    val inputGst: Double = 0.0
)
