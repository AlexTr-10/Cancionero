package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.CatalogProduct
import com.example.data.model.CompanyProfile
import com.example.ui.viewmodel.DropshipViewModel
import com.example.util.CatalogPdfExporter
import com.example.util.PhotoStorageHelper
import java.text.NumberFormat
import java.util.Locale

enum class CatalogViewMode {
    LIST, GRID, STORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(viewModel: DropshipViewModel, onNavigateToCompany: () -> Unit) {
    val context = LocalContext.current
    val catalogProducts by viewModel.catalogProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val companyProfile by viewModel.companyProfile.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var selectedStatus by remember { mutableStateOf("Todos") }
    var selectedTag by remember { mutableStateOf("Todos") }
    var viewMode by remember { mutableStateOf(CatalogViewMode.GRID) }

    var selectedAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var showAdCreator by remember { mutableStateOf(false) }
    var adCreatorProduct by remember { mutableStateOf<CatalogProduct?>(null) }
    var adCreatorProducts by remember { mutableStateOf<List<CatalogProduct>>(emptyList()) }
    var selectedProductIds by remember { mutableStateOf(setOf<Int>()) }
    var productToEdit by remember { mutableStateOf<CatalogProduct?>(null) }
    var productToView by remember { mutableStateOf<CatalogProduct?>(null) }

    val isSelectionMode = selectedProductIds.isNotEmpty()

    // Filter Logic
    val filteredProducts = catalogProducts.filter { prod ->
        val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true) ||
                prod.category.contains(searchQuery, ignoreCase = true) ||
                prod.brand.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == "Todas" || prod.category.equals(selectedCategory, ignoreCase = true)
        val matchesStatus = selectedStatus == "Todos" || prod.status.equals(selectedStatus, ignoreCase = true)
        val matchesTag = selectedTag == "Todos" || prod.tags.contains(selectedTag, ignoreCase = true)

        matchesSearch && matchesCategory && matchesStatus && matchesTag
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Nuevo Producto", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = MLBlue,
                modifier = Modifier.testTag("catalog_add_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MLBackgroundLight)
        ) {
            // TOP BANNER & SEARCH BAR
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // MULTI-SELECTION CONTEXTUAL ACTION BAR
                    AnimatedVisibility(visible = isSelectionMode) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("multi_selection_bar")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { selectedProductIds = emptySet() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancelar Selección", tint = Color.White)
                                    }
                                    Text(
                                        text = "${selectedProductIds.size} seleccionado(s)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            if (selectedProductIds.size == filteredProducts.size) {
                                                selectedProductIds = emptySet()
                                            } else {
                                                selectedProductIds = filteredProducts.map { it.id }.toSet()
                                            }
                                        }
                                    ) {
                                        Text(
                                            if (selectedProductIds.size == filteredProducts.size) "Deseleccionar" else "Todos",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val selected = filteredProducts.filter { it.id in selectedProductIds }
                                            if (selected.isNotEmpty()) {
                                                adCreatorProduct = null
                                                adCreatorProducts = selected
                                                showAdCreator = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("bulk_create_ad_btn")
                                    ) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Crear Publicidad", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedProductIds.forEach { id -> viewModel.deleteCatalogProduct(id) }
                                            selectedProductIds = emptySet()
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar Selección", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Catálogo de Productos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${filteredProducts.size} productos registrados",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // PDF Export Button
                            Button(
                                onClick = { showPdfExportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_pdf_export_dialog_btn")
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Exportar PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar por nombre, SKU, categoría o marca...", fontSize = 13.sp) },
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
                            .testTag("catalog_search_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MLBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // View Mode Switcher + Add Category Button + Select Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Categories filter chips
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedCategory == "Todas",
                                onClick = { selectedCategory = "Todas" },
                                label = { Text("Todas", fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MLBlue, selectedLabelColor = Color.White)
                            )

                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat.name,
                                    onClick = { selectedCategory = cat.name },
                                    label = { Text(cat.name, fontSize = 11.5.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MLBlue, selectedLabelColor = Color.White)
                                )
                            }

                            IconButton(
                                onClick = { showAddCategoryDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Nueva Categoría", tint = MLBlue)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // View mode toggle buttons + Multi-selection button
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedProductIds = emptySet()
                                    } else if (filteredProducts.isNotEmpty()) {
                                        selectedProductIds = setOf(filteredProducts.first().id)
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("toggle_selection_mode_btn")
                            ) {
                                Icon(
                                    Icons.Default.Checklist,
                                    contentDescription = "Seleccionar Varios",
                                    tint = if (isSelectionMode) MLBlue else Color.Gray
                                )
                            }

                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFCBD5E1)))

                            IconButton(
                                onClick = { viewMode = CatalogViewMode.LIST },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ViewList,
                                    contentDescription = "Lista",
                                    tint = if (viewMode == CatalogViewMode.LIST) MLBlue else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { viewMode = CatalogViewMode.GRID },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.GridView,
                                    contentDescription = "Cuadrícula",
                                    tint = if (viewMode == CatalogViewMode.GRID) MLBlue else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = { viewMode = CatalogViewMode.STORE },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = "Modo Tienda",
                                    tint = if (viewMode == CatalogViewMode.STORE) MLBlue else Color.Gray
                                )
                            }
                        }
                    }

                    // Secondary Tag & Status Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusOptions = listOf("Todos", "Disponible", "Agotado", "Próximamente")
                        statusOptions.forEach { st ->
                            AssistChip(
                                onClick = { selectedStatus = st },
                                label = { Text(st, fontSize = 11.sp) },
                                leadingIcon = {
                                    if (selectedStatus == st) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        val tagOptions = listOf("Todos", "Nuevo", "Oferta", "Más vendido", "Recomendado", "Exclusivo")
                        tagOptions.forEach { tg ->
                            AssistChip(
                                onClick = { selectedTag = tg },
                                label = { Text("🏷️ $tg", fontSize = 11.sp) },
                                leadingIcon = {
                                    if (selectedTag == tg) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }

            // PRODUCT DISPLAY CONTENT
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
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
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No se encontraron productos", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Toca el botón '+' abajo para registrar tu primer producto o ajusta los filtros de búsqueda.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                when (viewMode) {
                    CatalogViewMode.LIST -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { prod ->
                                val isSelected = prod.id in selectedProductIds
                                CatalogProductListItem(
                                    product = prod,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = {
                                        selectedProductIds = if (isSelected) selectedProductIds - prod.id else selectedProductIds + prod.id
                                    },
                                    onEdit = {
                                        productToEdit = prod
                                        showAddDialog = true
                                    },
                                    onDelete = { viewModel.deleteCatalogProduct(prod.id) },
                                    onView = { productToView = prod },
                                    onCreateAd = {
                                        adCreatorProduct = prod
                                        adCreatorProducts = emptyList()
                                        showAdCreator = true
                                    }
                                )
                            }
                        }
                    }
                    CatalogViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { prod ->
                                val isSelected = prod.id in selectedProductIds
                                CatalogProductGridItem(
                                    product = prod,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = {
                                        selectedProductIds = if (isSelected) selectedProductIds - prod.id else selectedProductIds + prod.id
                                    },
                                    onEdit = {
                                        productToEdit = prod
                                        showAddDialog = true
                                    },
                                    onDelete = { viewModel.deleteCatalogProduct(prod.id) },
                                    onView = { productToView = prod },
                                    onCreateAd = {
                                        adCreatorProduct = prod
                                        adCreatorProducts = emptyList()
                                        showAdCreator = true
                                    }
                                )
                            }
                        }
                    }
                    CatalogViewMode.STORE -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { prod ->
                                val isSelected = prod.id in selectedProductIds
                                CatalogProductStoreItem(
                                    product = prod,
                                    profile = companyProfile,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = {
                                        selectedProductIds = if (isSelected) selectedProductIds - prod.id else selectedProductIds + prod.id
                                    },
                                    onEdit = {
                                        productToEdit = prod
                                        showAddDialog = true
                                    },
                                    onDelete = { viewModel.deleteCatalogProduct(prod.id) },
                                    onCreateAd = {
                                        adCreatorProduct = prod
                                        adCreatorProducts = emptyList()
                                        showAdCreator = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOGS
    if (showPdfExportDialog) {
        CatalogPdfExportDialog(
            products = catalogProducts,
            profile = companyProfile,
            onDismiss = { showPdfExportDialog = false },
            onExport = { style ->
                companyProfile?.let { prof ->
                    CatalogPdfExporter.exportAndShareCatalog(
                        context = context,
                        profile = prof,
                        products = catalogProducts,
                        style = style
                    )
                } ?: run {
                    Toast.makeText(context, "Configura los datos de tu empresa antes de exportar", Toast.LENGTH_LONG).show()
                    onNavigateToCompany()
                }
            }
        )
    }

    if (showAddDialog) {
        AddEditCatalogProductDialog(
            productToEdit = productToEdit,
            categories = categories.map { it.name },
            onDismiss = { showAddDialog = false },
            onSave = { product ->
                viewModel.saveCatalogProduct(product)
                showAddDialog = false
                Toast.makeText(context, "Producto guardado con éxito", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { name ->
                viewModel.addCategory(name)
                showAddCategoryDialog = false
                Toast.makeText(context, "Categoría agregada", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (productToView != null) {
        ProductDetailDialog(
            product = productToView!!,
            profile = companyProfile,
            onDismiss = { productToView = null },
            onCreateAd = { prod ->
                productToView = null
                adCreatorProduct = prod
                adCreatorProducts = emptyList()
                showAdCreator = true
            }
        )
    }

    if (showAdCreator) {
        AdCreatorScreen(
            viewModel = viewModel,
            initialProduct = adCreatorProduct,
            initialProducts = adCreatorProducts,
            onDismiss = {
                showAdCreator = false
                adCreatorProduct = null
                adCreatorProducts = emptyList()
            }
        )
    }
}

@Composable
fun CatalogProductListItem(
    product: CatalogProduct,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onCreateAd: () -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val coverUri = remember(product) { product.getCoverPhotoUri() }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) MLBlue else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onToggleSelect() else onView()
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MLBlue)
                )
            }

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (coverUri.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Text("📦", fontSize = 28.sp)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                }

                Text(
                    text = "SKU: ${product.sku.ifBlank { "N/A" }} • ${product.category}",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = numberFormat.format(product.sellingPrice) + " COP",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.5.sp,
                        color = MLBlue
                    )

                    if (product.previousPrice > product.sellingPrice) {
                        Text(
                            text = numberFormat.format(product.previousPrice),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp).testTag("product_menu_btn_${product.id}")
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Ver Detalle") },
                        onClick = {
                            showMenu = false
                            onView()
                        },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF0284C7)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Crear Publicidad") },
                        onClick = {
                            showMenu = false
                            onCreateAd()
                        },
                        leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFDC2626)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                    )
                }
            }
        }
    }
}

@Composable
fun CatalogProductGridItem(
    product: CatalogProduct,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit,
    onCreateAd: () -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val coverUri = remember(product) { product.getCoverPhotoUri() }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isSelected) MLBlue else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) onToggleSelect() else onView()
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUri.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("📦", fontSize = 42.sp)
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(checkedColor = MLBlue)
                        )
                    }
                } else if (product.tags.isNotBlank()) {
                    Surface(
                        color = MLBlue,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = product.tags.split(",").first(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                maxLines = 2
            )

            Text(
                text = product.category,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = numberFormat.format(product.sellingPrice) + " COP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = MLBlue
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp).testTag("product_grid_menu_btn_${product.id}")
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ver Detalle") },
                            onClick = {
                                showMenu = false
                                onView()
                            },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF0284C7)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Crear Publicidad") },
                            onClick = {
                                showMenu = false
                                onCreateAd()
                            },
                            leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFDC2626)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogProductStoreItem(
    product: CatalogProduct,
    profile: CompanyProfile?,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCreateAd: () -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val coverUri = remember(product) { product.getCoverPhotoUri() }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) MLBlue else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (coverUri.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("📷 Imagen de Producto", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(checkedColor = MLBlue)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .background(Color.White, CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.DarkGray, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Crear Publicidad") },
                            onClick = {
                                showMenu = false
                                onCreateAd()
                            },
                            leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFDC2626)) }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )

            if (product.shortDescription.isNotBlank()) {
                Text(
                    text = product.shortDescription,
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PRECIO DE VENTA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(
                        text = numberFormat.format(product.sellingPrice) + " COP",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color(0xFF047857)
                    )
                }

                Surface(
                    color = Color(0xFF22C55E),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Pedir por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPhotoManager(
    photosList: List<String>,
    coverPhotoUri: String,
    onPhotosChanged: (newList: List<String>, newCover: String) -> Unit
) {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val copiedUris = uris.map { uri ->
                PhotoStorageHelper.copyUriToInternalStorage(context, uri)
            }
            val updatedList = photosList + copiedUris
            val updatedCover = if (coverPhotoUri.isBlank()) copiedUris.first() else coverPhotoUri
            onPhotosChanged(updatedList, updatedCover)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            val persistentUri = PhotoStorageHelper.copyUriToInternalStorage(context, tempCameraUri!!)
            val updatedList = photosList + persistentUri
            val updatedCover = if (coverPhotoUri.isBlank()) persistentUri else coverPhotoUri
            onPhotosChanged(updatedList, updatedCover)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val pair = PhotoStorageHelper.createCameraImageUri(context)
            if (pair != null) {
                tempCameraUri = pair.first
                cameraLauncher.launch(pair.first)
            }
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para tomar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MLBlue, modifier = Modifier.size(20.dp))
                    Text("Fotografías del Producto", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                }
                Surface(
                    color = MLBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${photosList.size} foto(s)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MLBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (coverPhotoUri.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverPhotoUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Portada Principal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        color = Color(0xFFEAB308),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Text("PORTADA PRINCIPAL", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sin fotos registradas", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text("Añade imágenes desde la galería o la cámara", fontSize = 10.5.sp, color = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val permission = Manifest.permission.CAMERA
                        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            val pair = PhotoStorageHelper.createCameraImageUri(context)
                            if (pair != null) {
                                tempCameraUri = pair.first
                                cameraLauncher.launch(pair.first)
                            }
                        } else {
                            cameraPermissionLauncher.launch(permission)
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("take_photo_btn"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cámara", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f).testTag("open_gallery_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MLBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Collections, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galería", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (photosList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Administrar Fotografías y Orden:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photosList.forEachIndexed { index, uri ->
                        val isCover = (uri == coverPhotoUri)

                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isCover) Color(0xFFFEF9C3) else Color.White),
                            border = BorderStroke(if (isCover) 2.dp else 1.dp, if (isCover) Color(0xFFEAB308) else Color(0xFFCBD5E1)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.width(100.dp)
                        ) {
                            Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onPhotosChanged(photosList, uri)
                                        }
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Foto ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (isCover) {
                                        Surface(
                                            color = Color(0xFFEAB308),
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp).size(14.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val mutable = photosList.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                onPhotosChanged(mutable, coverPhotoUri)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowBackIos, contentDescription = "Mover izquierda", modifier = Modifier.size(12.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val mutable = photosList.toMutableList()
                                            mutable.removeAt(index)
                                            var newCover = coverPhotoUri
                                            if (isCover) {
                                                newCover = mutable.firstOrNull() ?: ""
                                            }
                                            onPhotosChanged(mutable, newCover)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < photosList.size - 1) {
                                                val mutable = photosList.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                onPhotosChanged(mutable, coverPhotoUri)
                                            }
                                        },
                                        enabled = index < photosList.size - 1,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Mover derecha", modifier = Modifier.size(12.dp))
                                    }
                                }

                                if (!isCover) {
                                    TextButton(
                                        onClick = { onPhotosChanged(photosList, uri) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Portada", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MLBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// DIALOG TO ADD OR EDIT PRODUCT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCatalogProductDialog(
    productToEdit: CatalogProduct?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (CatalogProduct) -> Unit
) {
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var sku by remember { mutableStateOf(productToEdit?.sku ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: (categories.firstOrNull() ?: "Tecnología")) }
    var brand by remember { mutableStateOf(productToEdit?.brand ?: "") }
    var shortDesc by remember { mutableStateOf(productToEdit?.shortDescription ?: "") }
    var fullDesc by remember { mutableStateOf(productToEdit?.fullDescription ?: "") }
    var sellingPriceStr by remember { mutableStateOf(productToEdit?.sellingPrice?.toInt()?.toString() ?: "") }
    var previousPriceStr by remember { mutableStateOf(productToEdit?.previousPrice?.toInt()?.toString() ?: "") }
    var status by remember { mutableStateOf(productToEdit?.status ?: "Disponible") }
    var tags by remember { mutableStateOf(productToEdit?.tags ?: "") }
    var colors by remember { mutableStateOf(productToEdit?.colors ?: "") }
    var variants by remember { mutableStateOf(productToEdit?.variants ?: "") }
    var stockStr by remember { mutableStateOf(productToEdit?.stock?.toString() ?: "10") }
    var supplier by remember { mutableStateOf(productToEdit?.supplier ?: "") }
    var internalNotes by remember { mutableStateOf(productToEdit?.internalNotes ?: "") }

    var photosList by remember { mutableStateOf(productToEdit?.getPhotosList() ?: emptyList()) }
    var coverPhotoUri by remember { mutableStateOf(productToEdit?.getCoverPhotoUri() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (productToEdit == null) "Nuevo Producto de Catálogo" else "Editar Producto", fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    ProductPhotoManager(
                        photosList = photosList,
                        coverPhotoUri = coverPhotoUri,
                        onPhotosChanged = { newList, newCover ->
                            photosList = newList
                            coverPhotoUri = newCover
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Producto *") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_prod_name")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("Código / SKU") },
                            modifier = Modifier.weight(1f).testTag("dialog_prod_sku")
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Categoría") },
                            modifier = Modifier.weight(1f).testTag("dialog_prod_category")
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it },
                            label = { Text("Precio Venta (COP) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("dialog_prod_price")
                        )
                        OutlinedTextField(
                            value = previousPriceStr,
                            onValueChange = { previousPriceStr = it },
                            label = { Text("Precio Anterior (Opcional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = shortDesc,
                        onValueChange = { shortDesc = it },
                        label = { Text("Descripción Corta") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = fullDesc,
                        onValueChange = { fullDesc = it },
                        label = { Text("Descripción Completa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Etiquetas (ej. Nuevo, Oferta, Recomendado)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = colors,
                            onValueChange = { colors = it },
                            label = { Text("Colores") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = variants,
                            onValueChange = { variants = it },
                            label = { Text("Tallas / Versiones") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = supplier,
                            onValueChange = { supplier = it },
                            label = { Text("Proveedor") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = internalNotes,
                        onValueChange = { internalNotes = it },
                        label = { Text("Notas Internas (Solo para ti)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || sellingPriceStr.toDoubleOrNull() == null) return@Button

                    val product = CatalogProduct(
                        id = productToEdit?.id ?: 0,
                        name = name.trim(),
                        sku = sku.trim(),
                        category = category.trim(),
                        brand = brand.trim(),
                        shortDescription = shortDesc.trim(),
                        fullDescription = fullDesc.trim(),
                        sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0,
                        previousPrice = previousPriceStr.toDoubleOrNull() ?: 0.0,
                        status = status,
                        tags = tags.trim(),
                        colors = colors.trim(),
                        variants = variants.trim(),
                        stock = stockStr.toIntOrNull() ?: 10,
                        supplier = supplier.trim(),
                        internalNotes = internalNotes.trim(),
                        photoUri = coverPhotoUri,
                        photosJson = photosList.joinToString("|")
                    )
                    onSave(product)
                },
                modifier = Modifier.testTag("dialog_save_btn")
            ) {
                Text("Guardar Producto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Categoría Personalizada", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Ingresa el nombre de la nueva categoría para organizar tu catálogo:", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Nombre de categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (categoryName.isNotBlank()) onAdd(categoryName) },
                colors = ButtonDefaults.buttonColors(containerColor = MLBlue)
            ) {
                Text("Crear Categoría")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ProductDetailDialog(
    product: CatalogProduct,
    profile: CompanyProfile?,
    onDismiss: () -> Unit,
    onCreateAd: (CatalogProduct) -> Unit = {}
) {
    val numberFormat = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply { maximumFractionDigits = 0 } }
    val photosList = remember(product) { product.getPhotosList() }
    var selectedPhotoUri by remember(product) { mutableStateOf(product.getCoverPhotoUri()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main Photo Display
                if (selectedPhotoUri.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedPhotoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Photo Selector Gallery
                if (photosList.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        photosList.forEach { uri ->
                            val isSelected = (uri == selectedPhotoUri)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MLBlue else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                    .clickable { selectedPhotoUri = uri }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Text("SKU: ${product.sku.ifBlank { "N/A" }} | Categoría: ${product.category}", fontSize = 12.sp, color = Color.Gray)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = numberFormat.format(product.sellingPrice) + " COP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF047857)
                    )
                    if (product.previousPrice > product.sellingPrice) {
                        Text(
                            text = numberFormat.format(product.previousPrice),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Descripción:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    text = product.fullDescription.ifBlank { product.shortDescription.ifBlank { "Sin descripción detallada." } },
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )

                if (product.colors.isNotBlank()) {
                    Text("🎨 Colores: ${product.colors}", fontSize = 12.5.sp)
                }
                if (product.variants.isNotBlank()) {
                    Text("📏 Tallas / Versiones: ${product.variants}", fontSize = 12.5.sp)
                }
                if (product.tags.isNotBlank()) {
                    Text("🏷️ Etiquetas: ${product.tags}", fontSize = 12.5.sp)
                }

                if (product.internalNotes.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("🔒 Notas Internas Propietario:", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF92400E))
                            Text(product.internalNotes, fontSize = 12.sp, color = Color(0xFF78350F))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onCreateAd(product) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Crear Publicidad")
                }
                OutlinedButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

// ==========================================
// COMPANY PROFILE & PDF EXPORT SCREEN
// ==========================================
@Composable
fun CompanyProfileScreen(viewModel: DropshipViewModel) {
    val context = LocalContext.current
    val currentProfile by viewModel.companyProfile.collectAsStateWithLifecycle()
    val catalogProducts by viewModel.catalogProducts.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var slogan by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var instagram by remember { mutableStateOf("") }
    var tiktok by remember { mutableStateOf("") }

    var selectedStyle by remember { mutableStateOf(CatalogPdfExporter.TemplateStyle.MODERNO) }
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

    LaunchedEffect(currentProfile) {
        currentProfile?.let {
            name = it.name
            slogan = it.slogan
            description = it.description
            whatsapp = it.whatsapp
            phone = it.phone
            email = it.email
            address = it.address
            city = it.city
            website = it.website
            facebook = it.facebook
            instagram = it.instagram
            tiktok = it.tiktok
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Business, contentDescription = null, tint = Color.White)
                    }
                }
                Column {
                    Text("Mi Empresa & Exportación PDF", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF0F172A))
                    Text("Configura los datos comerciales que aparecerán en tu catálogo PDF.", fontSize = 12.5.sp, color = Color(0xFF64748B))
                }
            }
        }

        // DISEÑO DEL CATÁLOGO & EXPORTACIÓN PDF
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                        Text("🎨 Diseño del Catálogo PDF", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Elige el estilo visual de revista comercial para la exportación. Los productos se organizarán por categorías con portada, índice automático, páginas de presentación y código QR de WhatsApp.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Estilo Visual de Revista:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCBD5E1))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        CatalogPdfExporter.TemplateStyle.values().forEach { style ->
                            val isSelected = selectedStyle == style
                            Surface(
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStyle = style }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedStyle = style },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF38BDF8),
                                            unselectedColor = Color(0xFF64748B)
                                        )
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(Color(android.graphics.Color.parseColor(style.primaryColorHex)), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(Color(android.graphics.Color.parseColor(style.accentColorHex)), CircleShape)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = style.description,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val activeProfile = CompanyProfile(
                                id = 1,
                                name = name.ifBlank { "Mi Tienda Express" },
                                slogan = slogan,
                                description = description,
                                whatsapp = whatsapp,
                                phone = phone,
                                email = email,
                                address = address,
                                city = city,
                                website = website,
                                facebook = facebook,
                                instagram = instagram,
                                tiktok = tiktok
                            )
                            viewModel.updateCompanyProfile(activeProfile)

                            if (catalogProducts.isEmpty()) {
                                Toast.makeText(context, "Agrega al menos 1 producto al catálogo para exportar", Toast.LENGTH_LONG).show()
                            } else {
                                CatalogPdfExporter.exportAndShareCatalog(
                                    context = context,
                                    profile = activeProfile,
                                    products = catalogProducts,
                                    style = selectedStyle
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("export_pdf_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚀 Generar y Compartir Catálogo PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // FORM FIELDS FOR COMPANY PROFILE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🏢 Datos de la Empresa", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Negocio / Marca *") },
                        modifier = Modifier.fillMaxWidth().testTag("company_name_input")
                    )

                    OutlinedTextField(
                        value = slogan,
                        onValueChange = { slogan = it },
                        label = { Text("Eslogan Comercial") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción de la Empresa") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = whatsapp,
                            onValueChange = { whatsapp = it },
                            label = { Text("WhatsApp Pedidos *") },
                            modifier = Modifier.weight(1f).testTag("company_whatsapp_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Teléfono Fijo") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo Electrónico") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Sitio Web") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Dirección") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Ciudad, País") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("📲 Redes Sociales", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(top = 8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = instagram,
                            onValueChange = { instagram = it },
                            label = { Text("Instagram") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = facebook,
                            onValueChange = { facebook = it },
                            label = { Text("Facebook") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val updated = CompanyProfile(
                                id = 1,
                                name = name,
                                slogan = slogan,
                                description = description,
                                whatsapp = whatsapp,
                                phone = phone,
                                email = email,
                                address = address,
                                city = city,
                                website = website,
                                facebook = facebook,
                                instagram = instagram,
                                tiktok = tiktok
                            )
                            viewModel.updateCompanyProfile(updated)
                            Toast.makeText(context, "Información de empresa guardada", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MLBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag("save_company_btn")
                    ) {
                        Text("Guardar Cambios de Empresa")
                    }
                }
            }
        }



        // BACKUP & RESTORE SECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(24.dp))
                        Text("💾 Respaldo y Restauración de Datos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0369A1))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Genera un archivo ZIP seguro con todo tu catálogo, fotografías de alta resolución, información comercial, historial y configuraciones para migrar a cualquier dispositivo.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF0284C7),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showBackupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_backup_dialog_btn")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gestionar Copia de Seguridad (.ZIP)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogPdfExportDialog(
    products: List<CatalogProduct>,
    profile: CompanyProfile?,
    onDismiss: () -> Unit,
    onExport: (CatalogPdfExporter.TemplateStyle) -> Unit
) {
    var selectedStyle by remember { mutableStateOf(CatalogPdfExporter.TemplateStyle.COLORIDO) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(26.dp))
                        Text(
                            text = "Diseño del Catálogo PDF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Selecciona el estilo visual de revista comercial para la exportación. Los productos se organizarán por categorías con portada, índice automático, páginas de presentación y código QR de WhatsApp:",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                CatalogPdfExporter.TemplateStyle.values().forEach { style ->
                    val isSelected = selectedStyle == style
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MLBlue else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedStyle = style }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedStyle = style },
                                colors = RadioButtonDefaults.colors(selectedColor = MLBlue)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color(android.graphics.Color.parseColor(style.primaryColorHex)), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(Color(android.graphics.Color.parseColor(style.accentColorHex)), CircleShape)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = style.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = style.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onExport(selectedStyle)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_pdf_confirm_btn")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generar Catálogo PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
