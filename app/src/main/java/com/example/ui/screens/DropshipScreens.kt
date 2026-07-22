package com.example.ui.screens

import android.content.Intent
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChecklistItem
import com.example.data.model.DropshipProduct
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.CalculationResult
import com.example.ui.viewmodel.DropshipViewModel
import java.text.NumberFormat
import java.util.Locale

// Custom Mercado Libre Colombia Signature Palette
val MLBlue = Color(0xFF3483FA)
val MLYellow = Color(0xFFFFF159)
val MLBackgroundLight = Color(0xFFF5F5F5)
val MLCardBackground = Color(0xFFFFFFFF)
val MLDarkBlue = Color(0xFF1259C3)
val MLGreen = Color(0xFF00A650)
val MLRed = Color(0xFFF30121)
val MLOrange = Color(0xFFFF5A00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropshipNavigationWrapper(viewModel: DropshipViewModel) {
    var selectedScreen by remember { mutableStateOf(0) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAdCreator by remember { mutableStateOf(false) }

    if (showBackupDialog) {
        BackupRestoreDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }

    if (showAdCreator) {
        AdCreatorScreen(
            viewModel = viewModel,
            onDismiss = { showAdCreator = false }
        )
    }

    val navItems = listOf(
        NavigationItem("Catálogo", Icons.Default.Storefront, "nav_catalog"),
        NavigationItem("Calculadora", Icons.Default.Calculate, "nav_calculator"),
        NavigationItem("Ranking", Icons.Default.EmojiEvents, "nav_ranking"),
        NavigationItem("Comparar", Icons.Default.CompareArrows, "nav_compare"),
        NavigationItem("Mi Empresa", Icons.Default.Business, "nav_company")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MLYellow, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Catálogo Vendedores",
                                tint = MLBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Catálogo Vendedores Pro",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MLYellow),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "COLOMBIA",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MLBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBackupDialog = true },
                        modifier = Modifier.testTag("top_bar_backup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Copia de Seguridad y Restauración",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MLBlue
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedScreen == index,
                        onClick = { selectedScreen = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, fontWeight = if (selectedScreen == index) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MLBlue,
                            selectedTextColor = MLBlue,
                            indicatorColor = MLYellow.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        },
        containerColor = MLBackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedScreen) {
                0 -> CatalogScreen(viewModel, onNavigateToCompany = { selectedScreen = 4 })
                1 -> CalculatorScreen(viewModel, onAddedToCatalog = { selectedScreen = 0 })
                2 -> RankingScreen(viewModel, onNavigateToCompare = { selectedScreen = 3 })
                3 -> ComparisonScreen(viewModel)
                4 -> CompanyProfileScreen(viewModel)
            }
        }
    }
}

data class NavigationItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val testTag: String)

// ==========================================
// 1. GUIDES SCREEN
// ==========================================
@Composable
fun GuidesScreen() {
    val guides = remember { getMockGuides() }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val filteredGuides = guides.filter {
        (selectedCategory == "Todos" || it.category == selectedCategory) &&
        (it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MLCardBackground),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Centro de Aprendizaje Dropshipping",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MLDarkBlue
                    )
                    Text(
                        text = "Familiarízate con las reglas de Mercado Libre Colombia y aprende cómo integrar tu negocio con Dropi y Mastershop.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            // Search & Filters Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar guías...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_guides_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MLBlue,
                    unfocusedBorderColor = Color.LightGray
                )
            )
        }

        item {
            // Category scroll row
            val categories = listOf("Todos", "Políticas", "Logística", "Reputación", "Contra Entrega", "Prendas")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.take(3).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MLBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.drop(3).forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MLBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        if (filteredGuides.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron guías correspondientes.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(filteredGuides) { guide ->
                GuideCard(guide)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GuideCard(guide: GuideItem) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("guide_card_${guide.id}"),
        colors = CardDefaults.cardColors(containerColor = MLCardBackground),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(guide.color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(guide.icon, contentDescription = null, tint = guide.color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = guide.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        Text(
                            text = guide.category,
                            fontSize = 11.sp,
                            color = guide.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expandir",
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = guide.content,
                        fontSize = 13.5.sp,
                        color = Color.DarkGray,
                        lineHeight = 19.sp
                    )
                    if (guide.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MLBackgroundLight),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "💡 Recomendación Clave:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MLDarkBlue
                               )
                                Text(
                                    text = guide.tips,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class GuideItem(
    val id: Int,
    val title: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val content: String,
    val tips: String
)

fun getMockGuides(): List<GuideItem> {
    return listOf(
        GuideItem(
            id = 1,
            title = "¿Está permitido hacer Dropshipping?",
            category = "Políticas",
            icon = Icons.Default.Shield,
            color = MLBlue,
            content = "Sí, Mercado Libre permite la venta de productos sin stock físico directo, siempre que uses proveedores locales estables (como Dropi o Mastershop). Sin embargo, NO existe un estatus especial para 'dropshipping': eres 100% responsable ante el comprador. Adicionalmente, tienes prohibido clonar o duplicar publicaciones (vender el mismo producto varias veces con el mismo catálogo), ya que suspenderán tus publicaciones.",
            tips = "Asegúrate de que las fotos y títulos de tus publicaciones se diferencien de las de otros vendedores en Dropi para evitar reportes por duplicidad."
        ),
        GuideItem(
            id = 2,
            title = "Políticas de Despacho y Reputación",
            category = "Reputación",
            icon = Icons.Default.TrendingUp,
            color = MLGreen,
            content = "Mercado Libre exige entregar los paquetes a la transportadora en menos de 24 horas hábiles. Si no lo haces, caerás en demora. Un porcentaje de retraso superior al 15% (o del 5% para tiendas líderes) bajará tu reputación a amarillo o rojo, quitándote visibilidad y cancelando los beneficios del envío gratuito subvencionado.",
            tips = "Establece '1 día de disponibilidad' (tiempo de preparación) en tus configuraciones de Mercado Libre para dar margen a la bodega de Dropi, aunque esto reste un poco de visibilidad de envíos inmediatos."
        ),
        GuideItem(
            id = 3,
            title = "Envíos y Etiquetas de Mercado Envíos",
            category = "Logística",
            icon = Icons.Default.LocalShipping,
            color = MLOrange,
            content = "¡MUY IMPORTANTE! NO puedes subir guías de envío de Dropi o Mastershop de manera manual a Mercado Libre. Debes despachar mediante Mercado Envíos obligatoriamente en la mayoría de categorías. El flujo correcto es:\n1. Se realiza la venta en Mercado Libre.\n2. Descargas el archivo PDF de la etiqueta de envío generado por Mercado Libre.\n3. Vas a Dropi o Mastershop, creas el pedido correspondiente, y cargas el PDF de la etiqueta en la sección correspondiente. La bodega del proveedor imprime tu etiqueta de Mercado Envíos, empaca y envía por ti.",
            tips = "Revisa los pedidos de Dropi dos veces al día para asegurarte de que las etiquetas se hayan asignado y procesado a tiempo por las bodegas."
        ),
        GuideItem(
            id = 4,
            title = "El Flujo del Dinero y Capital de Trabajo",
            category = "Contra Entrega",
            icon = Icons.Default.MonetizationOn,
            color = MLGreen,
            content = "Mercado Libre NO tiene soporte nativo para pago contra entrega (pago en efectivo al recibir). El comprador paga 100% a través de Mercado Pago antes de enviarle. El dinero de la venta queda retenido por Mercado Libre de 2 a 14 días. Sin embargo, para que Dropi procese tu despacho, DEBES pagarle el costo del producto de inmediato. Por lo tanto, necesitas un fondo de reserva para financiar los pedidos antes de cobrar.",
            tips = "Te sugerimos iniciar con un fondo de por lo menos $150,000 COP para cubrir las compras iniciales del proveedor mientras Mercado Pago libera tus primeros saldos."
        ),
        GuideItem(
            id = 5,
            title = "Riesgo de Devolución en Ropa y Tallas",
            category = "Prendas",
            icon = Icons.Default.Checkroom,
            color = MLBlue,
            content = "La venta de ropa (pijamas, lencería, sets) mediante dropshipping es muy lucrativa pero tiene altas tasas de devolución (hasta un 20%) debido a errores en la selección de tallas. En Mercado Libre, si el comprador devuelve una prenda por talla incorrecta, la transportadora le devolverá el paquete gratis, pero Mercado Libre te cobrará a ti el flete de devolución (aprox. $11,000 COP), anulando tu margen.",
            tips = "Es obligatorio incluir tablas de tallas exhaustivas en centímetros (pecho, cadera, cintura) y educar al cliente en la descripción del producto para que compre la correcta."
        ),
        GuideItem(
            id = 6,
            title = "¿Qué hacer si un producto se agota?",
            category = "Reputación",
            icon = Icons.Default.Warning,
            color = MLRed,
            content = "Si vendes un artículo y luego tu proveedor (Dropi/Mastershop) se queda sin existencias, estás obligado a cancelar la venta si no consigues otra alternativa rápidamente. Cancelar ventas por falta de stock penaliza gravemente tu reputación. Si superas un 2.5% de cancelaciones manuales directas, tu cuenta puede ser suspendida provisionalmente.",
            tips = "Verifica los niveles de stock en Dropi a las 8:00 AM y 6:00 PM todos los días. Si quedan menos de 5 unidades con el proveedor, pausa tu publicación de inmediato para evitar incidentes."
        ),
        GuideItem(
            id = 7,
            title = "Estrategias para Empezar sin Presupuesto",
            category = "Políticas",
            icon = Icons.Default.Lightbulb,
            color = MLGreen,
            content = "Si vas a iniciar con un presupuesto ajustado y de 3 a 10 productos:\n1. Elige artículos pequeños y ligeros (<1kg) para que el envío gratis subvencionado no sea costoso.\n2. Evita productos con muchas variantes de color o talla (ej. electrónicos simples, gadgets para cocina o cosméticos) para facilitar el control.\n3. Optimiza tus fichas técnicas con títulos SEO detallados para competir con tiendas consolidadas sin pagar publicidad.",
            tips = "Dedícale tiempo a responder preguntas previas a la venta en menos de 15 minutos; la rapidez de respuesta aumenta las conversiones en un 40%."
        )
    )
}

// ==========================================
// 2. CALCULATOR SCREEN
// ==========================================
@Composable
fun CalculatorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    hint: String? = null,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5B5548),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            trailingIcon = {
                Text(
                    text = suffix,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8A8272),
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MLBlue,
                unfocusedBorderColor = Color(0xFFDDD6C7)
            ),
            shape = RoundedCornerShape(8.dp)
        )
        if (hint != null) {
            Text(
                text = hint,
                fontSize = 11.5.sp,
                color = Color(0xFF9C9481),
                modifier = Modifier.padding(top = 4.dp),
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color = Color(0xFFB5794A)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Surface(
            color = iconColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = title,
            fontSize = 16.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B2620)
        )
    }
}

@Composable
fun CostBreakdownRow(
    label: String,
    amountStr: String,
    percentage: Double,
    indicatorColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1.3f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(indicatorColor, RoundedCornerShape(2.dp))
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = Color(0xFF2B2620)
            )
        }
        Text(
            text = amountStr,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = Color(0xFF332F28),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = "${String.format("%.1f", percentage)}%",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = indicatorColor,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.End
        )
    }
}

data class HealthStatus(
    val color: Color,
    val bg: Color,
    val title: String,
    val badge: String,
    val explanation: String
)

data class StatusConfig(
    val color: Color,
    val bg: Color,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val msg: String
)

// ==========================================
// 2. CALCULATOR SCREEN
// ==========================================
@Composable
fun CalculatorScreen(
    viewModel: DropshipViewModel,
    onAddedToCatalog: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("80000") }
    var supplierPriceStr by remember { mutableStateOf("40000") }
    var commissionMlStr by remember { mutableStateOf("13") }
    var envioStr by remember { mutableStateOf("10000") }
    var platformFeeStr by remember { mutableStateOf("0") }
    var empaqueFeeStr by remember { mutableStateOf("0") }
    var returnsReserveStr by remember { mutableStateOf("8") }

    // Convert inputs safely
    val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
    val supplierPrice = supplierPriceStr.toDoubleOrNull() ?: 0.0
    val commissionMl = commissionMlStr.toDoubleOrNull() ?: 0.0
    val shippingCost = envioStr.toDoubleOrNull() ?: 0.0
    val platformFee = platformFeeStr.toDoubleOrNull() ?: 0.0
    val empaqueFee = empaqueFeeStr.toDoubleOrNull() ?: 0.0
    val returnsReserve = returnsReserveStr.toDoubleOrNull() ?: 0.0

    val result = viewModel.calculateProfit(
        sellingPrice = sellingPrice,
        supplierPrice = supplierPrice,
        commissionMlPercent = commissionMl,
        shippingCost = shippingCost,
        platformFee = platformFee,
        returnsReservePercent = returnsReserve,
        packagingCost = empaqueFee
    )

    val savedProducts by viewModel.savedProducts.collectAsStateWithLifecycle()

    LaunchedEffect(productName, sellingPriceStr, supplierPriceStr, commissionMlStr, envioStr, platformFeeStr, empaqueFeeStr, returnsReserveStr) {
        if (sellingPrice > 0 && supplierPrice > 0) {
            kotlinx.coroutines.delay(1000)
            viewModel.autoSaveCalculation(
                title = productName,
                supplierPrice = supplierPrice,
                supplierShippingCost = 0.0,
                sellingPrice = sellingPrice,
                commissionType = "${commissionMl}% Com",
                result = result
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SCREEN HEADER ---
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB5794A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = Color(0xFFFFFDF9),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Calculadora de Rentabilidad",
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = Color(0xFF2B2620)
                    )
                }
            }
            Text(
                text = "Calcula tu ganancia real después de comisiones, envíos, devoluciones y demás costos.",
                fontSize = 13.5.sp,
                color = Color(0xFF8A8272),
                modifier = Modifier.padding(top = 6.dp),
                lineHeight = 18.sp
            )
        }

        // --- SECTION 1: 💰 VENTA ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                border = BorderStroke(1.dp, Color(0xFFE8E1D2)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title = "💰 Venta",
                        icon = Icons.Default.AttachMoney,
                        iconColor = Color(0xFF2E7D32)
                    )

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Nombre del Producto (Opcional)", fontSize = 13.sp) },
                        placeholder = { Text("Ej: Pijama Térmica Polar") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .testTag("calc_product_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MLBlue,
                            unfocusedBorderColor = Color(0xFFDDD6C7)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    CalculatorField(
                        label = "Precio de venta",
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        suffix = "COP",
                        hint = "Precio final al que ofrecerás el producto al comprador",
                        testTag = "calc_selling_price_input"
                    )
                }
            }
        }

        // --- SECTION 2: 📦 PRODUCTO ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                border = BorderStroke(1.dp, Color(0xFFE8E1D2)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title = "📦 Producto",
                        icon = Icons.Default.Inventory2,
                        iconColor = Color(0xFF1565C0)
                    )

                    CalculatorField(
                        label = "Costo del producto",
                        value = supplierPriceStr,
                        onValueChange = { supplierPriceStr = it },
                        suffix = "COP",
                        hint = "Costo de adquisición, producción o compra unitaria del producto",
                        testTag = "calc_supplier_price_input"
                    )
                }
            }
        }

        // --- SECTION 3: 💸 COSTOS ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                border = BorderStroke(1.dp, Color(0xFFE8E1D2)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title = "💸 Costos",
                        icon = Icons.Default.ReceiptLong,
                        iconColor = Color(0xFFC62828)
                    )

                    CalculatorField(
                        label = "Comisión de la plataforma",
                        value = commissionMlStr,
                        onValueChange = { commissionMlStr = it },
                        suffix = "%",
                        hint = "Comisión porcentual por venta según el canal (ej. Mercado Libre, TikTok Shop, etc.)",
                        testTag = "calc_commission_pct_input"
                    )

                    CalculatorField(
                        label = "Costo de envío asumido por el vendedor",
                        value = envioStr,
                        onValueChange = { envioStr = it },
                        suffix = "COP",
                        hint = "Déjalo en 0 si el comprador paga el envío completo",
                        testTag = "calc_envio_input"
                    )

                    CalculatorField(
                        label = "Costo de la plataforma",
                        value = platformFeeStr,
                        onValueChange = { platformFeeStr = it },
                        suffix = "COP",
                        hint = "Cargo fijo por venta o suscripción prorrateada (si aplica)",
                        testTag = "calc_platform_fee_input"
                    )

                    CalculatorField(
                        label = "Empaque e insumos",
                        value = empaqueFeeStr,
                        onValueChange = { empaqueFeeStr = it },
                        suffix = "COP",
                        hint = "Bolsas, cajas, cinta o pegatinas por paquete",
                        testTag = "calc_empaque_input"
                    )
                }
            }
        }

        // --- SECTION 4: 🔄 RIESGO ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                border = BorderStroke(1.dp, Color(0xFFE8E1D2)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SectionHeader(
                        title = "🔄 Riesgo",
                        icon = Icons.Default.Autorenew,
                        iconColor = Color(0xFFEF6C00)
                    )

                    CalculatorField(
                        label = "Reserva para cancelaciones y devoluciones",
                        value = returnsReserveStr,
                        onValueChange = { returnsReserveStr = it },
                        suffix = "%",
                        hint = "Porcentaje estimado para cubrir devoluciones o no entregas (ej. 5% - 10%)",
                        testTag = "calc_devoluciones_input"
                    )
                }
            }
        }

        // --- SECTION 5: 📊 RESULTADO DE RENTABILIDAD ---
        if (sellingPrice > 0) {
            val marginNeto = result.margin
            val dineroRecibido = (sellingPrice - result.commissionFee - result.freeShippingFee - result.fixedFee).coerceAtLeast(0.0)

            val health = when {
                marginNeto >= 25.0 -> HealthStatus(
                    color = Color(0xFF1B5E20),
                    bg = Color(0xFFE8F5E9),
                    title = "🟩 Excelente",
                    badge = "Margen > 25%",
                    explanation = "¡Excelente nivel de rentabilidad! Cuentas con un amplio margen de seguridad para absorber imprevistos, hacer promociones o pauta digital."
                )
                marginNeto >= 15.0 -> HealthStatus(
                    color = Color(0xFFB78103),
                    bg = Color(0xFFFFF8E1),
                    title = "🟨 Aceptable",
                    badge = "Margen 15% - 25%",
                    explanation = "Tu margen neto es viable pero moderado. Monitorea de cerca la tasa de devoluciones e imprevistos de flete."
                )
                else -> HealthStatus(
                    color = Color(0xFFC62828),
                    bg = Color(0xFFFFEBEE),
                    title = "🟥 Riesgoso",
                    badge = "Margen < 15%",
                    explanation = "El margen es inferior al 15%. Una sola devolución o paquete no entregado puede dejar la operación en pérdida."
                )
            }

            // Percentages relative to selling price
            val pctProducto = (supplierPrice / sellingPrice * 100).coerceIn(0.0, 100.0)
            val pctComision = (result.commissionFee / sellingPrice * 100).coerceIn(0.0, 100.0)
            val pctEnvio = (result.freeShippingFee / sellingPrice * 100).coerceIn(0.0, 100.0)
            val pctPlataforma = (result.fixedFee / sellingPrice * 100).coerceIn(0.0, 100.0)
            val pctReserva = (result.taxWithholding / sellingPrice * 100).coerceIn(0.0, 100.0)
            val pctGanancia = (result.profit / sellingPrice * 100).coerceIn(-100.0, 100.0)

            // Dynamic recommendations
            val recommendations = mutableListOf<String>()
            if (marginNeto < 15.0) {
                recommendations.add("• El margen es muy bajo. Considera aumentar el precio de venta o negociar un costo menor con el proveedor.")
            } else if (marginNeto >= 25.0) {
                recommendations.add("• Excelente rentabilidad. Tienes un buen margen para invertir en pauta digital o campañas de descuento.")
            }

            if (pctEnvio > 15.0) {
                recommendations.add("• El costo del envío representa un ${String.format("%.1f", pctEnvio)}% elevado sobre el precio de venta. Evalúa optimizar el empaque o ajustar fletes.")
            }

            if (pctComision > 15.0) {
                recommendations.add("• La comisión de la plataforma (${String.format("%.1f", pctComision)}%) afecta considerablemente la utilidad.")
            }

            if (returnsReserve > 8.0) {
                recommendations.add("• La reserva de devoluciones (${String.format("%.1f", returnsReserve)}%) es alta. Descripciones más claras y buen embalaje ayudarán a reducir este gasto.")
            }

            if (recommendations.isEmpty()) {
                recommendations.add("• La estructura de costos se encuentra bien equilibrada para este producto.")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // 1. TARJETA DE RESUMEN EJECUTIVO
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Resumen de Rentabilidad",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Text(
                                text = "Ganancia Neta Estimada",
                                fontSize = 13.sp,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = formatCOP(result.profit),
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (result.profit >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            HorizontalDivider(
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📈 Margen Neto", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = "${String.format("%.1f", result.margin)}%",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("💵 Total Costos", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = formatCOP(result.totalCost),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFCA5A5)
                                    )
                                }
                                Column(modifier = Modifier.weight(1.2f)) {
                                    Text("💲 Dinero Recibido", fontSize = 11.5.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = formatCOP(dineroRecibido),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }

                    // 2. INDICADOR DE SALUD DEL NEGOCIO
                    Card(
                        colors = CardDefaults.cardColors(containerColor = health.bg),
                        border = BorderStroke(1.5.dp, health.color.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        tint = health.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = health.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = health.color
                                    )
                                }
                                Surface(
                                    color = health.color.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = health.badge,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = health.color,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = health.explanation,
                                fontSize = 13.sp,
                                color = Color(0xFF332F28),
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // 3. DESGLOSE DE COSTOS (% sobre precio de venta)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF9)),
                        border = BorderStroke(1.dp, Color(0xFFE8E1D2)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = Color(0xFFB5794A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Desglose de Costos sobre Venta",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2B2620)
                                )
                            }

                            Text(
                                text = "Precio de Venta: ${formatCOP(sellingPrice)} (100%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5B5548),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Visual multi-color progress bar preview
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFFE5E7EB), RoundedCornerShape(5.dp))
                            ) {
                                if (pctProducto > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight((pctProducto.toFloat() / 100f).coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(Color(0xFF1565C0))
                                    )
                                }
                                if (pctComision > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight((pctComision.toFloat() / 100f).coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(Color(0xFFC62828))
                                    )
                                }
                                if (pctEnvio > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight((pctEnvio.toFloat() / 100f).coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(Color(0xFFEF6C00))
                                    )
                                }
                                if (pctReserva > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight((pctReserva.toFloat() / 100f).coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(Color(0xFF6A1B9A))
                                    )
                                }
                                if (pctGanancia > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight((pctGanancia.toFloat() / 100f).coerceAtLeast(0.01f))
                                            .fillMaxHeight()
                                            .background(Color(0xFF2E7D32))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CostBreakdownRow("📦 Costo del producto", formatCOP(supplierPrice), pctProducto, Color(0xFF1565C0))
                            CostBreakdownRow("💳 Comisión de plataforma", formatCOP(result.commissionFee), pctComision, Color(0xFFC62828))
                            CostBreakdownRow("🚚 Costo de envío", formatCOP(result.freeShippingFee), pctEnvio, Color(0xFFEF6C00))
                            CostBreakdownRow("🛠️ Plataforma y empaque", formatCOP(result.fixedFee), pctPlataforma, Color(0xFF455A64))
                            CostBreakdownRow("🔄 Reserva devoluciones", formatCOP(result.taxWithholding), pctReserva, Color(0xFF6A1B9A))

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE8E1D2))

                            CostBreakdownRow("✨ Ganancia Neta", formatCOP(result.profit), pctGanancia, Color(0xFF2E7D32), isBold = true)
                        }
                    }

                    // 4. RECOMENDACIONES AUTOMÁTICAS
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Recomendaciones Estratégicas",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            recommendations.forEach { rec ->
                                Text(
                                    text = rec,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // 5. BOTONES DE ACCIÓN (Compartir & Guardar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Compartir resultado button
                        OutlinedButton(
                            onClick = {
                                val summaryText = buildString {
                                    appendLine("📊 CALCULADORA DE RENTABILIDAD")
                                    appendLine("📦 Producto: ${productName.ifBlank { "Producto en Simulación" }}")
                                    appendLine("💰 Precio de Venta: ${formatCOP(sellingPrice)}")
                                    appendLine("----------------------------------")
                                    appendLine("📦 Costo del Producto: ${formatCOP(supplierPrice)} (${String.format("%.1f", pctProducto)}%)")
                                    appendLine("💳 Comisión de Venta: ${formatCOP(result.commissionFee)} (${String.format("%.1f", pctComision)}%)")
                                    appendLine("🚚 Costo de Envío: ${formatCOP(result.freeShippingFee)} (${String.format("%.1f", pctEnvio)}%)")
                                    appendLine("🛠️ Plataforma / Empaque: ${formatCOP(result.fixedFee)} (${String.format("%.1f", pctPlataforma)}%)")
                                    appendLine("🔄 Reserva Devoluciones: ${formatCOP(result.taxWithholding)} (${String.format("%.1f", pctReserva)}%)")
                                    appendLine("----------------------------------")
                                    appendLine("🧾 Total Costos: ${formatCOP(result.totalCost)}")
                                    appendLine("💲 Dinero Recibido: ${formatCOP(dineroRecibido)}")
                                    appendLine("✨ GANANCIA NETA: ${formatCOP(result.profit)}")
                                    appendLine("📈 Margen Neto: ${String.format("%.1f", result.margin)}%")
                                    appendLine("🚦 Estado: ${health.title}")
                                }

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Compartir resultado de rentabilidad")
                                context.startActivity(shareIntent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_result_button"),
                            border = BorderStroke(1.5.dp, Color(0xFFB5794A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFFB5794A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartir", fontWeight = FontWeight.Bold, color = Color(0xFFB5794A), fontSize = 12.sp)
                        }

                        // Guardar simulación button
                        Button(
                            onClick = {
                                val savedCommissionType = "${String.format("%.1f", commissionMl)}% Com | ${String.format("%.1f", returnsReserve)}% Dev"
                                viewModel.saveCalculation(
                                    title = productName.ifBlank { "Simulación ${formatCOP(sellingPrice)}" },
                                    supplierPrice = supplierPrice,
                                    supplierShippingCost = shippingCost,
                                    sellingPrice = sellingPrice,
                                    commissionType = savedCommissionType,
                                    result = result
                                )
                                Toast.makeText(context, "Simulación guardada con éxito", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_simulation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB5794A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }

                        // Importar al Catálogo button
                        Button(
                            onClick = {
                                val score = viewModel.computeScore(sellingPrice, result.profit, result.margin, result.totalCost)
                                val stars = viewModel.computeStars(score)
                                viewModel.importCalculatedProductToCatalog(
                                    title = productName.ifBlank { "Producto Analizado (${formatCOP(sellingPrice)})" },
                                    category = "General",
                                    sellingPrice = sellingPrice,
                                    profit = result.profit,
                                    margin = result.margin,
                                    score = score,
                                    stars = stars,
                                    description = "Producto analizado en la Calculadora de Rentabilidad con $sellingPrice COP de precio."
                                )
                                Toast.makeText(context, "¡Producto añadido al Catálogo!", Toast.LENGTH_SHORT).show()
                                onAddedToCatalog?.invoke()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("add_to_catalog_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MLBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Al Catálogo", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Guía de referencia: 40%+ de margen bruto = sano · 25-40% = ajustado · menos de 25% = riesgoso",
                fontSize = 11.5.sp,
                color = Color(0xFF9C9481),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                lineHeight = 15.sp
            )
        }

        // List of saved simulations
        if (savedProducts.isNotEmpty()) {
            item {
                Text(
                    text = "Simulaciones Guardadas (${savedProducts.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(savedProducts) { product ->
                SavedProductCard(product, onDelete = { viewModel.deleteProduct(product.id) })
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false, isAccent: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isAccent) MLOrange else Color(0xFF5B5548),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (isAccent) MLOrange else Color(0xFF2B2620),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

@Composable
fun SavedProductCard(product: DropshipProduct, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MLCardBackground),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Venta ML: ${formatCOP(product.sellingPrice)} | Prov: ${formatCOP(product.supplierPrice)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MLRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isCustom = product.commissionType.contains("|")
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCustom) MLBlue.copy(alpha = 0.1f) else if (product.commissionType == "clasica") MLBlue.copy(alpha = 0.15f) else MLYellow.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.commissionType.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustom) MLDarkBlue else MLBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (product.hasFreeShipping) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MLGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ASUME ENVÍO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MLGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ganancia: ", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = formatCOP(product.calculatedProfit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (product.calculatedProfit > 0) MLGreen else MLRed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${String.format("%.1f", product.calculatedMargin)}%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (product.calculatedMargin > 20) MLGreen else if (product.calculatedMargin > 0) MLOrange else MLRed
                    )
                }
            }
        }
    }
}

fun formatCOP(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

// ==========================================
// 3. CHECKLIST SCREEN
// ==========================================
@Composable
fun ChecklistScreen(viewModel: DropshipViewModel) {
    val items by viewModel.checklistItems.collectAsStateWithLifecycle()

    val totalItems = items.size
    val completedItems = items.count { it.isCompleted }
    val progressPercent = if (totalItems > 0) (completedItems.toFloat() / totalItems.toFloat()) else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Tu Plan de Ruta para el Lanzamiento",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MLDarkBlue
            )
            Text(
                text = "Completa esta lista de tareas fundamentales antes y durante la publicación de tus primeros artículos para mitigar riesgos en Mercado Libre.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Progress bar card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MLCardBackground),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progreso de Lanzamiento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$completedItems / $totalItems (${(progressPercent * 100).toInt()}%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MLBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .testTag("checklist_progress_bar"),
                        color = MLBlue,
                        trackColor = Color.LightGray.copy(alpha = 0.4f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }

        // Sectioned list items
        val sections = listOf(
            "inicio" to "Fase 1: Configuración Inicial",
            "reputacion" to "Fase 2: Estrategia de Reputación",
            "logistica" to "Fase 3: Ejecución de Despachos",
            "ropa" to "Fase 4: Categoría Ropa (Prendas de Vestir)"
        )

        sections.forEach { (sectionKey, sectionTitle) ->
            val sectionItems = items.filter { it.section == sectionKey }
            if (sectionItems.isNotEmpty()) {
                item {
                    Text(
                        text = sectionTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(sectionItems) { checklistItem ->
                    ChecklistItemRow(checklistItem, onToggle = { isChecked ->
                        viewModel.toggleChecklistItem(checklistItem.id, isChecked)
                    })
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ChecklistItemRow(item: ChecklistItem, onToggle: (Boolean) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MLCardBackground),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = MLBlue),
                modifier = Modifier.testTag("checkbox_${item.id}")
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (item.isCompleted) Color.Gray else Color.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = if (item.isCompleted) Color.Gray.copy(alpha = 0.8f) else Color.DarkGray
                )
            }
        }
    }
}

// ==========================================
// 4. CLOTHING SIZES SCREEN
// ==========================================
@Composable
fun ClothingSizesScreen() {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var selectedGender by remember { mutableStateOf("Mujer") } // "Mujer" or "Hombre"
    var selectedItemType by remember { mutableStateOf("Pijama / Ropa Hogar") } // "Pijama / Ropa Hogar" or "Camisa / Blusa"
    var chestInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var hipsInput by remember { mutableStateOf("") }

    var calculatedSize by remember { mutableStateOf("") }
    var sizeDetailsText by remember { mutableStateOf("") }

    // Logic to calculate Colombian Standard Clothing Size
    fun calculateSizing() {
        val chest = chestInput.toDoubleOrNull() ?: 0.0
        val waist = waistInput.toDoubleOrNull() ?: 0.0
        val hips = hipsInput.toDoubleOrNull() ?: 0.0

        if (chest <= 0.0 || waist <= 0.0) {
            Toast.makeText(context, "Por favor ingresa pecho y cintura válidos", Toast.LENGTH_SHORT).show()
            return
        }

        // Sizing thresholds (Standard guidelines in Colombia for clothing)
        val size = if (selectedGender == "Mujer") {
            when {
                chest <= 85.0 && waist <= 65.0 -> "S (Talla 6-8)"
                chest <= 93.0 && waist <= 73.0 -> "M (Talla 10)"
                chest <= 101.0 && waist <= 81.0 -> "L (Talla 12-14)"
                chest <= 109.0 && waist <= 89.0 -> "XL (Talla 16)"
                else -> "XXL (Talla 18+)"
            }
        } else {
            // Hombre
            when {
                chest <= 95.0 && waist <= 80.0 -> "S (Talla 28-30)"
                chest <= 103.0 && waist <= 88.0 -> "M (Talla 32-34)"
                chest <= 111.0 && waist <= 96.0 -> "L (Talla 36-38)"
                chest <= 119.0 && waist <= 104.0 -> "XL (Talla 40-42)"
                else -> "XXL (Talla 44+)"
            }
        }

        calculatedSize = size

        // Generate copyable text template
        sizeDetailsText = """
📢 GUÍA DE TALLAS - LEER DETALLADAMENTE ANTES DE COMPRAR

Para garantizar que tu prenda de vestir (Género: $selectedGender, Tipo: $selectedItemType) te quede perfecta, te sugerimos verificar tus medidas en centímetros:

📏 Talla recomendada según tus medidas: $size
📌 Medidas base sugeridas:
- Pecho / Busto: ${if (chest > 0) "$chest cm" else "N/A"}
- Cintura: ${if (waist > 0) "$waist cm" else "N/A"}
- Cadera: ${if (hips > 0) "$hips cm" else "N/A"}

💡 RECOMENDACIÓN DROPSHIPPING COLOMBIA:
Esta prenda viene con horma colombiana estándar. Si prefieres usar las pijamas u hogar holgados, te aconsejamos comprar UNA TALLA ADICIONAL de la recomendada para evitar solicitudes de devolución por ajuste. ¡Agradecemos tu preferencia!
        """.trimIndent()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Asistente de Tallas (Minimiza Devoluciones)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MLDarkBlue
            )
            Text(
                text = "Una de las mayores pérdidas en Dropshipping Colombia son los costos de flete por cambios de prendas de vestir. Calcula la talla exacta y genera un bloque de texto adaptado para copiar en la descripción de Mercado Libre.",
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MLCardBackground),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Especificaciones de la Prenda",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Gender Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { selectedGender = "Mujer" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedGender == "Mujer") MLBlue else Color.LightGray.copy(alpha = 0.5f),
                                contentColor = if (selectedGender == "Mujer") Color.White else Color.DarkGray
                            )
                        ) {
                            Text("Mujer", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedGender = "Hombre" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedGender == "Hombre") MLBlue else Color.LightGray.copy(alpha = 0.5f),
                                contentColor = if (selectedGender == "Hombre") Color.White else Color.DarkGray
                            )
                        ) {
                            Text("Hombre", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Item type select
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tipo de Prenda:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        val types = listOf("Pijama / Ropa Hogar", "Camisa / Blusa")
                        Box {
                            var expanded by remember { mutableStateOf(false) }
                            Button(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MLBackgroundLight, contentColor = Color.Black)
                            ) {
                                Text(selectedItemType, fontSize = 12.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                types.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedItemType = type
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = Color.LightGray.copy(alpha = 0.5f))

                    Text(
                        text = "Medidas del Comprador en Centímetros",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chestInput,
                            onValueChange = { chestInput = it },
                            label = { Text("Pecho (cm)", fontSize = 11.sp) },
                            placeholder = { Text("Ej: 90") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chest_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = waistInput,
                            onValueChange = { waistInput = it },
                            label = { Text("Cuntura (cm)", fontSize = 11.sp) },
                            placeholder = { Text("Ej: 70") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("waist_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = hipsInput,
                            onValueChange = { hipsInput = it },
                            label = { Text("Cadera (cm)", fontSize = 11.sp) },
                            placeholder = { Text("Ej: 95") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hips_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = { calculateSizing() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculate_sizing_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MLBlue)
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calcular Talla y Generar Guía", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sizing result output
        if (calculatedSize.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MLCardBackground),
                    elevation = CardDefaults.cardElevation(3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Talla Sugerida:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MLGreen),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = calculatedSize,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Guía de Tallas para tu Publicación (Vista Previa):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MLDarkBlue
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MLBackgroundLight),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = sizeDetailsText,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(10.dp),
                                lineHeight = 16.sp
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(sizeDetailsText))
                                Toast.makeText(context, "Guía de tallas copiada al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_sizing_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MLGreen)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copiar Guía de Tallas", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ==========================================
// 5. AI ADVISOR SCREEN
// ==========================================
@Composable
fun AiAdvisorScreen(viewModel: DropshipViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Suggestions chips list
    val suggestions = listOf(
        "¿Cómo subo las etiquetas a Dropi?",
        "¿Qué pasa si se agota el stock?",
        "¿Se puede vender contra entrega?",
        "¿Cómo evito que suspendan mi cuenta?"
    )

    // Automatically scroll to the latest message when it arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Title Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MLGreen, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Chat DropiML: Consultas Inteligentes",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
        }

        Text(
            text = "Haz preguntas sobre comisiones, reglas, transportadoras colombianas o solución de problemas en bodegas.",
            fontSize = 11.5.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Suggestion chips row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { suggestion ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MLBlue.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clickable { viewModel.sendChatMessage(suggestion) }
                                    .testTag("suggestion_chip_$suggestion")
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 11.sp,
                                    color = MLDarkBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.25f)),
                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MLBlue
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Escribiendo respuesta de DropiML...",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input bottom bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                placeholder = { Text("Escribe tu consulta aquí...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MLBlue,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Button(
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        viewModel.sendChatMessage(inputMessage)
                        inputMessage = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MLBlue),
                modifier = Modifier
                    .testTag("chat_send_button")
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val containerColor = if (message.isUser) MLBlue else Color.White
    val textColor = if (message.isUser) Color.White else Color.Black
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 0.dp)
    } else {
        RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = shape,
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
        Text(
            text = if (message.isUser) "Tú" else "DropiML Asesor",
            fontSize = 9.5.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
        )
    }
}

// ==========================================
// 6. RANKING DE PRODUCTOS SCREEN
// ==========================================
@Composable
fun RankingScreen(viewModel: DropshipViewModel, onNavigateToCompare: () -> Unit) {
    val savedProducts by viewModel.savedProducts.collectAsStateWithLifecycle()
    val selectedProductIds by viewModel.selectedProductIds.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("Puntaje") }

    // Statistics computation
    val totalCount = savedProducts.size
    val excelentesCount = savedProducts.count { it.calculatedMargin >= 25.0 || it.score >= 80 }
    val aceptablesCount = savedProducts.count { (it.calculatedMargin in 15.0..24.9) || (it.score in 50..79) }
    val riesgosoCount = savedProducts.count { it.calculatedMargin < 15.0 && it.score < 50 }
    val avgProfit = if (savedProducts.isNotEmpty()) savedProducts.map { it.calculatedProfit }.average() else 0.0
    val avgMargin = if (savedProducts.isNotEmpty()) savedProducts.map { it.calculatedMargin }.average() else 0.0
    val bestProduct = savedProducts.maxByOrNull { it.score }

    val filteredList = savedProducts.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    val sortedList = remember(filteredList, sortOption) {
        when (sortOption) {
            "Puntaje" -> filteredList.sortedByDescending { it.score }
            "Ganancia" -> filteredList.sortedByDescending { it.calculatedProfit }
            "Margen" -> filteredList.sortedByDescending { it.calculatedMargin }
            "Reciente" -> filteredList.sortedByDescending { it.timestamp }
            "Riesgo" -> filteredList.sortedBy { it.supplierPrice / (it.sellingPrice.takeIf { p -> p > 0 } ?: 1.0) }
            "Nombre" -> filteredList.sortedBy { it.title.lowercase() }
            else -> filteredList.sortedByDescending { it.score }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HEADER & DESCRIPTION
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White)
                        }
                    }
                    Column {
                        Text("Ranking de Productos", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
                        Text("Asistente para seleccionar los productos más rentables para tu tienda.", fontSize = 12.5.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // DASHBOARD DE ESTADÍSTICAS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📊 Estadísticas del Historial", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("📦 Analizados", "$totalCount", Color.White)
                            StatBox("🟢 Excelentes", "$excelentesCount", Color(0xFF4ADE80))
                            StatBox("🟡 Aceptables", "$aceptablesCount", Color(0xFFFACC15))
                            StatBox("🔴 Riesgosos", "$riesgosoCount", Color(0xFFF87171))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("💰 Ganancia Promedio", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(formatCOP(avgProfit), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                            }
                            Column {
                                Text("📈 Margen Promedio", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text("${String.format("%.1f", avgMargin)}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                            Column {
                                Text("🏆 Mejor Producto", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                Text(bestProduct?.title?.take(14) ?: "N/A", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFACC15))
                            }
                        }
                    }
                }
            }

            // BÚSQUEDA Y FILTROS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar producto por nombre...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ranking_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MLBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    // Filtros
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filterOptions = listOf(
                            "Puntaje" to "🏆 Mayor Puntaje",
                            "Ganancia" to "💰 Mayor Ganancia",
                            "Margen" to "📈 Mayor Margen",
                            "Reciente" to "🕒 Más Reciente",
                            "Riesgo" to "🛡️ Menor Riesgo",
                            "Nombre" to "🔤 Nombre A-Z"
                        )

                        filterOptions.forEach { (key, label) ->
                            FilterChip(
                                selected = sortOption == key,
                                onClick = { sortOption = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (sortOption == key) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MLBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // LISTA DE PRODUCTOS CON RANKING
            if (sortedList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No se encontraron productos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Realiza un cálculo en la pestaña 'Calculadora' para registrar productos automáticamente.", fontSize = 12.5.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                itemsIndexed(sortedList) { index, product ->
                    RankingProductCard(
                        rankPosition = index + 1,
                        product = product,
                        isSelected = selectedProductIds.contains(product.id),
                        onToggleSelect = { viewModel.toggleProductSelection(product.id) },
                        onDelete = { viewModel.deleteProduct(product.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FLOATING BAR SI HAY SELECCIONADOS PARA COMPARAR
        if (selectedProductIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${selectedProductIds.size} de 3 seleccionados",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Listos para comparar lado a lado",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.clearSelection() }) {
                            Text("Limpiar", color = Color(0xFFF87171), fontSize = 12.sp)
                        }
                        Button(
                            onClick = onNavigateToCompare,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Comparar", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankingProductCard(
    rankPosition: Int,
    product: DropshipProduct,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeText = when (rankPosition) {
        1 -> "🥇 #1"
        2 -> "🥈 #2"
        3 -> "🥉 #3"
        else -> "#$rankPosition"
    }

    val badgeBg = when (rankPosition) {
        1 -> Color(0xFFFEF3C7)
        2 -> Color(0xFFF1F5F9)
        3 -> Color(0xFFFFEDD5)
        else -> Color(0xFFF8FAFC)
    }

    val badgeColor = when (rankPosition) {
        1 -> Color(0xFFD97706)
        2 -> Color(0xFF475569)
        3 -> Color(0xFFC2410C)
        else -> Color(0xFF64748B)
    }

    val starsStr = "⭐".repeat(product.stars.coerceIn(1, 5))

    val healthConfig = when {
        product.calculatedMargin >= 25.0 || product.score >= 80 -> HealthBadgeConfig("🟢 Excelente", Color(0xFF166534), Color(0xFFDCFCE7))
        product.calculatedMargin >= 18.0 || product.score >= 65 -> HealthBadgeConfig("🟢 Muy buena", Color(0xFF15803D), Color(0xFFF0FDF4))
        product.calculatedMargin >= 12.0 || product.score >= 50 -> HealthBadgeConfig("🟡 Aceptable", Color(0xFF854D0E), Color(0xFFFEF9C3))
        product.calculatedMargin >= 5.0 || product.score >= 35 -> HealthBadgeConfig("🟠 Riesgoso", Color(0xFFC2410C), Color(0xFFFFEDD5))
        else -> HealthBadgeConfig("🔴 No recomendado", Color(0xFF991B1B), Color(0xFFFEE2E2))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MLBlue else Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Rank badge, Name, Select Checkbox, Delete
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() }
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Score and Stars Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(starsStr, fontSize = 14.sp)
                    Text("(${product.score}/100 pts)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                }

                Surface(
                    color = healthConfig.bg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = healthConfig.text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = healthConfig.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 6.dp))

            // Financial Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("💰 Ganancia Neta", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        formatCOP(product.calculatedProfit),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (product.calculatedProfit >= 0) Color(0xFF166534) else Color(0xFF991B1B)
                    )
                }
                Column {
                    Text("📈 Margen", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        "${String.format("%.1f", product.calculatedMargin)}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
                Column {
                    Text("💵 Costos Totales", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                        formatCOP(product.totalCost.takeIf { it > 0 } ?: (product.sellingPrice - product.calculatedProfit)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

data class HealthBadgeConfig(
    val text: String,
    val color: Color,
    val bg: Color
)

@Composable
fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.5.sp, color = Color(0xFF94A3B8))
    }
}

// ==========================================
// 7. COMPARADOR DE PRODUCTOS SCREEN
// ==========================================
@Composable
fun ComparisonScreen(viewModel: DropshipViewModel) {
    val savedProducts by viewModel.savedProducts.collectAsStateWithLifecycle()
    val selectedProductIds by viewModel.selectedProductIds.collectAsStateWithLifecycle()

    val selectedProducts = savedProducts.filter { selectedProductIds.contains(it.id) }

    if (selectedProducts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = MLBlue,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Comparador de Productos",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Selecciona hasta 3 productos en la pestaña 'Ranking' marcando sus casillas para compararlos aquí lado a lado.",
                        fontSize = 13.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    } else {
        val winnerProduct = selectedProducts.maxByOrNull { it.score * 1000 + it.calculatedProfit }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, tint = Color(0xFF38BDF8))
                        }
                    }
                    Column {
                        Text("Comparación Lado a Lado", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B))
                        Text("Analiza las diferencias clave para tomar la mejor decisión.", fontSize = 12.5.sp, color = Color(0xFF64748B))
                    }
                }
            }

            // WINNER HIGHLIGHT BANNER
            if (winnerProduct != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(24.dp))
                                Text(
                                    text = "🏆 OPCIÓN RECOMENDADA: ${winnerProduct.title}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF065F46)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Este producto ofrece la mejor combinación de puntaje (${winnerProduct.score}/100), utilidad estimada (${formatCOP(winnerProduct.calculatedProfit)}) y margen neto (${String.format("%.1f", winnerProduct.calculatedMargin)}%).",
                                fontSize = 13.sp,
                                color = Color(0xFF047857),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // SIDE BY SIDE COMPARISON CARDS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedProducts.forEach { prod ->
                        val isWinner = prod.id == winnerProduct?.id
                        ComparisonProductColumn(
                            product = prod,
                            isWinner = isWinner,
                            onRemove = { viewModel.toggleProductSelection(prod.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonProductColumn(
    product: DropshipProduct,
    isWinner: Boolean,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isWinner) Color(0xFFF0FDF4) else Color.White),
        border = BorderStroke(if (isWinner) 2.dp else 1.dp, if (isWinner) Color(0xFF10B981) else Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWinner) {
                    Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(6.dp)) {
                        Text("🏆 Ganador", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar", tint = Color.Gray)
                }
            }

            Text(
                text = product.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF1E293B),
                maxLines = 2,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Stars & Score
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("⭐".repeat(product.stars.coerceIn(1, 5)), fontSize = 12.sp)
                Text("${product.score}/100", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

            CompMetricRow("💰 Ganancia", formatCOP(product.calculatedProfit), isBold = true, color = if (product.calculatedProfit >= 0) Color(0xFF166534) else Color(0xFF991B1B))
            CompMetricRow("📈 Margen", "${String.format("%.1f", product.calculatedMargin)}%", isBold = true, color = Color(0xFF1E293B))
            CompMetricRow("🏷️ Precio Venta", formatCOP(product.sellingPrice))
            CompMetricRow("📦 Costo Prod.", formatCOP(product.supplierPrice))
            CompMetricRow("💵 Costos Tot.", formatCOP(product.totalCost.takeIf { it > 0 } ?: (product.sellingPrice - product.calculatedProfit)))
            CompMetricRow("🚦 Estado", product.healthState, color = if (product.calculatedMargin >= 25) Color(0xFF166534) else Color(0xFFD97706))
        }
    }
}

@Composable
fun CompMetricRow(label: String, value: String, isBold: Boolean = false, color: Color = Color(0xFF334155)) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
        Text(value, fontSize = 13.5.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = color)
    }
}

