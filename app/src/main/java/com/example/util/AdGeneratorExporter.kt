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
import java.io.InputStream
import java.text.NumberFormat
import java.util.*

object AdGeneratorExporter {

    enum class AdTemplateType(val displayName: String, val description: String) {
        UNIPRODUCTO_A4("Volante Uniproducto (A4)", "Volante publicitario completo para 1 producto destacado con ficha y QR"),
        MULTIPRODUCTO_A4("Volante Multiproducto (A4)", "Grilla con múltiples productos en oferta y precios destacados"),
        CARTEL_VITRINA_A4("Cartel para Vitrina / Ventana", "Diseño de alto impacto con precio gigante para vitrinas y mostradores"),
        AVISO_POSTE_A4("Aviso para Postes y Muros", "Formato de alta visibilidad para imprimir y pegar en la calle/comunidad"),
        SOCIAL_POST_1X1("Publicación Cuadrada 1:1 (Redes)", "Formato óptimo para Feed de Instagram y Facebook (1080x1080)"),
        SOCIAL_STORY_9X16("Estado / Historia 9:16 (WhatsApp/IG)", "Formato vertical para Estados de WhatsApp e Historias (1080x1920)")
    }

    data class AdConfig(
        val templateType: AdTemplateType = AdTemplateType.UNIPRODUCTO_A4,
        val badgeText: String = "¡OFERTA ESPECIAL!",
        val discountPercent: Int = 0, // e.g. 20 for 20% OFF
        val customPromoPrice: Double? = null,
        val callToAction: String = "¡Pide el tuyo ahora por WhatsApp!",
        val showQrCode: Boolean = true,
        val primaryColorHex: String = "#DC2626", // Default vibrant red
        val secondaryColorHex: String = "#FACC15" // Default bright yellow
    )

    /**
     * Generates a high-res Bitmap preview or export for the chosen Ad configuration.
     */
    fun generateAdBitmap(
        context: Context,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        config: AdConfig
    ): Bitmap {
        val (width, height) = when (config.templateType) {
            AdTemplateType.SOCIAL_POST_1X1 -> Pair(1080, 1080)
            AdTemplateType.SOCIAL_STORY_9X16 -> Pair(1080, 1920)
            else -> Pair(1240, 1754) // High-res A4 ratio (approx 150 DPI)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawAdContent(context, canvas, width.toFloat(), height.toFloat(), profile, products, config)
        return bitmap
    }

    /**
     * Generates a PDF document for print ready output.
     */
    fun generateAdPdf(
        context: Context,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        config: AdConfig
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            drawAdContent(context, page.canvas, pageWidth.toFloat(), pageHeight.toFloat(), profile, products, config)

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Publicidad_${System.currentTimeMillis()}.pdf")
            FileOutputStream(file).use { fos ->
                pdfDocument.writeTo(fos)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves PNG Image and shares via Android Sharesheet.
     */
    fun exportAndShareImage(
        context: Context,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        config: AdConfig
    ) {
        try {
            val bitmap = generateAdBitmap(context, profile, products, config)
            val cacheDir = File(context.cacheDir, "shared_ads")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, "Publicidad_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Publicidad - ${profile.name}")
                putExtra(Intent.EXTRA_TEXT, "¡Hola! Te comparto esta promoción especial de ${profile.name}. ${config.callToAction}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Publicidad por..."))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al compartir la imagen publicitarias.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves PDF and shares via Android Sharesheet.
     */
    fun exportAndSharePdf(
        context: Context,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        config: AdConfig
    ) {
        val pdfFile = generateAdPdf(context, profile, products, config)
        if (pdfFile != null && pdfFile.exists()) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Volante Publicitario PDF - ${profile.name}")
                    putExtra(Intent.EXTRA_TEXT, "Adjunto volante impreso de ${profile.name}.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir Volante PDF por..."))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "No se pudo generar el archivo PDF.", Toast.LENGTH_SHORT).show()
        }
    }

    // -------------------------------------------------------------
    // DRAWING TEMPLATE LOGIC
    // -------------------------------------------------------------
    private fun drawAdContent(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        config: AdConfig
    ) {
        when (config.templateType) {
            AdTemplateType.UNIPRODUCTO_A4 -> drawUniproductoA4(context, canvas, w, h, profile, products.firstOrNull(), config)
            AdTemplateType.MULTIPRODUCTO_A4 -> drawMultiproductoA4(context, canvas, w, h, profile, products, config)
            AdTemplateType.CARTEL_VITRINA_A4 -> drawCartelVitrinaA4(context, canvas, w, h, profile, products.firstOrNull(), config)
            AdTemplateType.AVISO_POSTE_A4 -> drawAvisoPosteA4(context, canvas, w, h, profile, products.firstOrNull(), config)
            AdTemplateType.SOCIAL_POST_1X1 -> drawSocialPost1x1(context, canvas, w, h, profile, products.firstOrNull(), config)
            AdTemplateType.SOCIAL_STORY_9X16 -> drawSocialStory9x16(context, canvas, w, h, profile, products.firstOrNull(), config)
        }
    }

    private fun drawUniproductoA4(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        product: CatalogProduct?,
        config: AdConfig
    ) {
        val primaryColor = parseColor(config.primaryColorHex, Color.parseColor("#DC2626"))
        val secondaryColor = parseColor(config.secondaryColorHex, Color.parseColor("#FACC15"))
        val numberFormat = getNumberFormat()

        // Background
        canvas.drawColor(Color.WHITE)

        // 1. Top Header Banner
        val headerHeight = h * 0.16f
        val headerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, w, headerHeight, headerPaint)

        // Company Logo or Title
        val logoBitmap = loadBitmapFromUriOrPath(context, profile.logoUri, 120, 120)
        var textStartX = w * 0.06f

        if (logoBitmap != null) {
            val logoSize = headerHeight * 0.65f
            val logoRect = RectF(w * 0.05f, (headerHeight - logoSize) / 2f, w * 0.05f + logoSize, (headerHeight - logoSize) / 2f + logoSize)
            canvas.drawBitmap(logoBitmap, null, logoRect, null)
            textStartX = logoRect.right + (w * 0.04f)
        }

        val companyNamePaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.032f
            isFakeBoldText = true
        }
        canvas.drawText(profile.name.ifBlank { "MI NEGOCIO" }.uppercase(), textStartX, headerHeight * 0.45f, companyNamePaint)

        val sloganPaint = Paint().apply {
            color = Color.parseColor("#FEF08A")
            textSize = h * 0.018f
        }
        canvas.drawText(profile.slogan.ifBlank { "¡Las mejores ofertas del mercado!" }, textStartX, headerHeight * 0.72f, sloganPaint)

        // 2. Promotional Badge Ribbon
        if (config.badgeText.isNotBlank()) {
            val badgeHeight = h * 0.055f
            val badgePaint = Paint().apply { color = secondaryColor }
            canvas.drawRect(0f, headerHeight, w, headerHeight + badgeHeight, badgePaint)

            val badgeTextPaint = Paint().apply {
                color = Color.parseColor("#991B1B")
                textSize = h * 0.026f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("⚡ ${config.badgeText.uppercase()} ⚡", w / 2f, headerHeight + (badgeHeight * 0.68f), badgeTextPaint)
        }

        val startBodyY = headerHeight + (if (config.badgeText.isNotBlank()) h * 0.055f else 0f) + (h * 0.02f)

        // 3. Product Photo Box
        val photoBoxSize = w * 0.52f
        val photoBoxRect = RectF(w * 0.06f, startBodyY, w * 0.06f + photoBoxSize, startBodyY + photoBoxSize)

        val photoCardPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
        val photoCardBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(photoBoxRect, 20f, 20f, photoCardPaint)
        canvas.drawRoundRect(photoBoxRect, 20f, 20f, photoCardBorder)

        val prodBitmap = loadBitmapFromUriOrPath(context, product?.photoUri ?: "", 500, 500)
        if (prodBitmap != null) {
            val padding = 15f
            val innerRect = RectF(photoBoxRect.left + padding, photoBoxRect.top + padding, photoBoxRect.right - padding, photoBoxRect.bottom - padding)
            canvas.drawBitmap(prodBitmap, null, innerRect, null)
        } else {
            val noImgPaint = Paint().apply {
                color = Color.GRAY
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("📷 Foto del Producto", photoBoxRect.centerX(), photoBoxRect.centerY(), noImgPaint)
        }

        // 4. Product Details Right Column
        val detailsLeft = photoBoxRect.right + (w * 0.04f)
        val detailsWidth = w - detailsLeft - (w * 0.05f)

        val prodTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = h * 0.028f
            isFakeBoldText = true
        }

        val nameText = product?.name ?: "Producto en Oferta"
        drawMultilineText(canvas, nameText, detailsLeft, startBodyY + (h * 0.035f), detailsWidth, prodTitlePaint, maxLines = 3)

        var detailsY = startBodyY + (h * 0.12f)

        if (product?.brand?.isNotBlank() == true) {
            val brandPaint = Paint().apply { color = Color.parseColor("#64748B"); textSize = h * 0.016f }
            canvas.drawText("Marca: ${product.brand}", detailsLeft, detailsY, brandPaint)
            detailsY += h * 0.025f
        }

        if (product?.sku?.isNotBlank() == true) {
            val skuPaint = Paint().apply { color = Color.parseColor("#64748B"); textSize = h * 0.016f }
            canvas.drawText("Ref/SKU: ${product.sku}", detailsLeft, detailsY, skuPaint)
            detailsY += h * 0.035f
        }

        // Price Section inside Right Column
        val originalPrice = product?.sellingPrice ?: 0.0
        val finalPrice = calculateFinalPrice(originalPrice, config)

        if (config.discountPercent > 0 || config.customPromoPrice != null || product?.previousPrice ?: 0.0 > 0) {
            val oldPriceVal = if (product?.previousPrice ?: 0.0 > originalPrice) product!!.previousPrice else originalPrice
            val oldPriceStr = "Antes: ${numberFormat.format(oldPriceVal)}"

            val oldPricePaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = h * 0.02f
                isStrikeThruText = true
            }
            canvas.drawText(oldPriceStr, detailsLeft, detailsY, oldPricePaint)
            detailsY += h * 0.04f
        }

        // GIANT PROMO PRICE TAG
        val priceBoxRect = RectF(detailsLeft, detailsY, detailsLeft + detailsWidth, detailsY + (h * 0.085f))
        val priceBoxPaint = Paint().apply { color = primaryColor }
        canvas.drawRoundRect(priceBoxRect, 16f, 16f, priceBoxPaint)

        val priceTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.038f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberFormat.format(finalPrice), priceBoxRect.centerX(), priceBoxRect.centerY() + (h * 0.012f), priceTextPaint)

        // 5. Description & Features Section
        var descY = maxOf(photoBoxRect.bottom, detailsY + (h * 0.12f)) + (h * 0.03f)

        val descHeaderPaint = Paint().apply {
            color = primaryColor
            textSize = h * 0.022f
            isFakeBoldText = true
        }
        canvas.drawText("DESCRIPCIÓN & CARACTERÍSTICAS", w * 0.06f, descY, descHeaderPaint)
        descY += h * 0.025f

        val linePaint = Paint().apply { color = primaryColor; strokeWidth = 3f }
        canvas.drawLine(w * 0.06f, descY, w * 0.94f, descY, linePaint)
        descY += h * 0.03f

        val descBodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = h * 0.018f
        }

        val descriptionText = product?.fullDescription?.ifBlank { product.shortDescription }
            ?: "Aprovecha esta excelente oportunidad con garantía de calidad y el mejor servicio. Disponibilidad inmediata para entrega o envío."

        descY = drawMultilineText(canvas, descriptionText, w * 0.06f, descY, w * 0.88f, descBodyPaint, maxLines = 6)

        // 6. Call To Action Banner
        val ctaY = h * 0.72f
        val ctaHeight = h * 0.065f
        val ctaRect = RectF(w * 0.05f, ctaY, w * 0.95f, ctaY + ctaHeight)
        val ctaPaint = Paint().apply { color = secondaryColor }
        canvas.drawRoundRect(ctaRect, 20f, 20f, ctaPaint)

        val ctaTextPaint = Paint().apply {
            color = Color.parseColor("#78350F")
            textSize = h * 0.024f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("👉 ${config.callToAction.uppercase()} 👈", ctaRect.centerX(), ctaRect.centerY() + (h * 0.008f), ctaTextPaint)

        // 7. Footer & Contact + QR Section
        val footerY = ctaY + ctaHeight + (h * 0.02f)
        val footerHeight = h - footerY - (h * 0.02f)

        val footerBg = Paint().apply { color = Color.parseColor("#F8FAFC") }
        val footerRect = RectF(w * 0.05f, footerY, w * 0.95f, h - (h * 0.02f))
        canvas.drawRoundRect(footerRect, 16f, 16f, footerBg)

        // Left contact info
        var contactY = footerY + (h * 0.035f)
        val contactTextPaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = h * 0.016f }
        val boldContactPaint = Paint().apply { color = primaryColor; textSize = h * 0.018f; isFakeBoldText = true }

        canvas.drawText("INFORMACIÓN DE PEDIDOS", w * 0.08f, contactY, boldContactPaint)
        contactY += h * 0.028f

        if (profile.whatsapp.isNotBlank()) {
            canvas.drawText("📱 WhatsApp: ${profile.whatsapp}", w * 0.08f, contactY, contactTextPaint)
            contactY += h * 0.024f
        }
        if (profile.phone.isNotBlank()) {
            canvas.drawText("📞 Teléfono: ${profile.phone}", w * 0.08f, contactY, contactTextPaint)
            contactY += h * 0.024f
        }
        if (profile.address.isNotBlank()) {
            canvas.drawText("📍 Dirección: ${profile.address}, ${profile.city}", w * 0.08f, contactY, contactTextPaint)
            contactY += h * 0.024f
        }
        if (profile.instagram.isNotBlank() || profile.facebook.isNotBlank()) {
            canvas.drawText("📲 Redes: ${profile.instagram} ${profile.facebook}", w * 0.08f, contactY, contactTextPaint)
        }

        // Right QR Code
        if (config.showQrCode && profile.whatsapp.isNotBlank()) {
            val qrSize = footerHeight * 0.72f
            val qrLeft = w * 0.92f - qrSize
            val qrTop = footerY + (footerHeight - qrSize) / 2f

            val waClean = profile.whatsapp.replace(Regex("[^0-9]"), "")
            val waUrl = "https://wa.me/$waClean?text=Hola,%20quisiera%20pedir%20información%20de%20${Uri.encode(product?.name ?: "este producto")}"

            val qrBitmap = QrCodeGenerator.generateQrBitmap(waUrl, 250, Color.BLACK, Color.WHITE)
            canvas.drawBitmap(qrBitmap, null, RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize), null)

            val qrLabelPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = h * 0.011f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Escanea para pedir", qrLeft + (qrSize / 2f), qrTop + qrSize + (h * 0.015f), qrLabelPaint)
        }
    }

    private fun drawMultiproductoA4(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        products: List<CatalogProduct>,
        config: AdConfig
    ) {
        val primaryColor = parseColor(config.primaryColorHex, Color.parseColor("#DC2626"))
        val secondaryColor = parseColor(config.secondaryColorHex, Color.parseColor("#FACC15"))
        val numberFormat = getNumberFormat()

        canvas.drawColor(Color.WHITE)

        // Header
        val headerHeight = h * 0.12f
        val headerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, w, headerHeight, headerPaint)

        val companyNamePaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.028f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.name.ifBlank { "MI NEGOCIO" }.uppercase(), w / 2f, headerHeight * 0.45f, companyNamePaint)

        val sloganPaint = Paint().apply {
            color = Color.parseColor("#FEF08A")
            textSize = h * 0.016f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🔥 ${config.badgeText.ifBlank { "GRAN CATÁLOGO DE OFERTAS" }} 🔥", w / 2f, headerHeight * 0.8f, sloganPaint)

        // Grid of products (2 columns x 3 rows max 6 items)
        val gridTop = headerHeight + (h * 0.02f)
        val gridBottom = h * 0.82f
        val cols = 2
        val rows = 3
        val cardWidth = (w * 0.88f) / cols
        val cardHeight = (gridBottom - gridTop) / rows

        val displayProducts = products.take(6)

        for (idx in displayProducts.indices) {
            val p = displayProducts[idx]
            val c = idx % cols
            val r = idx / cols

            val cardLeft = (w * 0.05f) + (c * (cardWidth + (w * 0.02f)))
            val cardTop = gridTop + (r * cardHeight)
            val cardRect = RectF(cardLeft, cardTop + 5f, cardLeft + cardWidth, cardTop + cardHeight - 15f)

            // Card background
            val cardBg = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val cardBorder = Paint().apply { color = Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 2f }
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBg)
            canvas.drawRoundRect(cardRect, 12f, 12f, cardBorder)

            // Image
            val imgSize = cardRect.width() * 0.42f
            val imgRect = RectF(cardRect.left + 10f, cardRect.top + (cardRect.height() - imgSize) / 2f, cardRect.left + 10f + imgSize, cardRect.top + (cardRect.height() - imgSize) / 2f + imgSize)

            val pBitmap = loadBitmapFromUriOrPath(context, p.photoUri, 200, 200)
            if (pBitmap != null) {
                canvas.drawBitmap(pBitmap, null, imgRect, null)
            } else {
                val placeholderPaint = Paint().apply { color = Color.LTGRAY }
                canvas.drawRoundRect(imgRect, 8f, 8f, placeholderPaint)
            }

            // Product text details
            val textLeft = imgRect.right + 12f
            val textWidth = cardRect.right - textLeft - 8f

            val titlePaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = h * 0.017f; isFakeBoldText = true }
            drawMultilineText(canvas, p.name, textLeft, cardRect.top + 25f, textWidth, titlePaint, maxLines = 2)

            val finalPrice = calculateFinalPrice(p.sellingPrice, config)

            var priceY = cardRect.bottom - 45f
            if (p.sellingPrice > finalPrice) {
                val oldPricePaint = Paint().apply { color = Color.GRAY; textSize = h * 0.013f; isStrikeThruText = true }
                canvas.drawText("Antes: ${numberFormat.format(p.sellingPrice)}", textLeft, priceY - 18f, oldPricePaint)
            }

            val pricePaint = Paint().apply { color = primaryColor; textSize = h * 0.022f; isFakeBoldText = true }
            canvas.drawText(numberFormat.format(finalPrice), textLeft, priceY, pricePaint)
        }

        // Footer CTA & Contact
        val footerRect = RectF(w * 0.04f, h * 0.83f, w * 0.96f, h * 0.97f)
        val footerPaint = Paint().apply { color = primaryColor }
        canvas.drawRoundRect(footerRect, 16f, 16f, footerPaint)

        val footerTitle = Paint().apply { color = Color.WHITE; textSize = h * 0.022f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("📱 PEDIDOS WHATSAPP: ${profile.whatsapp}", footerRect.centerX(), footerRect.top + (h * 0.04f), footerTitle)

        val footerSub = Paint().apply { color = Color.parseColor("#FEF08A"); textSize = h * 0.016f; textAlign = Paint.Align.CENTER }
        canvas.drawText("${config.callToAction} | ${profile.address} ${profile.city}", footerRect.centerX(), footerRect.top + (h * 0.08f), footerSub)
    }

    private fun drawCartelVitrinaA4(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        product: CatalogProduct?,
        config: AdConfig
    ) {
        val primaryColor = parseColor(config.primaryColorHex, Color.parseColor("#DC2626"))
        val secondaryColor = parseColor(config.secondaryColorHex, Color.parseColor("#FACC15"))
        val numberFormat = getNumberFormat()

        // High contrast store window poster canvas
        canvas.drawColor(Color.parseColor("#0F172A")) // Deep dark night background for vibrant standout

        // Header Store Name
        val storePaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.028f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.name.ifBlank { "MI TIENDA" }.uppercase(), w / 2f, h * 0.06f, storePaint)

        // Giant Badge Box
        val badgeRect = RectF(w * 0.05f, h * 0.09f, w * 0.95f, h * 0.17f)
        val badgePaint = Paint().apply { color = secondaryColor }
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#78350F")
            textSize = h * 0.042f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(config.badgeText.ifBlank { "¡OFERTA IMPERDIBLE!" }.uppercase(), badgeRect.centerX(), badgeRect.centerY() + (h * 0.014f), badgeTextPaint)

        // Product Photo
        val photoSize = w * 0.62f
        val photoRect = RectF((w - photoSize) / 2f, h * 0.19f, (w + photoSize) / 2f, h * 0.19f + photoSize)

        val pBitmap = loadBitmapFromUriOrPath(context, product?.photoUri ?: "", 500, 500)
        val photoBg = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(photoRect, 24f, 24f, photoBg)

        if (pBitmap != null) {
            val innerRect = RectF(photoRect.left + 15f, photoRect.top + 15f, photoRect.right - 15f, photoRect.bottom - 15f)
            canvas.drawBitmap(pBitmap, null, innerRect, null)
        }

        // Product Name
        val nameY = photoRect.bottom + (h * 0.04f)
        val namePaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.032f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(product?.name?.uppercase() ?: "PRODUCTO DESTACADO", w / 2f, nameY, namePaint)

        // GIANT PRICE TAG (35% of the poster)
        val finalPrice = calculateFinalPrice(product?.sellingPrice ?: 0.0, config)
        val priceBoxY = nameY + (h * 0.025f)
        val priceBoxRect = RectF(w * 0.04f, priceBoxY, w * 0.96f, priceBoxY + (h * 0.16f))

        val priceBgPaint = Paint().apply { color = primaryColor }
        canvas.drawRoundRect(priceBoxRect, 24f, 24f, priceBgPaint)

        val giantPricePaint = Paint().apply {
            color = secondaryColor
            textSize = h * 0.075f // HUGE TEXT FOR WINDOW DISPLAY
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberFormat.format(finalPrice), priceBoxRect.centerX(), priceBoxRect.centerY() + (h * 0.025f), giantPricePaint)

        // Store Phone / WhatsApp Footer
        val footerY = priceBoxRect.bottom + (h * 0.03f)
        val phonePaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.032f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📱 WHATSAPP: ${profile.whatsapp}", w / 2f, footerY, phonePaint)

        val addressPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = h * 0.018f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📍 ${profile.address} ${profile.city} | ${config.callToAction}", w / 2f, footerY + (h * 0.032f), addressPaint)
    }

    private fun drawAvisoPosteA4(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        product: CatalogProduct?,
        config: AdConfig
    ) {
        val numberFormat = getNumberFormat()

        // Street poster layout - High contrast yellow & black design
        canvas.drawColor(Color.parseColor("#FACC15")) // Bright yellow paper

        // Big black title header
        val headerHeight = h * 0.16f
        val headerPaint = Paint().apply { color = Color.BLACK }
        canvas.drawRect(0f, 0f, w, headerHeight, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#FACC15")
            textSize = h * 0.042f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("¡ATENCIÓN! OFERTA ESPECIAL", w / 2f, headerHeight * 0.42f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.022f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.name.uppercase(), w / 2f, headerHeight * 0.78f, subtitlePaint)

        // Product Name
        val prodNameY = headerHeight + (h * 0.05f)
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = h * 0.038f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(product?.name?.uppercase() ?: "OFERTA IMPERDIBLE", w / 2f, prodNameY, namePaint)

        // Photo
        val photoSize = w * 0.55f
        val photoRect = RectF((w - photoSize) / 2f, prodNameY + (h * 0.02f), (w + photoSize) / 2f, prodNameY + (h * 0.02f) + photoSize)
        val pBitmap = loadBitmapFromUriOrPath(context, product?.photoUri ?: "", 500, 500)

        val photoBg = Paint().apply { color = Color.WHITE }
        val photoBorder = Paint().apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 6f }
        canvas.drawRect(photoRect, photoBg)
        canvas.drawRect(photoRect, photoBorder)

        if (pBitmap != null) {
            canvas.drawBitmap(pBitmap, null, RectF(photoRect.left + 10f, photoRect.top + 10f, photoRect.right - 10f, photoRect.bottom - 10f), null)
        }

        // Giant Price
        val finalPrice = calculateFinalPrice(product?.sellingPrice ?: 0.0, config)
        val priceY = photoRect.bottom + (h * 0.07f)

        val pricePaint = Paint().apply {
            color = Color.BLACK
            textSize = h * 0.075f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(numberFormat.format(finalPrice), w / 2f, priceY, pricePaint)

        // Giant Phone Box
        val phoneY = priceY + (h * 0.03f)
        val phoneBoxRect = RectF(w * 0.05f, phoneY, w * 0.95f, phoneY + (h * 0.12f))
        canvas.drawRect(phoneBoxRect, headerPaint)

        val phoneTextPaint = Paint().apply {
            color = Color.parseColor("#FACC15")
            textSize = h * 0.042f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📱 WHATSAPP / LLAMADAS:", phoneBoxRect.centerX(), phoneBoxRect.top + (h * 0.045f), phoneTextPaint)

        val phoneNumPaint = Paint().apply {
            color = Color.WHITE
            textSize = h * 0.052f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(profile.whatsapp.ifBlank { profile.phone }, phoneBoxRect.centerX(), phoneBoxRect.top + (h * 0.098f), phoneNumPaint)

        // Tear-off strips preview at bottom
        val stripY = h * 0.88f
        val dashPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        canvas.drawLine(0f, stripY, w, stripY, dashPaint)

        val stripTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = h * 0.014f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📍 ${profile.address} ${profile.city} | ${config.callToAction}", w / 2f, stripY + (h * 0.04f), stripTextPaint)
    }

    private fun drawSocialPost1x1(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        product: CatalogProduct?,
        config: AdConfig
    ) {
        val primaryColor = parseColor(config.primaryColorHex, Color.parseColor("#DC2626"))
        val secondaryColor = parseColor(config.secondaryColorHex, Color.parseColor("#FACC15"))
        val numberFormat = getNumberFormat()

        // Trendy dark linear gradient background
        val bgShader = LinearGradient(0f, 0f, w, h, Color.parseColor("#0F172A"), Color.parseColor("#1E293B"), Shader.TileMode.CLAMP)
        val bgPaint = Paint().apply { shader = bgShader }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Top Header
        val logoBitmap = loadBitmapFromUriOrPath(context, profile.logoUri, 100, 100)
        val topY = h * 0.08f

        if (logoBitmap != null) {
            canvas.drawBitmap(logoBitmap, null, RectF(w * 0.06f, topY - 30f, w * 0.06f + 70f, topY + 40f), null)
        }

        val brandPaint = Paint().apply { color = Color.WHITE; textSize = h * 0.038f; isFakeBoldText = true }
        canvas.drawText(profile.name.uppercase(), w * 0.06f + (if (logoBitmap != null) 90f else 0f), topY + 15f, brandPaint)

        // Badge Pill
        val badgeRect = RectF(w * 0.6f, topY - 25f, w * 0.94f, topY + 35f)
        val badgeBg = Paint().apply { color = secondaryColor }
        canvas.drawRoundRect(badgeRect, 30f, 30f, badgeBg)

        val badgeText = Paint().apply { color = Color.parseColor("#78350F"); textSize = h * 0.024f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(config.badgeText, badgeRect.centerX(), badgeRect.centerY() + 8f, badgeText)

        // Product Image in Center Container
        val imgSize = w * 0.58f
        val imgRect = RectF((w - imgSize) / 2f, h * 0.2f, (w + imgSize) / 2f, h * 0.2f + imgSize)

        val cardBg = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(imgRect, 32f, 32f, cardBg)

        val pBitmap = loadBitmapFromUriOrPath(context, product?.photoUri ?: "", 500, 500)
        if (pBitmap != null) {
            canvas.drawBitmap(pBitmap, null, RectF(imgRect.left + 15f, imgRect.top + 15f, imgRect.right - 15f, imgRect.bottom - 15f), null)
        }

        // Product Name
        val nameY = imgRect.bottom + (h * 0.05f)
        val namePaint = Paint().apply { color = Color.WHITE; textSize = h * 0.036f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(product?.name ?: "Producto en Oferta", w / 2f, nameY, namePaint)

        // Price Tag
        val finalPrice = calculateFinalPrice(product?.sellingPrice ?: 0.0, config)
        val priceY = nameY + (h * 0.06f)

        val pricePaint = Paint().apply { color = secondaryColor; textSize = h * 0.052f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(numberFormat.format(finalPrice), w / 2f, priceY, pricePaint)

        // Footer Contact
        val footerY = h * 0.92f
        val contactPaint = Paint().apply { color = Color.parseColor("#94A3B8"); textSize = h * 0.024f; textAlign = Paint.Align.CENTER }
        canvas.drawText("💬 WhatsApp: ${profile.whatsapp} | ${profile.instagram}", w / 2f, footerY, contactPaint)
    }

    private fun drawSocialStory9x16(
        context: Context,
        canvas: Canvas,
        w: Float,
        h: Float,
        profile: CompanyProfile,
        product: CatalogProduct?,
        config: AdConfig
    ) {
        val primaryColor = parseColor(config.primaryColorHex, Color.parseColor("#DC2626"))
        val secondaryColor = parseColor(config.secondaryColorHex, Color.parseColor("#FACC15"))
        val numberFormat = getNumberFormat()

        // Story Background
        val bgShader = LinearGradient(0f, 0f, 0f, h, primaryColor, Color.parseColor("#0F172A"), Shader.TileMode.CLAMP)
        val bgPaint = Paint().apply { shader = bgShader }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Header Title
        val headerY = h * 0.08f
        val storePaint = Paint().apply { color = Color.WHITE; textSize = h * 0.026f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(profile.name.uppercase(), w / 2f, headerY, storePaint)

        val badgeTextY = headerY + (h * 0.04f)
        val badgePaint = Paint().apply { color = secondaryColor; textSize = h * 0.038f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("⚡ ${config.badgeText.uppercase()} ⚡", w / 2f, badgeTextY, badgePaint)

        // Center Product Card
        val cardWidth = w * 0.82f
        val cardHeight = h * 0.45f
        val cardRect = RectF((w - cardWidth) / 2f, h * 0.18f, (w + cardWidth) / 2f, h * 0.18f + cardHeight)

        val cardBg = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(cardRect, 40f, 40f, cardBg)

        val pBitmap = loadBitmapFromUriOrPath(context, product?.photoUri ?: "", 600, 600)
        if (pBitmap != null) {
            canvas.drawBitmap(pBitmap, null, RectF(cardRect.left + 20f, cardRect.top + 20f, cardRect.right - 20f, cardRect.bottom - 20f), null)
        }

        // Product Name
        val nameY = cardRect.bottom + (h * 0.04f)
        val namePaint = Paint().apply { color = Color.WHITE; textSize = h * 0.028f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(product?.name ?: "Producto Destacado", w / 2f, nameY, namePaint)

        // Giant Price Floating Pill
        val finalPrice = calculateFinalPrice(product?.sellingPrice ?: 0.0, config)
        val priceY = nameY + (h * 0.02f)
        val priceBoxRect = RectF(w * 0.15f, priceY, w * 0.85f, priceY + (h * 0.08f))

        val priceBoxBg = Paint().apply { color = secondaryColor }
        canvas.drawRoundRect(priceBoxRect, 30f, 30f, priceBoxBg)

        val priceTextPaint = Paint().apply { color = Color.parseColor("#78350F"); textSize = h * 0.042f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText(numberFormat.format(finalPrice), priceBoxRect.centerX(), priceBoxRect.centerY() + 15f, priceTextPaint)

        // Bottom CTA Box (WhatsApp Swipe Up style)
        val ctaY = h * 0.82f
        val ctaBox = RectF(w * 0.08f, ctaY, w * 0.92f, ctaY + (h * 0.09f))
        val ctaBg = Paint().apply { color = Color.WHITE }
        canvas.drawRoundRect(ctaBox, 24f, 24f, ctaBg)

        val ctaTextPaint = Paint().apply { color = primaryColor; textSize = h * 0.022f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        canvas.drawText("💬 ${config.callToAction}", ctaBox.centerX(), ctaBox.centerY() - 10f, ctaTextPaint)

        val numPaint = Paint().apply { color = Color.parseColor("#0F172A"); textSize = h * 0.02f; textAlign = Paint.Align.CENTER }
        canvas.drawText("Escríbenos al: ${profile.whatsapp}", ctaBox.centerX(), ctaBox.centerY() + 25f, numPaint)
    }

    // -------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------
    private fun calculateFinalPrice(basePrice: Double, config: AdConfig): Double {
        if (config.customPromoPrice != null && config.customPromoPrice > 0) {
            return config.customPromoPrice
        }
        if (config.discountPercent in 1..99) {
            return basePrice * (1.0 - (config.discountPercent / 100.0))
        }
        return basePrice
    }

    private fun getNumberFormat(): NumberFormat {
        return NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }

    private fun parseColor(colorHex: String, fallback: Int): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            fallback
        }
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        maxLines: Int = 4
    ): Float {
        val words = text.split(" ")
        var line = ""
        var currentY = startY
        var linesDrawn = 0

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val measure = paint.measureText(testLine)

            if (measure > maxWidth) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += paint.textSize * 1.25f
                linesDrawn++
                if (linesDrawn >= maxLines) break
            } else {
                line = testLine
            }
        }

        if (line.isNotEmpty() && linesDrawn < maxLines) {
            canvas.drawText(line, x, currentY, paint)
            currentY += paint.textSize * 1.25f
        }

        return currentY
    }

    private fun loadBitmapFromUriOrPath(context: Context, pathOrUri: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (pathOrUri.isBlank()) return null
        return try {
            val inputStream: InputStream? = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(pathOrUri))
            } else {
                val file = File(pathOrUri)
                if (file.exists()) file.inputStream() else null
            }

            inputStream?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
