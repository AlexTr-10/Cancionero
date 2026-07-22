package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_products")
data class CatalogProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sku: String = "",
    val category: String = "Tecnología",
    val brand: String = "",
    val shortDescription: String = "",
    val fullDescription: String = "",
    val sellingPrice: Double,
    val previousPrice: Double = 0.0,
    val promoPrice: Double = 0.0,
    val status: String = "Disponible", // Disponible, Agotado, Próximamente
    val tags: String = "", // e.g. "Nuevo, Oferta, Más vendido"
    val colors: String = "", // e.g. "Negro, Azul"
    val variants: String = "", // e.g. "S, M, L"
    val stock: Int = 10,
    val supplier: String = "",
    val internalNotes: String = "",
    val photoUri: String = "",
    val photosJson: String = "", // Pipe-separated URIs string "uri1|uri2|uri3"
    val calculatedMargin: Double = 0.0,
    val calculatedProfit: Double = 0.0,
    val score: Int = 0,
    val stars: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getPhotosList(): List<String> {
        if (photosJson.isNotBlank()) {
            return photosJson.split("|").filter { it.isNotBlank() }
        }
        if (photoUri.isNotBlank()) {
            return listOf(photoUri)
        }
        return emptyList()
    }

    fun getCoverPhotoUri(): String {
        if (photoUri.isNotBlank()) return photoUri
        return getPhotosList().firstOrNull() ?: ""
    }
}
