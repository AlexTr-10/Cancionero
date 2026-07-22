package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.model.ChecklistItem
import com.example.data.model.DropshipProduct
import com.example.data.repository.DropshipRepository
import com.example.network.RetrofitClient
import com.example.network.GenerateContentRequest
import com.example.network.Content
import com.example.network.Part
import com.example.network.GenerationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.util.BackupRestoreManager
import com.example.util.BackupResult
import com.example.util.RestoreResult
import android.net.Uri

class DropshipViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = DropshipRepository(
        database.dropshipProductDao(),
        database.checklistItemDao(),
        database.catalogDao()
    )

    // Data flow from DB
    val savedProducts: StateFlow<List<DropshipProduct>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val catalogProducts: StateFlow<List<com.example.data.model.CatalogProduct>> = repository.catalogProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<com.example.data.model.CategoryItem>> = repository.categories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val companyProfile: StateFlow<com.example.data.model.CompanyProfile?> = repository.companyProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val checklistItems: StateFlow<List<ChecklistItem>> = repository.allChecklistItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.prepopulateChecklistIfNeeded()
            repository.prepopulateCatalogIfNeeded()
        }
    }

    // --- Catalog Operations ---
    fun saveCatalogProduct(product: com.example.data.model.CatalogProduct) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCatalogProduct(product)
        }
    }

    fun deleteCatalogProduct(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCatalogProductById(id)
        }
    }

    fun addCategory(name: String, iconName: String = "Devices") {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCategory(com.example.data.model.CategoryItem(name = name.trim(), iconName = iconName))
        }
    }

    fun updateCompanyProfile(profile: com.example.data.model.CompanyProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCompanyProfile(profile)
        }
    }

    // Direct integration: Import calculated dropship product into catalog
    fun importCalculatedProductToCatalog(
        title: String,
        category: String,
        sellingPrice: Double,
        profit: Double,
        margin: Double,
        score: Int,
        stars: Int,
        description: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val catProduct = com.example.data.model.CatalogProduct(
                name = title.ifBlank { "Producto Importado" },
                category = category.ifBlank { "Tecnología" },
                shortDescription = description.ifBlank { "Producto rentable analizado con la Calculadora ($stars estrellas)." },
                fullDescription = "Producto analizado con la Calculadora de Rentabilidad. Margen neto del ${String.format("%.1f", margin)}% y ganancia estimada de \$${profit.toInt()} COP.",
                sellingPrice = sellingPrice,
                status = "Disponible",
                tags = "Recomendado",
                calculatedMargin = margin,
                calculatedProfit = profit,
                score = score,
                stars = stars
            )
            repository.insertCatalogProduct(catProduct)
        }
    }
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "¡Hola! Soy DropiML, tu asesor experto de dropshipping en Mercado Libre Colombia. ¿En qué puedo ayudarte hoy? Puedo resolver tus dudas sobre Dropi, Mastershop, reputación de ventas, comisiones, envío gratis obligatorios o cómo manejar tallas de ropa.",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateChecklistIfNeeded()
        }
    }

    // --- Calculator Math ---
    fun computeScore(
        sellingPrice: Double,
        profit: Double,
        margin: Double,
        totalCost: Double
    ): Int {
        if (sellingPrice <= 0) return 0
        val marginPts = (margin / 35.0 * 40.0).coerceIn(0.0, 40.0)
        val profitPts = (profit / 35000.0 * 25.0).coerceIn(0.0, 25.0)
        val costRatio = if (sellingPrice > 0) totalCost / sellingPrice else 1.0
        val costPts = ((1.0 - costRatio) / 0.5 * 20.0).coerceIn(0.0, 20.0)
        val riskPts = if (margin > 0) 15.0 else 0.0

        return (marginPts + profitPts + costPts + riskPts).toInt().coerceIn(0, 100)
    }

    fun computeStars(score: Int): Int {
        return when {
            score >= 95 -> 5
            score >= 80 -> 4
            score >= 65 -> 3
            score >= 50 -> 2
            else -> 1
        }
    }

    fun computeHealthState(score: Int, margin: Double): String {
        return when {
            margin >= 25.0 || score >= 80 -> "Excelente"
            margin >= 18.0 || score >= 65 -> "Muy Buena"
            margin >= 15.0 || score >= 50 -> "Aceptable"
            margin >= 5.0 || score >= 35 -> "Riesgoso"
            else -> "No recomendado"
        }
    }

    fun calculateProfit(
        sellingPrice: Double,
        supplierPrice: Double,
        commissionMlPercent: Double,
        shippingCost: Double,
        platformFee: Double,
        returnsReservePercent: Double,
        packagingCost: Double = 0.0
    ): CalculationResult {
        if (sellingPrice <= 0.0) return CalculationResult()

        val pv = sellingPrice
        val cp = supplierPrice
        val pctML = commissionMlPercent / 100.0
        val env = shippingCost
        val plat = platformFee + packagingCost
        val pctDev = returnsReservePercent / 100.0

        val montoML = pv * pctML
        val montoDev = pv * pctDev
        val totalCostos = cp + montoML + env + plat + montoDev
        val margenNeto = pv - totalCostos
        val margenPct = if (pv > 0) (margenNeto / pv) * 100.0 else 0.0

        val margenBruto = pv - (cp + montoML + env + plat)
        val margenBrutoPct = if (pv > 0) (margenBruto / pv) * 100.0 else 0.0

        return CalculationResult(
            commissionFee = montoML,
            fixedFee = plat,
            freeShippingFee = env,
            taxWithholding = montoDev,
            totalMlFees = montoML + montoDev,
            totalCost = totalCostos,
            profit = margenNeto,
            margin = margenPct,
            hasFreeShipping = env > 0,
            grossMarginPercent = margenBrutoPct
        )
    }

    private var lastAutoSavedTitle: String = ""
    private var lastAutoSavedPrice: Double = 0.0

    fun autoSaveCalculation(
        title: String,
        supplierPrice: Double,
        supplierShippingCost: Double,
        sellingPrice: Double,
        commissionType: String,
        result: CalculationResult
    ) {
        if (sellingPrice <= 0 || supplierPrice <= 0) return
        val itemTitle = title.ifBlank { "Producto ${sellingPrice.toInt()}" }

        if (itemTitle == lastAutoSavedTitle && sellingPrice == lastAutoSavedPrice) return

        lastAutoSavedTitle = itemTitle
        lastAutoSavedPrice = sellingPrice

        saveCalculation(
            title = itemTitle,
            supplierPrice = supplierPrice,
            supplierShippingCost = supplierShippingCost,
            sellingPrice = sellingPrice,
            commissionType = commissionType,
            result = result
        )
    }

    fun saveCalculation(
        title: String,
        supplierPrice: Double,
        supplierShippingCost: Double,
        sellingPrice: Double,
        commissionType: String,
        result: CalculationResult
    ) {
        val score = computeScore(sellingPrice, result.profit, result.margin, result.totalCost)
        val stars = computeStars(score)
        val health = computeHealthState(score, result.margin)

        viewModelScope.launch(Dispatchers.IO) {
            val product = DropshipProduct(
                title = title.ifBlank { "Producto Calculado" },
                supplierPrice = supplierPrice,
                supplierShippingCost = supplierShippingCost,
                sellingPrice = sellingPrice,
                commissionType = commissionType,
                calculatedProfit = result.profit,
                calculatedMargin = result.margin,
                hasFreeShipping = result.hasFreeShipping,
                score = score,
                stars = stars,
                totalCost = result.totalCost,
                healthState = health
            )
            repository.insertProduct(product)
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProductById(id)
        }
    }

    // Comparison selection state
    private val _selectedProductIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedProductIds: StateFlow<Set<Int>> = _selectedProductIds.asStateFlow()

    fun toggleProductSelection(id: Int) {
        val current = _selectedProductIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            if (current.size < 3) {
                current.add(id)
            }
        }
        _selectedProductIds.value = current
    }

    fun clearSelection() {
        _selectedProductIds.value = emptySet()
    }

    // --- Checklist Operations ---
    fun toggleChecklistItem(id: Int, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChecklistItemStatus(id, isCompleted)
        }
    }

    // --- AI Chat Logic ---
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return

        // Add user message to list
        val updatedMessages = _chatMessages.value.toMutableList()
        updatedMessages.add(ChatMessage(text = message, isUser = true))
        _chatMessages.value = updatedMessages

        _isChatLoading.value = true

        viewModelScope.launch {
            val replyText = generateAiReply(message, updatedMessages)
            _isChatLoading.value = false
            val finalMessages = _chatMessages.value.toMutableList()
            finalMessages.add(ChatMessage(text = replyText, isUser = false))
            _chatMessages.value = finalMessages
        }
    }

    private suspend fun generateAiReply(userPrompt: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("DropshipViewModel", "Gemini API key is missing or is placeholder. Using fallback expert simulator.")
            return@withContext getExpertOfflineResponse(userPrompt)
        }

        // System Instruction context
        val systemInstruction = "Eres DropiML, un asesor experto en dropshipping para Mercado Libre Colombia, especializado en integraciones con Dropi y Mastershop. " +
                "Ayudas a nuevos vendedores a entender las políticas, la logística de Mercado Envíos, cómo enviar etiquetas PDF a bodegas, calcular costos y márgenes de ganancia reales " +
                "incluyendo comisiones clásicas (14% + $1500 COP fijo) / premium (18.5% + $1500 COP fijo) y la regla de envío gratis para productos de más de $90,000 COP, y cómo minimizar " +
                "devoluciones en la categoría de ropa (tallas y guías). Responde de forma amigable, clara, concisa y estructurada en español de Colombia."

        // Prepare context history (limit to last 6 turns to stay within lightweight limits)
        val apiHistory = history.takeLast(7).map { msg ->
            Content(
                parts = listOf(Part(text = msg.text))
            )
        }

        val request = GenerateContentRequest(
            contents = apiHistory,
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "Lo siento, no pude procesar la respuesta. Por favor inténtalo de nuevo."
        } catch (e: Exception) {
            Log.e("DropshipViewModel", "Error in Gemini API call, calling offline simulator", e)
            getExpertOfflineResponse(userPrompt)
        }
    }

    // --- Backup & Restore State ---
    private val _isBackupInProgress = MutableStateFlow(false)
    val isBackupInProgress: StateFlow<Boolean> = _isBackupInProgress.asStateFlow()

    private val _isRestoreInProgress = MutableStateFlow(false)
    val isRestoreInProgress: StateFlow<Boolean> = _isRestoreInProgress.asStateFlow()

    fun createBackupZip(onResult: (BackupResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBackupInProgress.value = true
            val result = BackupRestoreManager.createBackupZip(getApplication(), database)
            _isBackupInProgress.value = false
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun restoreBackupZip(zipUri: Uri, replaceExisting: Boolean = true, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRestoreInProgress.value = true
            val result = BackupRestoreManager.restoreBackupZip(getApplication(), database, zipUri, replaceExisting)
            _isRestoreInProgress.value = false
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun shareBackupZip(zipFile: java.io.File) {
        BackupRestoreManager.shareBackupZip(getApplication(), zipFile)
    }

    fun copyZipToUri(sourceZip: java.io.File, destinationUri: Uri): Boolean {
        return BackupRestoreManager.copyZipToUri(getApplication(), sourceZip, destinationUri)
    }

    // Comprehensive expert rules fallback for offline or un-keyed usage
    private fun getExpertOfflineResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("politica") || lower.contains("permitido") || lower.contains("permiten") || lower.contains("normas") -> {
                "**Política de Dropshipping en Mercado Libre Colombia:**\n\n" +
                "1. **¿Está permitido?** Sí, está permitido vender sin inventario físico con proveedores locales, pero Mercado Libre **no tiene una categoría especial para Dropshipping**. Eres responsable al 100% por los despachos.\n" +
                "2. **Duplicidad:** No puedes clonar o duplicar publicaciones del mismo producto con la misma cuenta o con cuentas relacionadas. Cada publicación debe ser única.\n" +
                "3. **Tiempos:** Debes enviar tus productos dentro de las primeras 24 horas hábiles si usas Mercado Envíos normal, o el mismo día si estás en Bogotá/Medellín con Mercado Envíos Flex. Si no lo haces, tu reputación caerá drásticamente de verde a naranja/rojo."
            }
            lower.contains("dropi") || lower.contains("mastershop") || lower.contains("proveedor") || lower.contains("bodega") -> {
                "**Logística con Dropi y Mastershop en Colombia:**\n\n" +
                "1. **¿Cómo funciona?** Cuando vendes en Mercado Libre, la plataforma te genera una etiqueta de **Mercado Envíos** (Servientrega, Coordinadora, Envía o Deprisa).\n" +
                "2. **Proceso de Despacho:** Debes descargar esa etiqueta en formato PDF e ingresarla al panel de Dropi/Mastershop en el pedido correspondiente. Su bodega la imprimirá, empacará tu producto y lo entregará a la transportadora.\n" +
                "3. **Fondo de saldo:** Recuerda que debes tener saldo precargado en Dropi para pagar el costo del producto al proveedor y el costo operativo de flete de Dropi antes de que despachen. Mercado Libre te liberará el dinero de la venta entre 2 y 14 días después."
            }
            lower.contains("comision") || lower.contains("calculo") || lower.contains("tarifa") || lower.contains("margen") || lower.contains("costo") -> {
                "**Estructura de Tarifas y Comisiones en Mercado Libre Colombia (COP):**\n\n" +
                "• **Comisión Clásica:** Promedio de 14% por venta (varía por categoría).\n" +
                "• **Comisión Premium:** Promedio de 18.5% (permite cuotas sin interés para el comprador).\n" +
                "• **Cargo Fijo:** Adicional de $1,500 COP por unidad vendida para productos menores a $90,000 COP.\n" +
                "• **Envío Gratis Obligatorio:** Si tu producto vale **$90,000 COP o más**, el envío gratis al comprador es obligatorio para ti. Mercado Libre te descontará entre $9,500 y $15,000 COP de envío (promedio $11,500 COP).\n" +
                "• **Retenciones:** Te descontarán un aproximado del 3.5% en retenciones de impuestos (ReteIVA, ReteICA, ReteFuente).\n\n" +
                "¡Prueba nuestra pestaña de **Calculadora** en el menú para simular tus costos exactos!"
            }
            lower.contains("tiempo") || lower.contains("entrega") || lower.contains("retraso") || lower.contains("reputacion") || lower.contains("demora") -> {
                "**Tiempos de Entrega y Reputación:**\n\n" +
                "• **Regla de las 24 horas:** Debes despachar las ventas en un máximo de 24 horas hábiles para conservar reputación verde.\n" +
                "• **Uso de disponibilidad de stock:** En la publicación puedes agregar 'Disponibilidad de stock' (ej. 1 o 2 días de tiempo de preparación). Esto te da un colchón de seguridad por si el proveedor tarda en procesar, pero **reduce la visibilidad en búsquedas** y te descarta para envíos en el mismo día.\n" +
                "• **Límite de retraso:** Si tienes más del 15% de envíos demorados, tu reputación caerá a color amarillo o rojo, reduciendo drásticamente tus ventas."
            }
            lower.contains("ropa") || lower.contains("talla") || lower.contains("pijama") || lower.contains("prenda") || lower.contains("devolucion") -> {
                "**Recomendaciones para la Categoría de Ropa:**\n\n" +
                "1. **Riesgo de Devolución:** En ropa, las devoluciones por talla incorrecta son muy comunes (hasta 20%). En Mercado Libre, si el comprador devuelve un producto, **tú asumes el costo del envío de regreso**, lo cual puede destruir tus ganancias.\n" +
                "2. **Buenas prácticas:**\n" +
                "   - Publica siempre una **Tabla de Tallas** detallada (pecho, cintura, cadera en cm) tanto en la descripción como en una de las imágenes de la publicación.\n" +
                "   - Indica si la prenda viene reducida u holgada (horma pequeña o grande).\n" +
                "   - Usa nuestra herramienta **Asistente de Tallas** en la pestaña correspondiente para generar una guía descriptiva profesional y copiarla en tu publicación."
            }
            lower.contains("contra entrega") || lower.contains("pago en casa") || lower.contains("efectivo") || lower.contains("cod") -> {
                "**Pago Contra Entrega en Mercado Libre:**\n\n" +
                "• **No está permitido:** Mercado Libre **no permite** de forma nativa la opción de pago contra entrega (pagar en efectivo al recibir).\n" +
                "• **Métodos autorizados:** Todos los compradores deben pagar a través de **Mercado Pago** usando Tarjeta de Crédito, débito PSE, o Efecty antes de que se genere la etiqueta de envío.\n" +
                "• Si un comprador te pide pagar contra entrega, adviértele que las políticas de la plataforma exigen pago anticipado para garantizar la compra protegida."
            }
            else -> {
                "Esa es una excelente pregunta sobre Dropshipping en Mercado Libre Colombia. Te recomiendo tener muy en cuenta:\n\n" +
                "1. **Monitorear el inventario** de Dropi/Mastershop diariamente.\n" +
                "2. **Calcular bien el precio final** para no perder dinero cuando el producto pase los $90,000 COP y se active el envío gratis obligatorio.\n" +
                "3. **Evitar demoras** en los despachos para proteger tu reputación verde.\n\n" +
                "¿Tienes alguna duda específica sobre comisiones, logística de etiquetas PDF o el asistente de tallas?"
            }
        }
    }
}

// Helper models
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class CalculationResult(
    val commissionFee: Double = 0.0,
    val fixedFee: Double = 0.0,
    val freeShippingFee: Double = 0.0,
    val taxWithholding: Double = 0.0,
    val totalMlFees: Double = 0.0,
    val totalCost: Double = 0.0,
    val profit: Double = 0.0,
    val margin: Double = 0.0,
    val hasFreeShipping: Boolean = false,
    val grossMarginPercent: Double = 0.0
)
