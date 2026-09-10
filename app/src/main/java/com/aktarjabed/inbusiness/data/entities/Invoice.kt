package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

import androidx.room.Index

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["businessId", "invoiceNumber"], unique = true),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class Invoice(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val idempotencyKey: String? = null,
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerGSTIN: String? = null,
    val buyerAddress: String = "",
    val totalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalCgst: Double = 0.0,
    val totalSgst: Double = 0.0,
    val totalIgst: Double = 0.0,
    val supplyType: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val irn: String? = null,
    val ackNo: String? = null,
    val ackDate: Instant? = null,
    val qrCodeData: String? = null
)