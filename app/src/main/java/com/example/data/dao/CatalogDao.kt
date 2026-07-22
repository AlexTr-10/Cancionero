package com.example.data.dao

import androidx.room.*
import com.example.data.model.CatalogProduct
import com.example.data.model.CategoryItem
import com.example.data.model.CompanyProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    // Products
    @Query("SELECT * FROM catalog_products ORDER BY timestamp DESC")
    fun getAllCatalogProducts(): Flow<List<CatalogProduct>>

    @Query("SELECT * FROM catalog_products ORDER BY timestamp DESC")
    suspend fun getAllCatalogProductsList(): List<CatalogProduct>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogProduct(product: CatalogProduct): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogProducts(products: List<CatalogProduct>)

    @Delete
    suspend fun deleteCatalogProduct(product: CatalogProduct)

    @Query("DELETE FROM catalog_products WHERE id = :id")
    suspend fun deleteCatalogProductById(id: Int)

    @Query("DELETE FROM catalog_products")
    suspend fun clearAllCatalogProducts()

    @Query("SELECT COUNT(*) FROM catalog_products")
    suspend fun getCatalogProductCount(): Int

    // Categories
    @Query("SELECT * FROM catalog_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryItem>>

    @Query("SELECT * FROM catalog_categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryItem): Long

    @Query("SELECT COUNT(*) FROM catalog_categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryItem>)

    @Query("DELETE FROM catalog_categories")
    suspend fun clearAllCategories()

    // Company Profile
    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    fun getCompanyProfile(): Flow<CompanyProfile?>

    @Query("SELECT * FROM company_profile WHERE id = 1 LIMIT 1")
    suspend fun getCompanyProfileDirect(): CompanyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCompanyProfile(profile: CompanyProfile)
}
