package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_sequence")
data class InvoiceSequence(
    @PrimaryKey val businessId: String,
    val lastSequenceNumber: Int = 0
)
