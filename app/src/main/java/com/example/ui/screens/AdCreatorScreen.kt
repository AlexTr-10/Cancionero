package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CatalogProduct
import com.example.data.model.CompanyProfile
import com.example.ui.viewmodel.DropshipViewModel
import com.example.util.AdGeneratorExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdCreatorScreen(
    viewModel: DropshipViewModel,
    initialProduct: CatalogProduct? = null,
    initialProducts: List<CatalogProduct> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allCatalogProducts by viewModel.catalogProducts.collectAsStateWithLifecycle(initialValue = emptyList())
    val companyProfileState by viewModel.companyProfile.collectAsStateWithLifecycle(initialValue = null)

    val companyProfile = remember(companyProfileState) {
        companyProfileState ?: CompanyProfile(
            id = 1,
            name = "Mi Negocio",
            slogan = "¡Calidad y los mejores precios!",
            description = "Atención personalizada y envíos rápidos",
            whatsapp = "+57 300 000 0000",
            phone = "3000000000",
            address = "Calle Comercial #123",
            city = "Bogotá",
            primaryColorHex = "#DC2626"
        )
    }

    // Selected products state
    var selectedProducts by remember {
        mutableStateOf(
            if (initialProducts.isNotEmpty()) initialProducts
            else if (initialProduct != null) listOf(initialProduct)
            else if (allCatalogProducts.isNotEmpty()) listOf(allCatalogProducts.first())
            else emptyList()
        )
    }

    var showProductPickerSheet by remember { mutableStateOf(false) }

    // Ad configuration states
    var templateType by remember { mutableStateOf(AdGeneratorExporter.AdTemplateType.UNIPRODUCTO_A4) }
    var badgeText by remember { mutableStateOf("¡OFERTA ESPECIAL!") }
    var discountPercent by remember { mutableIntStateOf(20) }
    var customPromoPriceText by remember { mutableStateOf("") }
    var callToAction by remember { mutableStateOf("¡Pide el tuyo ahora por WhatsApp!") }
    var showQrCode by remember { mutableStateOf(true) }
    var primaryColorHex by remember { mutableStateOf("#DC2626") }

    // Preset options
    val badgePresets = listOf(
        "¡OFERTA ESPECIAL!",
        "LIQUIDACIÓN TOTAL",
        "NUEVO INGRESO",
        "SUPER DESCUENTO",
        "PRECIO IMPERDIBLE",
        "¡ÚLTIMAS UNIDADES!"
    )

    val ctaPresets = listOf(
        "¡Pide el tuyo ahora por WhatsApp!",
        "¡Escríbenos y agenda tu envío!",
        "¡Envíos a todo el país!",
        "¡Aprovecha antes de que se agote!"
    )

    val colorPresets = listOf(
        "#DC2626" to "Rojo Oferta",
        "#1D4ED8" to "Azul Real",
        "#16A34A" to "Verde Neón",
        "#7C3AED" to "Púrpura",
        "#D97706" to "Dorado",
        "#0F172A" to "Negro Elegante"
    )

    val currentConfig = remember(
        templateType, badgeText, discountPercent, customPromoPriceText,
        callToAction, showQrCode, primaryColorHex
    ) {
        val parsedCustomPrice = customPromoPriceText.toDoubleOrNull()
        AdGeneratorExporter.AdConfig(
            templateType = templateType,
            badgeText = badgeText,
            discountPercent = discountPercent,
            customPromoPrice = parsedCustomPrice,
            callToAction = callToAction,
            showQrCode = showQrCode,
            primaryColorHex = primaryColorHex
        )
    }

    // Rendered Bitmap preview
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingPreview by remember { mutableStateOf(false) }

    // Update preview bitmap asynchronously when inputs change
    LaunchedEffect(currentConfig, selectedProducts, companyProfile) {
        if (selectedProducts.isNotEmpty()) {
            isGeneratingPreview = true
            withContext(Dispatchers.Default) {
                val bmp = AdGeneratorExporter.generateAdBitmap(
                    context = context,
                    profile = companyProfile,
                    products = selectedProducts,
                    config = currentConfig
                )
                previewBitmap = bmp
            }
            isGeneratingPreview = false
        } else {
            previewBitmap = null
        }
    }

    // Product Picker Dialog
    if (showProductPickerSheet) {
        AlertDialog(
            onDismissRequest = { showProductPickerSheet = false },
            title = {
                Text("Seleccionar Producto(s) para Publicidad", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCatalogProducts) { product ->
                        val isSelected = selectedProducts.any { it.id == product.id }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProducts = if (isSelected) {
                                        if (selectedProducts.size > 1) selectedProducts.filter { it.id != product.id }
                                        else selectedProducts // Keep at least one
                                    } else {
                                        selectedProducts + product
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        selectedProducts = if (isSelected) {
                                            if (selectedProducts.size > 1) selectedProducts.filter { it.id != product.id }
                                            else selectedProducts
                                        } else {
                                            selectedProducts + product
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Ref: ${product.sku} | $${String.format("%,.0f", product.sellingPrice)}", fontSize = 11.5.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showProductPickerSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Aceptar (${selectedProducts.size})")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("📢 Creador de Publicidad", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
                        Text("Diseña volantes y estados listos para enviar", fontSize = 11.5.sp, color = Color(0xFFE0F2FE))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 12.dp,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedProducts.isEmpty()) {
                                Toast.makeText(context, "Selecciona al menos un producto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            AdGeneratorExporter.exportAndShareImage(context, companyProfile, selectedProducts, currentConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_ad_image_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir PNG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (selectedProducts.isEmpty()) {
                                Toast.makeText(context, "Selecciona al menos un producto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            AdGeneratorExporter.exportAndSharePdf(context, companyProfile, selectedProducts, currentConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_ad_pdf_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: LIVE REAL-TIME PREVIEW CANVAS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Preview, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                Text("Vista Previa en Tiempo Real", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                            Text(templateType.displayName, fontSize = 11.5.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 280.dp, max = 380.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isGeneratingPreview) {
                                CircularProgressIndicator(color = Color(0xFF2563EB))
                            } else if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Vista previa publicidad",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("No hay productos seleccionados", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // SECTION 2: PRODUCT SELECTION CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Text("Producto(s) para la Publicidad", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                            }
                            OutlinedButton(
                                onClick = { showProductPickerSheet = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("change_ad_products_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cambiar (${selectedProducts.size})", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(selectedProducts) { p ->
                                FilterChip(
                                    selected = true,
                                    onClick = { },
                                    label = { Text(p.name, maxLines = 1, fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: TEMPLATE STYLE SELECTION
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("📐 Formato y Tipo de Plantilla", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(10.dp))

                        AdGeneratorExporter.AdTemplateType.values().forEach { t ->
                            val isSelected = templateType == t
                            Surface(
                                color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { templateType = t }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { templateType = t },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(t.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text(t.description, fontSize = 11.5.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: PROMOTIONAL BADGE & DISCOUNT CONFIG
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🏷️ Insignia & Descuento de Promoción", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Badge Text
                        OutlinedTextField(
                            value = badgeText,
                            onValueChange = { badgeText = it },
                            label = { Text("Texto de la Insignia / Encabezado") },
                            leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset Badge Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(badgePresets) { preset ->
                                FilterChip(
                                    selected = badgeText == preset,
                                    onClick = { badgeText = preset },
                                    label = { Text(preset, fontSize = 11.5.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Discount Percentage Slider / Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Porcentaje de Descuento:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("$discountPercent% OFF", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                        }

                        val discountChips = listOf(0, 10, 15, 20, 25, 30, 40, 50)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                            items(discountChips) { d ->
                                FilterChip(
                                    selected = discountPercent == d,
                                    onClick = {
                                        discountPercent = d
                                        customPromoPriceText = ""
                                    },
                                    label = { Text(if (d == 0) "Sin desc." else "-$d%", fontSize = 11.5.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Promo Price
                        OutlinedTextField(
                            value = customPromoPriceText,
                            onValueChange = { customPromoPriceText = it },
                            label = { Text("O ingresar Precio Promocional Fijo ($)") },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // SECTION 5: CALL TO ACTION & COLOR ACCENT
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("🎨 Estilo Visual & Llamado a la Acción (CTA)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color(0xFF0F172A))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Color Preset Picker
                        Text("Color Destacado Principal:", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorPresets.forEach { (hex, name) ->
                                val isSelected = primaryColorHex.equals(hex, ignoreCase = true)
                                val colorVal = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Red }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colorVal)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.Black else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { primaryColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = name, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // CTA Text
                        OutlinedTextField(
                            value = callToAction,
                            onValueChange = { callToAction = it },
                            label = { Text("Mensaje Llamado a la Acción (CTA)") },
                            leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            items(ctaPresets) { cta ->
                                FilterChip(
                                    selected = callToAction == cta,
                                    onClick = { callToAction = cta },
                                    label = { Text(cta, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Include QR Code switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color(0xFF2563EB))
                                Text("Incluir Código QR de WhatsApp", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            }
                            Switch(
                                checked = showQrCode,
                                onCheckedChange = { showQrCode = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2563EB))
                            )
                        }
                    }
                }
            }

            // SECTION 6: COMPANY BRANDING SUMMARY
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF0284C7))
                            Text("Datos Empresariales Incluidos Automáticamente", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0369A1))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("• Negocio: ${companyProfile.name}", fontSize = 11.5.sp, color = Color(0xFF0369A1))
                        Text("• WhatsApp: ${companyProfile.whatsapp}", fontSize = 11.5.sp, color = Color(0xFF0369A1))
                        Text("• Dirección: ${companyProfile.address}, ${companyProfile.city}", fontSize = 11.5.sp, color = Color(0xFF0369A1))
                        if (companyProfile.instagram.isNotBlank() || companyProfile.facebook.isNotBlank()) {
                            Text("• Redes: ${companyProfile.instagram} ${companyProfile.facebook}", fontSize = 11.5.sp, color = Color(0xFF0369A1))
                        }
                    }
                }
            }
        }
    }
}
