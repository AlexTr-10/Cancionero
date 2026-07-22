package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dropship_products")
data class DropshipProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val supplierPrice: Double,
    val supplierShippingCost: Double,
    val sellingPrice: Double,
    val commissionType: String, // "clasica" or "premium"
    val calculatedProfit: Double,
    val calculatedMargin: Double,
    val hasFreeShipping: Boolean,
    val score: Int = 0,
    val stars: Int = 1,
    val totalCost: Double = 0.0,
    val healthState: String = "Aceptable",
    val timestamp: Long = System.currentTimeMillis()
)

