package com.example.data.dao

import androidx.room.*
import com.example.data.model.DropshipProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface DropshipProductDao {
    @Query("SELECT * FROM dropship_products ORDER BY timestamp DESC")
    fun getAllProducts(): Flow<List<DropshipProduct>>

    @Query("SELECT * FROM dropship_products ORDER BY timestamp DESC")
    suspend fun getAllProductsList(): List<DropshipProduct>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: DropshipProduct): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<DropshipProduct>)

    @Delete
    suspend fun deleteProduct(product: DropshipProduct)

    @Query("DELETE FROM dropship_products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Query("DELETE FROM dropship_products")
    suspend fun clearAllProducts()
}
