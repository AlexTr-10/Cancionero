package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.model.CatalogProduct
import com.example.data.model.CategoryItem
import com.example.data.model.ChecklistItem
import com.example.data.model.CompanyProfile
import com.example.data.model.DropshipProduct
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupResult(
    val isSuccess: Boolean,
    val zipFile: File? = null,
    val catalogProductsCount: Int = 0,
    val photosCount: Int = 0,
    val simulationsCount: Int = 0,
    val categoriesCount: Int = 0,
    val errorMessage: String? = null
)

data class RestoreResult(
    val isSuccess: Boolean,
    val catalogProductsRestored: Int = 0,
    val photosRestored: Int = 0,
    val simulationsRestored: Int = 0,
    val categoriesRestored: Int = 0,
    val errorMessage: String? = null
)

object BackupRestoreManager {

    /**
     * Creates a complete ZIP backup containing database JSONs and media photos.
     */
    suspend fun createBackupZip(context: Context, database: AppDatabase): BackupResult {
        return try {
            val catalogProducts = database.catalogDao().getAllCatalogProductsList()
            val categories = database.catalogDao().getAllCategoriesList()
            val companyProfile = database.catalogDao().getCompanyProfileDirect()
            val dropshipProducts = database.dropshipProductDao().getAllProductsList()
            val checklistItems = database.checklistItemDao().getAllItemsList()

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "Respaldo_CatalogPro_$timestampStr.zip"
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val zipFile = File(backupDir, backupFileName)
            val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

            val photoUriToZipPath = mutableMapOf<String, String>()
            var photoIndexCounter = 0

            // 1. Export Catalog Product Photos
            for (product in catalogProducts) {
                val photos = product.getPhotosList()
                for (pIndex in photos.indices) {
                    val photoUriStr = photos[pIndex]
                    if (photoUriStr.isNotBlank() && !photoUriToZipPath.containsKey(photoUriStr)) {
                        val ext = getFileExtension(photoUriStr)
                        val relativeZipPath = "photos/prod_${product.id}_${photoIndexCounter++}.$ext"
                        val inputStream = getInputStreamFromUriOrPath(context, photoUriStr)
                        if (inputStream != null) {
                            try {
                                zipOutputStream.putNextEntry(ZipEntry(relativeZipPath))
                                inputStream.copyTo(zipOutputStream)
                                zipOutputStream.closeEntry()
                                photoUriToZipPath[photoUriStr] = relativeZipPath
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                inputStream.close()
                            }
                        }
                    }
                }
            }

            // 2. Export Company Profile Logo
            if (companyProfile != null && companyProfile.logoUri.isNotBlank()) {
                val logoUriStr = companyProfile.logoUri
                if (!photoUriToZipPath.containsKey(logoUriStr)) {
                    val ext = getFileExtension(logoUriStr)
                    val relativeZipPath = "company/logo.$ext"
                    val inputStream = getInputStreamFromUriOrPath(context, logoUriStr)
                    if (inputStream != null) {
                        try {
                            zipOutputStream.putNextEntry(ZipEntry(relativeZipPath))
                            inputStream.copyTo(zipOutputStream)
                            zipOutputStream.closeEntry()
                            photoUriToZipPath[logoUriStr] = relativeZipPath
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            inputStream.close()
                        }
                    }
                }
            }

            // 3. Create JSON Files
            // Metadata
            val metadataObj = JSONObject().apply {
                put("appName", "Catálogo Vendedores Pro")
                put("backupVersion", 1)
                put("createdAt", System.currentTimeMillis())
                put("formattedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("device", Build.MODEL)
                put("catalogProductsCount", catalogProducts.size)
                put("simulationsCount", dropshipProducts.size)
                put("categoriesCount", categories.size)
                put("photosCount", photoUriToZipPath.size)
            }
            addJsonEntryToZip(zipOutputStream, "metadata.json", metadataObj.toString(2))

            // Catalog Products
            val productsArray = JSONArray()
            for (p in catalogProducts) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("sku", p.sku)
                    put("category", p.category)
                    put("brand", p.brand)
                    put("shortDescription", p.shortDescription)
                    put("fullDescription", p.fullDescription)
                    put("sellingPrice", p.sellingPrice)
                    put("previousPrice", p.previousPrice)
                    put("promoPrice", p.promoPrice)
                    put("status", p.status)
                    put("tags", p.tags)
                    put("colors", p.colors)
                    put("variants", p.variants)
                    put("stock", p.stock)
                    put("supplier", p.supplier)
                    put("internalNotes", p.internalNotes)

                    val mappedCover = photoUriToZipPath[p.photoUri] ?: ""
                    put("photoUri", mappedCover)

                    val mappedPhotosList = p.getPhotosList().map { photoUriToZipPath[it] ?: "" }.filter { it.isNotBlank() }
                    put("photosJson", mappedPhotosList.joinToString("|"))

                    put("calculatedMargin", p.calculatedMargin)
                    put("calculatedProfit", p.calculatedProfit)
                    put("score", p.score)
                    put("stars", p.stars)
                    put("timestamp", p.timestamp)
                }
                productsArray.put(pObj)
            }
            addJsonEntryToZip(zipOutputStream, "catalog_products.json", productsArray.toString(2))

            // Categories
            val categoriesArray = JSONArray()
            for (c in categories) {
                val cObj = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("iconName", c.iconName)
                }
                categoriesArray.put(cObj)
            }
            addJsonEntryToZip(zipOutputStream, "categories.json", categoriesArray.toString(2))

            // Company Profile
            if (companyProfile != null) {
                val companyObj = JSONObject().apply {
                    put("id", companyProfile.id)
                    put("name", companyProfile.name)
                    put("slogan", companyProfile.slogan)
                    put("description", companyProfile.description)
                    put("logoUri", photoUriToZipPath[companyProfile.logoUri] ?: "")
                    put("whatsapp", companyProfile.whatsapp)
                    put("phone", companyProfile.phone)
                    put("email", companyProfile.email)
                    put("address", companyProfile.address)
                    put("city", companyProfile.city)
                    put("website", companyProfile.website)
                    put("facebook", companyProfile.facebook)
                    put("instagram", companyProfile.instagram)
                    put("tiktok", companyProfile.tiktok)
                    put("primaryColorHex", companyProfile.primaryColorHex)
                }
                addJsonEntryToZip(zipOutputStream, "company_profile.json", companyObj.toString(2))
            }

            // Dropship Products (Calculations / History)
            val simulationsArray = JSONArray()
            for (ds in dropshipProducts) {
                val dsObj = JSONObject().apply {
                    put("id", ds.id)
                    put("title", ds.title)
                    put("supplierPrice", ds.supplierPrice)
                    put("supplierShippingCost", ds.supplierShippingCost)
                    put("sellingPrice", ds.sellingPrice)
                    put("commissionType", ds.commissionType)
                    put("calculatedProfit", ds.calculatedProfit)
                    put("calculatedMargin", ds.calculatedMargin)
                    put("hasFreeShipping", ds.hasFreeShipping)
                    put("score", ds.score)
                    put("stars", ds.stars)
                    put("totalCost", ds.totalCost)
                    put("healthState", ds.healthState)
                    put("timestamp", ds.timestamp)
                }
                simulationsArray.put(dsObj)
            }
            addJsonEntryToZip(zipOutputStream, "dropship_products.json", simulationsArray.toString(2))

            // Checklist Items
            val checklistArray = JSONArray()
            for (chk in checklistItems) {
                val chkObj = JSONObject().apply {
                    put("id", chk.id)
                    put("title", chk.title)
                    put("description", chk.description)
                    put("section", chk.section)
                    put("isCompleted", chk.isCompleted)
                }
                checklistArray.put(chkObj)
            }
            addJsonEntryToZip(zipOutputStream, "checklist_items.json", checklistArray.toString(2))

            zipOutputStream.finish()
            zipOutputStream.close()

            BackupResult(
                isSuccess = true,
                zipFile = zipFile,
                catalogProductsCount = catalogProducts.size,
                photosCount = photoUriToZipPath.size,
                simulationsCount = dropshipProducts.size,
                categoriesCount = categories.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Error al generar el archivo ZIP")
        }
    }

    /**
     * Restores complete data from a ZIP backup file.
     */
    suspend fun restoreBackupZip(
        context: Context,
        database: AppDatabase,
        zipUri: Uri,
        replaceExisting: Boolean = true
    ): RestoreResult {
        val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
        return try {
            if (!tempDir.exists()) tempDir.mkdirs()

            // 1. Unpack ZIP
            val inputStream = context.contentResolver.openInputStream(zipUri)
                ?: return RestoreResult(isSuccess = false, errorMessage = "No se pudo abrir el archivo ZIP seleccionado.")

            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
            var zipEntry = zipInputStream.nextEntry

            while (zipEntry != null) {
                val newFile = File(tempDir, zipEntry.name)
                // Security check for Zip Slip
                if (!newFile.canonicalPath.startsWith(tempDir.canonicalPath)) {
                    throw SecurityException("Entrada ZIP inválida: ${zipEntry.name}")
                }

                if (zipEntry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zipInputStream.copyTo(fos)
                    }
                }
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            // 2. Validate Metadata
            val metadataFile = File(tempDir, "metadata.json")
            if (!metadataFile.exists()) {
                return RestoreResult(isSuccess = false, errorMessage = "El archivo ZIP no contiene un respaldo válido (falta metadata.json).")
            }

            // 3. Extract and Copy Media Files to Persistent Storage
            val zipToLocalUriMap = mutableMapOf<String, String>()

            val targetPhotosDir = File(context.filesDir, "product_photos")
            if (!targetPhotosDir.exists()) targetPhotosDir.mkdirs()

            val extractedPhotosDir = File(tempDir, "photos")
            if (extractedPhotosDir.exists() && extractedPhotosDir.isDirectory) {
                extractedPhotosDir.listFiles()?.forEach { photoFile ->
                    if (photoFile.isFile) {
                        val destFile = File(targetPhotosDir, "restored_${System.currentTimeMillis()}_${photoFile.name}")
                        photoFile.copyTo(destFile, overwrite = true)
                        val relativeZipPath = "photos/${photoFile.name}"
                        zipToLocalUriMap[relativeZipPath] = Uri.fromFile(destFile).toString()
                    }
                }
            }

            val targetCompanyDir = File(context.filesDir, "company_logo")
            if (!targetCompanyDir.exists()) targetCompanyDir.mkdirs()

            val extractedCompanyDir = File(tempDir, "company")
            if (extractedCompanyDir.exists() && extractedCompanyDir.isDirectory) {
                extractedCompanyDir.listFiles()?.forEach { logoFile ->
                    if (logoFile.isFile) {
                        val destFile = File(targetCompanyDir, "restored_logo_${System.currentTimeMillis()}_${logoFile.name}")
                        logoFile.copyTo(destFile, overwrite = true)
                        val relativeZipPath = "company/${logoFile.name}"
                        zipToLocalUriMap[relativeZipPath] = Uri.fromFile(destFile).toString()
                    }
                }
            }

            // 4. DB Restore
            if (replaceExisting) {
                database.catalogDao().clearAllCatalogProducts()
                database.catalogDao().clearAllCategories()
                database.dropshipProductDao().clearAllProducts()
                database.checklistItemDao().clearAllChecklistItems()
            }

            // Categories
            val categoriesFile = File(tempDir, "categories.json")
            var categoriesRestoredCount = 0
            if (categoriesFile.exists()) {
                val catArray = JSONArray(categoriesFile.readText())
                val catList = mutableListOf<CategoryItem>()
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    catList.add(
                        CategoryItem(
                            id = if (replaceExisting) obj.optInt("id", 0) else 0,
                            name = obj.optString("name", ""),
                            iconName = obj.optString("iconName", "Devices")
                        )
                    )
                }
                if (catList.isNotEmpty()) {
                    database.catalogDao().insertCategories(catList)
                    categoriesRestoredCount = catList.size
                }
            }

            // Catalog Products
            val productsFile = File(tempDir, "catalog_products.json")
            var productsRestoredCount = 0
            if (productsFile.exists()) {
                val prodArray = JSONArray(productsFile.readText())
                val prodList = mutableListOf<CatalogProduct>()
                for (i in 0 until prodArray.length()) {
                    val obj = prodArray.getJSONObject(i)

                    val rawPhotoUri = obj.optString("photoUri", "")
                    val mappedPhotoUri = zipToLocalUriMap[rawPhotoUri] ?: rawPhotoUri

                    val rawPhotosJson = obj.optString("photosJson", "")
                    val mappedPhotosList = rawPhotosJson.split("|")
                        .filter { it.isNotBlank() }
                        .map { zipToLocalUriMap[it] ?: it }
                    val mappedPhotosJson = mappedPhotosList.joinToString("|")

                    prodList.add(
                        CatalogProduct(
                            id = if (replaceExisting) obj.optInt("id", 0) else 0,
                            name = obj.optString("name", "Producto"),
                            sku = obj.optString("sku", ""),
                            category = obj.optString("category", "General"),
                            brand = obj.optString("brand", ""),
                            shortDescription = obj.optString("shortDescription", ""),
                            fullDescription = obj.optString("fullDescription", ""),
                            sellingPrice = obj.optDouble("sellingPrice", 0.0),
                            previousPrice = obj.optDouble("previousPrice", 0.0),
                            promoPrice = obj.optDouble("promoPrice", 0.0),
                            status = obj.optString("status", "Disponible"),
                            tags = obj.optString("tags", ""),
                            colors = obj.optString("colors", ""),
                            variants = obj.optString("variants", ""),
                            stock = obj.optInt("stock", 10),
                            supplier = obj.optString("supplier", ""),
                            internalNotes = obj.optString("internalNotes", ""),
                            photoUri = mappedPhotoUri,
                            photosJson = mappedPhotosJson,
                            calculatedMargin = obj.optDouble("calculatedMargin", 0.0),
                            calculatedProfit = obj.optDouble("calculatedProfit", 0.0),
                            score = obj.optInt("score", 0),
                            stars = obj.optInt("stars", 1),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (prodList.isNotEmpty()) {
                    database.catalogDao().insertCatalogProducts(prodList)
                    productsRestoredCount = prodList.size
                }
            }

            // Company Profile
            val companyFile = File(tempDir, "company_profile.json")
            if (companyFile.exists()) {
                val obj = JSONObject(companyFile.readText())
                val rawLogo = obj.optString("logoUri", "")
                val mappedLogo = zipToLocalUriMap[rawLogo] ?: rawLogo

                val companyProfile = CompanyProfile(
                    id = 1,
                    name = obj.optString("name", "Mi Tienda"),
                    slogan = obj.optString("slogan", ""),
                    description = obj.optString("description", ""),
                    logoUri = mappedLogo,
                    whatsapp = obj.optString("whatsapp", ""),
                    phone = obj.optString("phone", ""),
                    email = obj.optString("email", ""),
                    address = obj.optString("address", ""),
                    city = obj.optString("city", ""),
                    website = obj.optString("website", ""),
                    facebook = obj.optString("facebook", ""),
                    instagram = obj.optString("instagram", ""),
                    tiktok = obj.optString("tiktok", ""),
                    primaryColorHex = obj.optString("primaryColorHex", "#1565C0")
                )
                database.catalogDao().saveCompanyProfile(companyProfile)
            }

            // Dropship Products (Calculations / History)
            val dsFile = File(tempDir, "dropship_products.json")
            var simulationsRestoredCount = 0
            if (dsFile.exists()) {
                val dsArray = JSONArray(dsFile.readText())
                val dsList = mutableListOf<DropshipProduct>()
                for (i in 0 until dsArray.length()) {
                    val obj = dsArray.getJSONObject(i)
                    dsList.add(
                        DropshipProduct(
                            id = if (replaceExisting) obj.optInt("id", 0) else 0,
                            title = obj.optString("title", "Simulación"),
                            supplierPrice = obj.optDouble("supplierPrice", 0.0),
                            supplierShippingCost = obj.optDouble("supplierShippingCost", 0.0),
                            sellingPrice = obj.optDouble("sellingPrice", 0.0),
                            commissionType = obj.optString("commissionType", "clasica"),
                            calculatedProfit = obj.optDouble("calculatedProfit", 0.0),
                            calculatedMargin = obj.optDouble("calculatedMargin", 0.0),
                            hasFreeShipping = obj.optBoolean("hasFreeShipping", false),
                            score = obj.optInt("score", 0),
                            stars = obj.optInt("stars", 1),
                            totalCost = obj.optDouble("totalCost", 0.0),
                            healthState = obj.optString("healthState", "Aceptable"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (dsList.isNotEmpty()) {
                    database.dropshipProductDao().insertProducts(dsList)
                    simulationsRestoredCount = dsList.size
                }
            }

            // Checklist Items
            val chkFile = File(tempDir, "checklist_items.json")
            if (chkFile.exists()) {
                val chkArray = JSONArray(chkFile.readText())
                val chkList = mutableListOf<ChecklistItem>()
                for (i in 0 until chkArray.length()) {
                    val obj = chkArray.getJSONObject(i)
                    chkList.add(
                        ChecklistItem(
                            id = if (replaceExisting) obj.optInt("id", 0) else 0,
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            section = obj.optString("section", "inicio"),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
                if (chkList.isNotEmpty()) {
                    database.checklistItemDao().insertItems(chkList)
                }
            }

            RestoreResult(
                isSuccess = true,
                catalogProductsRestored = productsRestoredCount,
                photosRestored = zipToLocalUriMap.size,
                simulationsRestored = simulationsRestoredCount,
                categoriesRestored = categoriesRestoredCount
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Error al restaurar la copia de seguridad.")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * Shares the ZIP backup file via Android Sharesheet (WhatsApp, Drive, Email, etc.)
     */
    fun shareBackupZip(context: Context, zipFile: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Respaldo Catálogo Vendedores Pro")
                putExtra(Intent.EXTRA_TEXT, "Adjunto respaldo completo de catálogo, fotografías y configuraciones.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir copia de seguridad por..."))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Copies the backup ZIP file to a target URI selected by the user via Storage Access Framework.
     */
    fun copyZipToUri(context: Context, sourceZip: File, destinationUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                FileInputStream(sourceZip).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun addJsonEntryToZip(zipOutputStream: ZipOutputStream, entryName: String, jsonString: String) {
        zipOutputStream.putNextEntry(ZipEntry(entryName))
        zipOutputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        zipOutputStream.closeEntry()
    }

    private fun getInputStreamFromUriOrPath(context: Context, uriOrPath: String): InputStream? {
        if (uriOrPath.isBlank()) return null
        return try {
            if (uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(uriOrPath))
            } else {
                val file = File(uriOrPath)
                if (file.exists()) FileInputStream(file) else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileExtension(pathOrUri: String): String {
        val clean = pathOrUri.substringBefore("?").substringBefore("#")
        val dotIndex = clean.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < clean.length - 1) {
            val ext = clean.substring(dotIndex + 1).lowercase(Locale.ROOT)
            if (ext.length <= 4 && ext.all { it.isLetterOrDigit() }) return ext
        }
        return "jpg"
    }
}
