package com.example.data.repository

import com.example.data.dao.CatalogDao
import com.example.data.dao.ChecklistItemDao
import com.example.data.dao.DropshipProductDao
import com.example.data.model.CatalogProduct
import com.example.data.model.CategoryItem
import com.example.data.model.ChecklistItem
import com.example.data.model.CompanyProfile
import com.example.data.model.DropshipProduct
import kotlinx.coroutines.flow.Flow

class DropshipRepository(
    private val productDao: DropshipProductDao,
    private val checklistDao: ChecklistItemDao,
    private val catalogDao: CatalogDao
) {
    val allProducts: Flow<List<DropshipProduct>> = productDao.getAllProducts()
    val allChecklistItems: Flow<List<ChecklistItem>> = checklistDao.getAllItems()
    val catalogProducts: Flow<List<CatalogProduct>> = catalogDao.getAllCatalogProducts()
    val categories: Flow<List<CategoryItem>> = catalogDao.getAllCategories()
    val companyProfile: Flow<CompanyProfile?> = catalogDao.getCompanyProfile()

    suspend fun insertCatalogProduct(product: CatalogProduct): Long {
        return catalogDao.insertCatalogProduct(product)
    }

    suspend fun deleteCatalogProductById(id: Int) {
        catalogDao.deleteCatalogProductById(id)
    }

    suspend fun insertCategory(category: CategoryItem): Long {
        return catalogDao.insertCategory(category)
    }

    suspend fun saveCompanyProfile(profile: CompanyProfile) {
        catalogDao.saveCompanyProfile(profile)
    }

    suspend fun prepopulateCatalogIfNeeded() {
        val catCount = catalogDao.getCategoryCount()
        if (catCount == 0) {
            val defaultCategories = listOf(
                CategoryItem(name = "Tecnología", iconName = "Devices"),
                CategoryItem(name = "Hogar", iconName = "Home"),
                CategoryItem(name = "Mascotas", iconName = "Pets"),
                CategoryItem(name = "Belleza", iconName = "Face"),
                CategoryItem(name = "Moda", iconName = "Checkroom"),
                CategoryItem(name = "Herramientas", iconName = "Build"),
                CategoryItem(name = "Vehículos", iconName = "DirectionsCar"),
                CategoryItem(name = "Accesorios", iconName = "Extension")
            )
            catalogDao.insertCategories(defaultCategories)
        }

        val prodCount = catalogDao.getCatalogProductCount()
        if (prodCount == 0) {
            val sampleProducts = listOf(
                CatalogProduct(
                    name = "Smartwatch T900 Ultra Max",
                    sku = "TECH-SW900",
                    category = "Tecnología",
                    brand = "UltraTech",
                    shortDescription = "Reloj inteligente 2.09\" AMOLED con llamadas bluetooth y monitor cardíaco.",
                    fullDescription = "El Smartwatch T900 Ultra Max cuenta con una pantalla retina de alta definición de 2.09 pulgadas, notificación de redes sociales, control de música, medición de oxígeno en sangre y más de 50 modos deportivos.",
                    sellingPrice = 110000.0,
                    previousPrice = 145000.0,
                    promoPrice = 99900.0,
                    status = "Disponible",
                    tags = "Más vendido, Oferta, Nuevo",
                    colors = "Negro, Titanio, Naranja",
                    variants = "Estándar 49mm",
                    stock = 45,
                    supplier = "Dropi Bodega Central",
                    internalNotes = "Margen de ganancia excelente. Despacho rápido en 24h.",
                    calculatedMargin = 38.5,
                    calculatedProfit = 42300.0,
                    score = 96,
                    stars = 5
                ),
                CatalogProduct(
                    name = "Freidora de Aire Digital 6 Litros",
                    sku = "HOGAR-FA6L",
                    category = "Hogar",
                    brand = "ChefPro",
                    shortDescription = "Air Fryer 1800W con panel táctil y 8 programas preestablecidos.",
                    fullDescription = "Cocina saludable sin aceite con la freidora de aire ChefPro de 6 litros. Antiadherente cerámico de fácil limpieza, temporizador inteligente y tecnología de circulación rápida de aire a 360°.",
                    sellingPrice = 249000.0,
                    previousPrice = 299000.0,
                    promoPrice = 229000.0,
                    status = "Disponible",
                    tags = "Recomendado, Oferta",
                    colors = "Negro Mate, Blanco Perlado",
                    variants = "6 Litros 1800W",
                    stock = 20,
                    supplier = "Mastershop Cali",
                    internalNotes = "Producto estrella en Mercado Libre. Alta rotación.",
                    calculatedMargin = 27.2,
                    calculatedProfit = 61200.0,
                    score = 88,
                    stars = 4
                ),
                CatalogProduct(
                    name = "Licuadora Portátil USB Recargable",
                    sku = "HOGAR-LIC-USB",
                    category = "Hogar",
                    brand = "FreshJuice",
                    shortDescription = "Licuadora personal de 6 cuchillas de acero para batidos y jugos.",
                    fullDescription = "Prepara tus batidos proteicos y jugos naturales en cualquier lugar. Batería de 2000mAh recargable vía USB tipo C. Vaso de 400ml en tritán libre de BPA.",
                    sellingPrice = 65000.0,
                    previousPrice = 85000.0,
                    promoPrice = 59900.0,
                    status = "Disponible",
                    tags = "Nuevo, Exclusivo",
                    colors = "Rosa, Menta, Azul Cielo",
                    variants = "400ml",
                    stock = 35,
                    supplier = "Dropi Medellín",
                    internalNotes = "Excelente para campañas en Meta Ads / TikTok.",
                    calculatedMargin = 19.5,
                    calculatedProfit = 12600.0,
                    score = 72,
                    stars = 3
                ),
                CatalogProduct(
                    name = "Auriculares Inalámbricos Pro ANC",
                    sku = "TECH-EAR-PRO",
                    category = "Tecnología",
                    brand = "AudioPro",
                    shortDescription = "Cancelación activa de ruido, Bluetooth 5.3 y estuche de carga inalámbrica.",
                    fullDescription = "Sonido de alta fidelidad con cancelación de ruido ambiental. Duración de batería de hasta 24 horas acumuladas con el estuche de carga.",
                    sellingPrice = 89000.0,
                    previousPrice = 120000.0,
                    status = "Disponible",
                    tags = "Oferta",
                    colors = "Blanco, Negro",
                    variants = "ANC Pro",
                    stock = 15,
                    supplier = "Dropi Bogotá",
                    calculatedMargin = 12.0,
                    calculatedProfit = 10680.0,
                    score = 58,
                    stars = 2
                )
            )

            sampleProducts.forEach { catalogDao.insertCatalogProduct(it) }
        }

        val currentProfile = catalogDao.getCompanyProfile()
        // If profile doesn't exist, create default
        catalogDao.saveCompanyProfile(
            CompanyProfile(
                id = 1,
                name = "Mi Tienda Express Colombia",
                slogan = "Lo mejor en tecnología, hogar y tendencias a un clic",
                description = "Somos distribuidores autorizados con entregas garantizadas en todo Colombia. Pagos seguros mediante Mercado Pago y atención prioritaria vía WhatsApp.",
                whatsapp = "+57 312 345 6789",
                phone = "601 745 0000",
                email = "contacto@mitiendaexpress.co",
                address = "Calle 93 # 12 - 45",
                city = "Bogotá D.C., Colombia",
                website = "www.mitiendaexpress.co",
                facebook = "mitiendaexpress.co",
                instagram = "@mitiendaexpress.co",
                tiktok = "@mitiendaexpress_col",
                primaryColorHex = "#1565C0"
            )
        )
    }

    suspend fun insertProduct(product: DropshipProduct) {
        productDao.insertProduct(product)
    }

    suspend fun deleteProductById(id: Int) {
        productDao.deleteProductById(id)
    }

    suspend fun updateChecklistItemStatus(id: Int, isCompleted: Boolean) {
        checklistDao.updateCompletionStatus(id, isCompleted)
    }

    suspend fun prepopulateChecklistIfNeeded() {
        val count = checklistDao.getItemCount()
        if (count == 0) {
            val items = listOf(
                ChecklistItem(
                    title = "Comprender la Política de Dropshipping",
                    description = "Verificar que usas proveedores locales y que entiendes que no puedes duplicar publicaciones en Mercado Libre.",
                    section = "inicio"
                ),
                ChecklistItem(
                    title = "Configurar Cuenta de Mercado Libre",
                    description = "Asegurar que tu cuenta de vendedor esté activa en Mercado Libre Colombia, preferiblemente como persona natural o jurídica con RUT.",
                    section = "inicio"
                ),
                ChecklistItem(
                    title = "Crear Cuenta en Dropi o Mastershop",
                    description = "Registrarse en una plataforma proveedora local en Colombia y familiarizarse con su catálogo de productos y costos.",
                    section = "inicio"
                ),
                ChecklistItem(
                    title = "Preparar Fondo de Caja Inicial",
                    description = "Tener un fondo de reserva (ej. $150,000 COP) para pagar el costo del producto al proveedor en Dropi, ya que Mercado Pago libera el dinero de la venta días después.",
                    section = "inicio"
                ),
                ChecklistItem(
                    title = "Definir Tiempo de Disponibilidad",
                    description = "Configurar 1 día de disponibilidad de stock como máximo para cubrir retrasos leves del proveedor sin perder visibilidad en el ranking.",
                    section = "reputacion"
                ),
                ChecklistItem(
                    title = "Monitorear Stock Diariamente",
                    description = "Establecer una rutina diaria (mañana y noche) para verificar que tus productos publicados no estén agotados en el inventario de Dropi.",
                    section = "reputacion"
                ),
                ChecklistItem(
                    title = "Aprender a Enviar las Etiquetas de Envío",
                    description = "Familiarizarse con el proceso de descargar la etiqueta de Mercado Envíos en PDF y subirla a Dropi para que la bodega imprima y despache.",
                    section = "logistica"
                ),
                ChecklistItem(
                    title = "Evitar Publicidad de Contra Entrega",
                    description = "Entender que en Mercado Libre no se puede ofrecer pago contra entrega propio, todo pago es procesado mediante Mercado Pago.",
                    section = "logistica"
                ),
                ChecklistItem(
                    title = "Selección de 3-10 Productos Iniciales",
                    description = "Buscar productos de alta demanda, ligeros (peso < 1kg) y de bajo costo para minimizar costos de flete y devoluciones.",
                    section = "inicio"
                ),
                ChecklistItem(
                    title = "Diseñar Tabla de Tallas para Ropa",
                    description = "Para pijamas y prendas de vestir, crear una guía de tallas muy detallada en la descripción o imágenes para evitar devoluciones costosas.",
                    section = "ropa"
                )
            )
            checklistDao.insertItems(items)
        }
    }
}
