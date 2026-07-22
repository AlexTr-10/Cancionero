package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.CatalogProduct
import com.example.data.model.CompanyProfile
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object CatalogPdfExporter {

    enum class TemplateStyle(
        val id: String,
        val displayName: String,
        val description: String,
        val primaryColorHex: String,
        val accentColorHex: String,
        val backgroundColorHex: String,
        val textColorHex: String,
        val cardBgHex: String,
        val badgeBgHex: String
    ) {
        CLASICO(
            "clasico",
            "Clásico",
            "Estilo tradicional refinado con tonos borgoña, azul marino y tipografía sobria",
            "#1E3A8A", "#991B1B", "#F8FAFC", "#0F172A", "#FFFFFF", "#991B1B"
        ),
        MODERNO(
            "moderno",
            "Moderno",
            "Líneas dinámicas, bordes redondeados y tonos azul eléctrico con cian",
            "#2563EB", "#06B6D4", "#F0F9FF", "#0F172A", "#FFFFFF", "#2563EB"
        ),
        MINIMALISTA(
            "minimalista",
            "Minimalista",
            "Elegancia sutil, espacios limpios, tipografía fina y tonos gris pizarra",
            "#334155", "#64748B", "#FFFFFF", "#1E293B", "#F8FAFC", "#475569"
        ),
        ELEGANTE(
            "elegante",
            "Elegante",
            "Estilo alta gama / joyería con fondo carbón, dorado de lujo y marcos finos",
            "#0F172A", "#D97706", "#1E293B", "#F8FAFC", "#334155", "#D97706"
        ),
        EMPRESARIAL(
            "empresarial",
            "Empresarial",
            "Estructura ejecutiva corporativa azul naval y verde esmeralda",
            "#0F2942", "#059669", "#F8FAFC", "#0F172A", "#FFFFFF", "#059669"
        ),
        OSCURO(
            "oscuro",
            "Oscuro",
            "Estilo revista nocturna / editorial con fondo oscuro y acentos neón",
            "#090D16", "#38BDF8", "#0F172A", "#F8FAFC", "#1E293B", "#0284C7"
        ),
        COLORIDO(
            "colorido",
            "Colorido",
            "Vibrante estilo catálogo de moda (Avon / Ésika) con fucsia y magenta",
            "#E11D48", "#7C3AED", "#FFF1F2", "#0F172A", "#FFFFFF", "#E11D48"
        )
    }

    fun exportAndShareCatalog(
        context: Context,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        style: TemplateStyle = TemplateStyle.COLORIDO
    ) {
        if (products.isEmpty()) {
            Toast.makeText(context, "El catálogo no contiene productos para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            val primaryColor = safeParseColor(profile.primaryColorHex.ifBlank { style.primaryColorHex }, Color.parseColor("#1E3A8A"))
            val accentColor = safeParseColor(style.accentColorHex, Color.parseColor("#E11D48"))
            val pageBgColor = safeParseColor(style.backgroundColorHex, Color.parseColor("#F8FAFC"))
            val isDarkTheme = style == TemplateStyle.OSCURO || style == TemplateStyle.ELEGANTE
            val textColor = safeParseColor(if (isDarkTheme) "#F8FAFC" else style.textColorHex, Color.parseColor("#0F172A"))
            val cardBgColor = safeParseColor(if (isDarkTheme) "#1E293B" else style.cardBgHex, Color.WHITE)

            val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
                maximumFractionDigits = 0
            }

            val dateFormat = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "CO"))
            val currentDateStr = dateFormat.format(Date())

            // 1. GROUP PRODUCTS STRICTLY BY CATEGORY
            val categoriesGrouped: Map<String, List<CatalogProduct>> = products
                .groupBy { it.category.ifBlank { "General" }.trim() }
                .toSortedMap()

            val productsPerPage = 2 // 2 products per page for spacious, high-resolution magazine look

            // 2. PRE-CALCULATE EXACT STARTING PAGE NUMBERS FOR TABLE OF CONTENTS
            var runningPage = 1
            val coverPageNumber = runningPage
            runningPage++ // Page 1: Cover

            val tocPageNumber = runningPage
            runningPage++ // Page 2: Table of Contents

            val categoryStartPages = mutableMapOf<String, Int>()
            categoriesGrouped.forEach { (catName, catProds) ->
                categoryStartPages[catName] = runningPage
                runningPage++ // 1 page for Category Presentation / Divider
                val numProductPages = Math.max(1, Math.ceil(catProds.size.toDouble() / productsPerPage).toInt())
                runningPage += numProductPages
            }

            val contactPageNumber = runningPage // Final Back Cover / Contact page
            val totalPages = runningPage

            // =========================================================
            // PAGE 1: COVER PAGE (PORTADA REVISTA)
            // =========================================================
            val coverPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, coverPageNumber).create()
            val coverPage = pdfDocument.startPage(coverPageInfo)
            val coverCanvas = coverPage.canvas

            drawCoverPage(
                canvas = coverCanvas,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                profile = profile,
                style = style,
                primaryColor = primaryColor,
                accentColor = accentColor,
                pageBgColor = pageBgColor,
                isDarkTheme = isDarkTheme,
                totalProducts = products.size,
                totalCategories = categoriesGrouped.size,
                currentDateStr = currentDateStr,
                pageNumber = coverPageNumber,
                totalPages = totalPages
            )
            pdfDocument.finishPage(coverPage)

            // =========================================================
            // PAGE 2: TABLE OF CONTENTS (ÍNDICE DE CATEGORÍAS)
            // =========================================================
            val tocPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, tocPageNumber).create()
            val tocPage = pdfDocument.startPage(tocPageInfo)
            val tocCanvas = tocPage.canvas

            drawTableOfContentsPage(
                canvas = tocCanvas,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                profile = profile,
                categoriesGrouped = categoriesGrouped,
                categoryStartPages = categoryStartPages,
                primaryColor = primaryColor,
                accentColor = accentColor,
                pageBgColor = pageBgColor,
                isDarkTheme = isDarkTheme,
                pageNumber = tocPageNumber,
                totalPages = totalPages
            )
            pdfDocument.finishPage(tocPage)

            // =========================================================
            // CATEGORY SECTIONS (PRESENTATION PAGE + PRODUCT PAGES)
            // =========================================================
            categoriesGrouped.forEach { (categoryName, categoryProducts) ->
                val startPageForCat = categoryStartPages[categoryName] ?: 3
                var currentCatPage = startPageForCat

                // A. CATEGORY PRESENTATION / DIVIDER PAGE
                val catPresPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentCatPage).create()
                val catPresPage = pdfDocument.startPage(catPresPageInfo)
                val catPresCanvas = catPresPage.canvas

                drawCategoryPresentationPage(
                    canvas = catPresCanvas,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    categoryName = categoryName,
                    catProductCount = categoryProducts.size,
                    profile = profile,
                    style = style,
                    primaryColor = primaryColor,
                    accentColor = accentColor,
                    pageBgColor = pageBgColor,
                    isDarkTheme = isDarkTheme,
                    pageNumber = currentCatPage,
                    totalPages = totalPages
                )
                pdfDocument.finishPage(catPresPage)
                currentCatPage++

                // B. PRODUCT PAGES FOR THIS CATEGORY
                val chunks = categoryProducts.chunked(productsPerPage)
                chunks.forEach { productChunk ->
                    val prodPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentCatPage).create()
                    val prodPage = pdfDocument.startPage(prodPageInfo)
                    val prodCanvas = prodPage.canvas

                    drawProductPage(
                        context = context,
                        canvas = prodCanvas,
                        pageWidth = pageWidth,
                        pageHeight = pageHeight,
                        categoryName = categoryName,
                        products = productChunk,
                        profile = profile,
                        style = style,
                        primaryColor = primaryColor,
                        accentColor = accentColor,
                        pageBgColor = pageBgColor,
                        cardBgColor = cardBgColor,
                        textColor = textColor,
                        isDarkTheme = isDarkTheme,
                        numberFormat = numberFormat,
                        pageNumber = currentCatPage,
                        totalPages = totalPages
                    )
                    pdfDocument.finishPage(prodPage)
                    currentCatPage++
                }
            }

            // =========================================================
            // FINAL PAGE: CONTACT & WHATSAPP QR CODE (CONTRAPORTADA)
            // =========================================================
            val contactPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, contactPageNumber).create()
            val contactPage = pdfDocument.startPage(contactPageInfo)
            val contactCanvas = contactPage.canvas

            drawContactBackCoverPage(
                canvas = contactCanvas,
                pageWidth = pageWidth,
                pageHeight = pageHeight,
                profile = profile,
                primaryColor = primaryColor,
                accentColor = accentColor,
                pageBgColor = pageBgColor,
                isDarkTheme = isDarkTheme,
                pageNumber = contactPageNumber,
                totalPages = totalPages
            )
            pdfDocument.finishPage(contactPage)

            // SAVE FILE AND SHARE
            val pdfFile = File(context.cacheDir, "catalogo_${profile.name.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Catálogo Comercial - ${profile.name}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Hola, te comparto el catálogo comercial de ${profile.name}. ¡Revisa nuestras colecciones y realiza tu pedido directo por WhatsApp al ${profile.whatsapp}!"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Compartir Catálogo PDF"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar el catálogo PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // -----------------------------------------------------------------
    // COVER PAGE RENDERER
    // -----------------------------------------------------------------
    private fun drawCoverPage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        profile: CompanyProfile,
        style: TemplateStyle,
        primaryColor: Int,
        accentColor: Int,
        pageBgColor: Int,
        isDarkTheme: Boolean,
        totalProducts: Int,
        totalCategories: Int,
        currentDateStr: String,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint().apply { color = pageBgColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Top Header Banner
        val headerHeight = 220f
        val bannerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), headerHeight, bannerPaint)

        // Header Accent Stripe
        val stripePaint = Paint().apply { color = accentColor }
        canvas.drawRect(0f, headerHeight - 12f, pageWidth.toFloat(), headerHeight, stripePaint)

        // Magazine Top Tag
        val tagPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("EDICIÓN EXCLUSIVA • CATÁLOGO COMERCIAL ${style.displayName.uppercase()}", pageWidth / 2f, 40f, tagPaint)

        // Company Name
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.name.uppercase(), pageWidth / 2f, 100f, titlePaint)

        // Slogan
        val sloganPaint = Paint().apply {
            color = safeParseColor("#E2E8F0", Color.LTGRAY)
            textSize = 13f
            textAlign = Paint.Align.CENTER
        }
        val sloganText = profile.slogan.ifBlank { "La mejor calidad y selección a tu alcance" }
        canvas.drawText(sloganText, pageWidth / 2f, 135f, sloganPaint)

        // Center Hero Card
        val cardRect = RectF(40f, 260f, (pageWidth - 40).toFloat(), 560f)
        val cardBgPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#1E293B") else Color.WHITE
        }
        val cardBorderPaint = Paint().apply {
            color = accentColor
            this.style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawRoundRect(cardRect, 20f, 20f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 20f, 20f, cardBorderPaint)

        val catalogTitlePaint = Paint().apply {
            color = accentColor
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CATÁLOGO DE PRODUCTOS", pageWidth / 2f, 320f, catalogTitlePaint)

        val catSubPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        val descText = profile.description.ifBlank { "Explora nuestra oferta exclusiva con precios especiales y atención personalizada." }
        canvas.drawText(descText.take(100), pageWidth / 2f, 355f, catSubPaint)

        // Badge Pill inside Cover
        val pillRect = RectF(pageWidth / 2f - 140f, 390f, pageWidth / 2f + 140f, 440f)
        val pillBgPaint = Paint().apply { color = primaryColor }
        canvas.drawRoundRect(pillRect, 25f, 25f, pillBgPaint)

        val pillTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔥 $totalProducts PRODUCTOS EN $totalCategories SECCIONES", pageWidth / 2f, 420f, pillTextPaint)

        val datePaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Fecha de actualización: $currentDateStr", pageWidth / 2f, 480f, datePaint)

        // WhatsApp Direct Order Box
        val waCardRect = RectF(50f, 590f, (pageWidth - 50).toFloat(), 670f)
        val waBgPaint = Paint().apply { color = Color.parseColor("#16A34A") }
        canvas.drawRoundRect(waCardRect, 14f, 14f, waBgPaint)

        val waTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("💬 PEDIDOS DIRECTOS VÍA WHATSAPP", pageWidth / 2f, 625f, waTitlePaint)

        val waNumPaint = Paint().apply {
            color = Color.WHITE
            textSize = 13f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("WhatsApp: ${profile.whatsapp} | ${profile.email}", pageWidth / 2f, 650f, waNumPaint)

        drawFooter(canvas, profile, pageWidth, pageHeight, pageNumber, totalPages, primaryColor, isDarkTheme)
    }

    // -----------------------------------------------------------------
    // TABLE OF CONTENTS PAGE RENDERER (ÍNDICE DE CATEGORÍAS)
    // -----------------------------------------------------------------
    private fun drawTableOfContentsPage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        profile: CompanyProfile,
        categoriesGrouped: Map<String, List<CatalogProduct>>,
        categoryStartPages: Map<String, Int>,
        primaryColor: Int,
        accentColor: Int,
        pageBgColor: Int,
        isDarkTheme: Boolean,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint().apply { color = pageBgColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Header Title
        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText("ÍNDICE DE CATEGORÍAS Y SECCIONES", 40f, 65f, titlePaint)

        val subPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textSize = 11.5f
        }
        canvas.drawText("Explora nuestro catálogo organizado por departamento. Haz tu pedido indicando la categoría:", 40f, 90f, subPaint)

        val linePaint = Paint().apply {
            color = accentColor
            strokeWidth = 2f
        }
        canvas.drawLine(40f, 105f, (pageWidth - 40).toFloat(), 105f, linePaint)

        var idxY = 140f
        val dotPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#475569") else Color.parseColor("#CBD5E1")
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }

        categoriesGrouped.forEach { (catName, catProducts) ->
            val startPage = categoryStartPages[catName] ?: 3

            val catBarPaint = Paint().apply {
                color = if (isDarkTheme) Color.parseColor("#1E293B") else Color.parseColor("#F1F5F9")
            }
            canvas.drawRoundRect(RectF(40f, idxY - 18f, (pageWidth - 40).toFloat(), idxY + 22f), 8f, 8f, catBarPaint)

            val catTextPaint = Paint().apply {
                color = if (isDarkTheme) Color.WHITE else Color.parseColor("#0F172A")
                textSize = 13f
                isFakeBoldText = true
            }
            val catLabel = "📂 $catName (${catProducts.size} productos)"
            canvas.drawText(catLabel, 55f, idxY + 4f, catTextPaint)

            val textWidth = catTextPaint.measureText(catLabel)
            val pageBadgeStr = "Pág. $startPage"

            val pageNumPaint = Paint().apply {
                color = accentColor
                textSize = 13f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(pageBadgeStr, (pageWidth - 55).toFloat(), idxY + 4f, pageNumPaint)

            val pageBadgeWidth = pageNumPaint.measureText(pageBadgeStr)
            canvas.drawLine(55f + textWidth + 15f, idxY, (pageWidth - 55).toFloat() - pageBadgeWidth - 15f, idxY, dotPaint)

            idxY += 48f
            if (idxY > pageHeight - 150f) return@forEach // Prevent overflow
        }

        // Info Tip Box
        val tipRect = RectF(40f, pageHeight - 140f, (pageWidth - 40).toFloat(), pageHeight - 70f)
        val tipBgPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#0F172A") else Color.parseColor("#EFF6FF")
        }
        val tipBorderPaint = Paint().apply {
            color = primaryColor
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(tipRect, 10f, 10f, tipBgPaint)
        canvas.drawRoundRect(tipRect, 10f, 10f, tipBorderPaint)

        val tipTitlePaint = Paint().apply {
            color = primaryColor
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText("💡 ¿Cómo hacer tu pedido?", 55f, pageHeight - 115f, tipTitlePaint)

        val tipDescPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#CBD5E1") else Color.parseColor("#334155")
            textSize = 10.5f
        }
        canvas.drawText("1. Ubica los productos que deseas en sus respectivas páginas.", 55f, pageHeight - 95f, tipDescPaint)
        canvas.drawText("2. Escríbenos por WhatsApp al ${profile.whatsapp} indicando el nombre del producto.", 55f, pageHeight - 80f, tipDescPaint)

        drawFooter(canvas, profile, pageWidth, pageHeight, pageNumber, totalPages, primaryColor, isDarkTheme)
    }

    // -----------------------------------------------------------------
    // CATEGORY PRESENTATION / DIVIDER PAGE RENDERER
    // -----------------------------------------------------------------
    private fun drawCategoryPresentationPage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        categoryName: String,
        catProductCount: Int,
        profile: CompanyProfile,
        style: TemplateStyle,
        primaryColor: Int,
        accentColor: Int,
        pageBgColor: Int,
        isDarkTheme: Boolean,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint().apply { color = pageBgColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Category Banner Box
        val bannerHeight = 320f
        val bannerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 150f, pageWidth.toFloat(), 150f + bannerHeight, bannerPaint)

        val stripePaint = Paint().apply { color = accentColor }
        canvas.drawRect(0f, 150f + bannerHeight - 10f, pageWidth.toFloat(), 150f + bannerHeight, stripePaint)

        val secTagPaint = Paint().apply {
            color = accentColor
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DEPARTAMENTO COMERCIAL EXCLUSIVO", pageWidth / 2f, 210f, secTagPaint)

        val catTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(categoryName.uppercase(), pageWidth / 2f, 270f, catTitlePaint)

        val countPaint = Paint().apply {
            color = safeParseColor("#E2E8F0", Color.LTGRAY)
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Colección especial • $catProductCount productos disponibles", pageWidth / 2f, 310f, countPaint)

        // Highlights Card
        val cardRect = RectF(50f, 520f, (pageWidth - 50).toFloat(), 700f)
        val cardBgPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#1E293B") else Color.WHITE
        }
        val cardBorderPaint = Paint().apply {
            color = accentColor
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(cardRect, 16f, 16f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 16f, 16f, cardBorderPaint)

        val cardTitlePaint = Paint().apply {
            color = primaryColor
            textSize = 14f
            isFakeBoldText = true
        }
        canvas.drawText("✨ Beneficios y Garantía en $categoryName", 75f, 560f, cardTitlePaint)

        val itemTextPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#E2E8F0") else Color.parseColor("#334155")
            textSize = 11.5f
        }
        canvas.drawText("✔ Productos 100% garantizados con la mejor calidad del mercado.", 75f, 595f, itemTextPaint)
        canvas.drawText("✔ Envíos seguros e inmediatos a todo el país.", 75f, 625f, itemTextPaint)
        canvas.drawText("✔ Atención personalizada vía WhatsApp: ${profile.whatsapp}", 75f, 655f, itemTextPaint)

        drawFooter(canvas, profile, pageWidth, pageHeight, pageNumber, totalPages, primaryColor, isDarkTheme)
    }

    // -----------------------------------------------------------------
    // PRODUCT PAGE RENDERER
    // -----------------------------------------------------------------
    private fun drawProductPage(
        context: Context,
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        categoryName: String,
        products: List<CatalogProduct>,
        profile: CompanyProfile,
        style: TemplateStyle,
        primaryColor: Int,
        accentColor: Int,
        pageBgColor: Int,
        cardBgColor: Int,
        textColor: Int,
        isDarkTheme: Boolean,
        numberFormat: NumberFormat,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint().apply { color = pageBgColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Top Category Bar
        val headerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 42f, headerPaint)

        val categoryHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            isFakeBoldText = true
        }
        canvas.drawText("${profile.name.uppercase()}  |  SECCIÓN: ${categoryName.uppercase()}", 30f, 26f, categoryHeaderPaint)

        // Render 2 Products per Page
        var cardY = 58f
        val cardHeight = 350f

        products.forEach { prod ->
            drawProductCard(
                context = context,
                canvas = canvas,
                product = prod,
                topY = cardY,
                cardHeight = cardHeight,
                pageWidth = pageWidth,
                primaryColor = primaryColor,
                accentColor = accentColor,
                cardBgColor = cardBgColor,
                textColor = textColor,
                isDarkTheme = isDarkTheme,
                numberFormat = numberFormat,
                profile = profile
            )
            cardY += cardHeight + 15f
        }

        drawFooter(canvas, profile, pageWidth, pageHeight, pageNumber, totalPages, primaryColor, isDarkTheme)
    }

    // -----------------------------------------------------------------
    // PRODUCT CARD RENDERER (MAGAZINE STYLE)
    // -----------------------------------------------------------------
    private fun drawProductCard(
        context: Context,
        canvas: Canvas,
        product: CatalogProduct,
        topY: Float,
        cardHeight: Float,
        pageWidth: Int,
        primaryColor: Int,
        accentColor: Int,
        cardBgColor: Int,
        textColor: Int,
        isDarkTheme: Boolean,
        numberFormat: NumberFormat,
        profile: CompanyProfile
    ) {
        val cardRect = RectF(30f, topY, (pageWidth - 30).toFloat(), topY + cardHeight)

        val cardBgPaint = Paint().apply { color = cardBgColor }
        val borderPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#334155") else Color.parseColor("#E2E8F0")
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(cardRect, 14f, 14f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 14f, 14f, borderPaint)

        // Image Box Left Column
        val imgWidth = 220f
        val imgHeight = 280f
        val imgRect = RectF(45f, topY + 18f, 45f + imgWidth, topY + 18f + imgHeight)

        val imgBgPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#0F172A") else Color.parseColor("#F1F5F9")
        }
        canvas.drawRoundRect(imgRect, 12f, 12f, imgBgPaint)

        val coverUri = product.getCoverPhotoUri()
        var drawnImage = false

        if (coverUri.isNotBlank()) {
            val bitmap = PhotoStorageHelper.loadBitmapFromUri(context, coverUri, 500, 500)
            if (bitmap != null) {
                canvas.save()
                val path = Path().apply {
                    addRoundRect(imgRect, 12f, 12f, Path.Direction.CW)
                }
                canvas.clipPath(path)

                val srcWidth = bitmap.width.toFloat()
                val srcHeight = bitmap.height.toFloat()
                val dstWidth = imgRect.width()
                val dstHeight = imgRect.height()

                val scale = Math.max(dstWidth / srcWidth, dstHeight / srcHeight)
                val scaledW = srcWidth * scale
                val scaledH = srcHeight * scale
                val left = imgRect.left + (dstWidth - scaledW) / 2f
                val top = imgRect.top + (dstHeight - scaledH) / 2f

                val destRect = RectF(left, top, left + scaledW, top + scaledH)
                val bitmapPaint = Paint().apply { isFilterBitmap = true }
                canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
                canvas.restore()
                bitmap.recycle()
                drawnImage = true
            }
        }

        if (!drawnImage) {
            val iconTextPaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("📷", imgRect.centerX(), imgRect.centerY() + 10f, iconTextPaint)
        }

        // Badges overlay on Image
        var badgeY = topY + 28f
        if (product.previousPrice > product.sellingPrice) {
            val discountPct = Math.round((1.0 - (product.sellingPrice / product.previousPrice)) * 100).toInt()
            if (discountPct > 0) {
                val discRect = RectF(52f, badgeY, 135f, badgeY + 22f)
                val discBgPaint = Paint().apply { color = Color.parseColor("#DC2626") }
                canvas.drawRoundRect(discRect, 6f, 6f, discBgPaint)

                val discTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("🔥 -$discountPct% OFF", discRect.centerX(), badgeY + 15f, discTextPaint)
                badgeY += 28f
            }
        }

        val tagText = product.tags.split(",").firstOrNull { it.isNotBlank() } ?: "Recomendado"
        val tagRect = RectF(52f, badgeY, 135f, badgeY + 22f)
        val tagBgPaint = Paint().apply { color = accentColor }
        canvas.drawRoundRect(tagRect, 6f, 6f, tagBgPaint)

        val tagTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(tagText.trim().uppercase(), tagRect.centerX(), badgeY + 15f, tagTextPaint)

        // Right Column Product Details
        val detailsLeft = 45f + imgWidth + 20f
        val detailsRight = (pageWidth - 45).toFloat()

        // Product Title
        val namePaint = Paint().apply {
            color = textColor
            textSize = 15f
            isFakeBoldText = true
        }
        canvas.drawText(product.name.take(35), detailsLeft, topY + 38f, namePaint)

        // SKU / Category
        val metaPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textSize = 10f
        }
        canvas.drawText("SKU: ${product.sku.ifBlank { "N/A" }} | Categoría: ${product.category}", detailsLeft, topY + 56f, metaPaint)

        // Price Section Box
        val priceBoxY = topY + 75f
        val priceStr = numberFormat.format(product.sellingPrice) + " COP"
        val pricePaint = Paint().apply {
            color = primaryColor
            textSize = 20f
            isFakeBoldText = true
        }
        canvas.drawText(priceStr, detailsLeft, priceBoxY + 22f, pricePaint)

        if (product.previousPrice > product.sellingPrice) {
            val oldPriceStr = "Antes: " + numberFormat.format(product.previousPrice)
            val oldPricePaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 11.5f
                isStrikeThruText = true
            }
            canvas.drawText(oldPriceStr, detailsLeft, priceBoxY + 42f, oldPricePaint)
        }

        // Short Description
        val descY = priceBoxY + 62f
        val descPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#CBD5E1") else Color.parseColor("#334155")
            textSize = 10.5f
        }
        val descLines = wrapText(product.shortDescription.ifBlank { product.fullDescription }, 32)
        var lineY = descY
        descLines.take(3).forEach { l ->
            canvas.drawText(l, detailsLeft, lineY, descPaint)
            lineY += 15f
        }

        // Variants / Colors
        var varY = lineY + 10f
        val attrPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#475569")
            textSize = 10f
        }
        if (product.colors.isNotBlank()) {
            canvas.drawText("🎨 Colores: ${product.colors.take(30)}", detailsLeft, varY, attrPaint)
            varY += 15f
        }
        if (product.variants.isNotBlank()) {
            canvas.drawText("📏 Tallas: ${product.variants.take(30)}", detailsLeft, varY, attrPaint)
            varY += 15f
        }

        // WhatsApp CTA Button
        val ctaRect = RectF(detailsLeft, topY + cardHeight - 48f, detailsRight, topY + cardHeight - 16f)
        val ctaBgPaint = Paint().apply { color = Color.parseColor("#16A34A") }
        canvas.drawRoundRect(ctaRect, 8f, 8f, ctaBgPaint)

        val ctaTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("💬 Pedir por WhatsApp: ${profile.whatsapp}", ctaRect.centerX(), ctaRect.centerY() + 4f, ctaTextPaint)
    }

    // -----------------------------------------------------------------
    // CONTACT & BACK COVER PAGE RENDERER (CONTRAPORTADA + QR CODE)
    // -----------------------------------------------------------------
    private fun drawContactBackCoverPage(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        profile: CompanyProfile,
        primaryColor: Int,
        accentColor: Int,
        pageBgColor: Int,
        isDarkTheme: Boolean,
        pageNumber: Int,
        totalPages: Int
    ) {
        val bgPaint = Paint().apply { color = pageBgColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Header Banner
        val headerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 130f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 22f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ATENCIÓN AL CLIENTE & PEDIDOS", pageWidth / 2f, 65f, titlePaint)

        val subTitlePaint = Paint().apply {
            color = safeParseColor("#E2E8F0", Color.LTGRAY)
            textSize = 12f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("¡Gracias por revisar nuestro catálogo comercial!", pageWidth / 2f, 95f, subTitlePaint)

        // Contact Info Box
        val infoRect = RectF(40f, 160f, (pageWidth - 40).toFloat(), 430f)
        val infoBgPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#1E293B") else Color.WHITE
        }
        val infoBorderPaint = Paint().apply {
            color = accentColor
            this.style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(infoRect, 16f, 16f, infoBgPaint)
        canvas.drawRoundRect(infoRect, 16f, 16f, infoBorderPaint)

        val infoHeaderPaint = Paint().apply {
            color = primaryColor
            textSize = 16f
            isFakeBoldText = true
        }
        canvas.drawText("🏢 ${profile.name.uppercase()}", 65f, 205f, infoHeaderPaint)

        val labelPaint = Paint().apply {
            color = if (isDarkTheme) Color.WHITE else Color.parseColor("#0F172A")
            textSize = 12f
        }

        var startY = 245f
        canvas.drawText("📱 WhatsApp Pedidos: ${profile.whatsapp}", 65f, startY, labelPaint)
        startY += 26f
        canvas.drawText("📞 Teléfono Fijo / Móvil: ${profile.phone.ifBlank { "N/A" }}", 65f, startY, labelPaint)
        startY += 26f
        canvas.drawText("✉️ Correo Electrónico: ${profile.email.ifBlank { "N/A" }}", 65f, startY, labelPaint)
        startY += 26f
        canvas.drawText("📍 Dirección Comercial: ${profile.address}, ${profile.city}", 65f, startY, labelPaint)
        startY += 26f
        canvas.drawText("🌐 Sitio Web / Catálogo: ${profile.website.ifBlank { "N/A" }}", 65f, startY, labelPaint)
        startY += 26f
        canvas.drawText("📲 Redes: Instagram @${profile.instagram} | Facebook ${profile.facebook}", 65f, startY, labelPaint)

        // QR CODE BOX
        val qrBoxRect = RectF(40f, 460f, (pageWidth - 40).toFloat(), 740f)
        val qrBoxBgPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#1E293B") else Color.WHITE
        }
        val qrBoxBorderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            this.style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(qrBoxRect, 16f, 16f, qrBoxBgPaint)
        canvas.drawRoundRect(qrBoxRect, 16f, 16f, qrBoxBorderPaint)

        val qrTitlePaint = Paint().apply {
            color = primaryColor
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📲 ESCANEA PARA PEDIR POR WHATSAPP", pageWidth / 2f, 500f, qrTitlePaint)

        // Generate Real QR Bitmap
        val cleanPhone = profile.whatsapp.replace(Regex("[^0-9]"), "")
        val waUrl = if (cleanPhone.isNotBlank()) {
            "https://wa.me/$cleanPhone?text=Hola%20${profile.name},%20me%20gustaria%20realizar%20un%20pedido%20de%20su%20catalogo"
        } else {
            profile.website.ifBlank { "https://whatsapp.com" }
        }

        val qrBitmap = QrCodeGenerator.generateQrBitmap(
            content = waUrl,
            size = 320,
            foregroundColor = Color.parseColor("#0F172A"),
            backgroundColor = Color.WHITE
        )

        val qrDstRect = RectF(pageWidth / 2f - 90f, 520f, pageWidth / 2f + 90f, 700f)
        val qrPaint = Paint().apply { isFilterBitmap = true }
        canvas.drawBitmap(qrBitmap, null, qrDstRect, qrPaint)

        val qrInstructionPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
            textSize = 10.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Apunta con la cámara de tu celular para chatear con nosotros automáticamente", pageWidth / 2f, 722f, qrInstructionPaint)

        drawFooter(canvas, profile, pageWidth, pageHeight, pageNumber, totalPages, primaryColor, isDarkTheme)
    }

    // -----------------------------------------------------------------
    // FOOTER RENDERER
    // -----------------------------------------------------------------
    private fun drawFooter(
        canvas: Canvas,
        profile: CompanyProfile,
        pageWidth: Int,
        pageHeight: Int,
        pageNumber: Int,
        totalPages: Int,
        primaryColor: Int,
        isDarkTheme: Boolean
    ) {
        val footerY = (pageHeight - 25).toFloat()

        val linePaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")
            strokeWidth = 1f
        }
        canvas.drawLine(30f, footerY - 12f, (pageWidth - 30).toFloat(), footerY - 12f, linePaint)

        val footerTextPaint = Paint().apply {
            color = if (isDarkTheme) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textSize = 9f
        }
        canvas.drawText("${profile.name} | WhatsApp: ${profile.whatsapp}", 35f, footerY, footerTextPaint)

        val pageNumPaint = Paint().apply {
            color = primaryColor
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Página $pageNumber de $totalPages", (pageWidth - 35).toFloat(), footerY, pageNumPaint)
    }

    private fun safeParseColor(colorHex: String, defaultColor: Int): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            defaultColor
        }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            if ((currentLine + " " + word).trim().length <= maxCharsPerLine) {
                currentLine = (currentLine + " " + word).trim()
            } else {
                if (currentLine.isNotBlank()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotBlank()) lines.add(currentLine)
        return lines
    }
}
