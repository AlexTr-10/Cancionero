package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_categories")
data class CategoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconName: String = "Devices"
)

@Entity(tableName = "company_profile")
data class CompanyProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Mi Tienda Dropshipping",
    val slogan: String = "Calidad, variedad y envíos rápidos a todo el país",
    val description: String = "Ofrecemos los productos más innovadores y en tendencia con garantía total y servicio personalizado.",
    val logoUri: String = "",
    val whatsapp: String = "+57 300 000 0000",
    val phone: String = "601 555 0100",
    val email: String = "ventas@mitienda.com",
    val address: String = "Carrera 15 # 93 - 40",
    val city: String = "Bogotá, Colombia",
    val website: String = "www.mitiendadropshipping.com",
    val facebook: String = "mitiendacolombia",
    val instagram: String = "@mitienda_col",
    val tiktok: String = "@mitienda_official",
    val primaryColorHex: String = "#1565C0"
)
