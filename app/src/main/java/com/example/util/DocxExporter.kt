package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.MeetingMinute
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxExporter {

    /**
     * Generates a native Microsoft Word (.docx) document for the given MeetingMinute.
     */
    fun generateDocx(context: Context, minute: MeetingMinute): File {
        val safeFolio = if (minute.folioNumber.isBlank()) "000" else minute.folioNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val fileName = "Acta_$safeFolio.docx"
        val outFile = File(context.cacheDir, fileName)

        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(getContentTypesXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(getGlobalRelsXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. word/_rels/document.xml.rels
            zos.putNextEntry(ZipEntry("word/_rels/document.xml.rels"))
            zos.write(getDocumentRelsXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. word/styles.xml
            zos.putNextEntry(ZipEntry("word/styles.xml"))
            zos.write(getStylesXml().toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. word/document.xml
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(getDocumentXml(minute).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        return outFile
    }

    /**
     * Triggers native Android ACTION_SEND intent to share the .docx file via WhatsApp, Email, Drive, etc.
     */
    fun shareDocx(context: Context, file: File, folioNumber: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Acta de Reunión - $folioNumber")
            putExtra(Intent.EXTRA_TEXT, "Adjunto Acta de Reunión $folioNumber en formato Word (.docx).")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Compartir Acta por WhatsApp / Aplicaciones"))
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>""".trimIndent()
    }

    private fun getGlobalRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
    }

    private fun getDocumentRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
    }

    private fun getStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:docDefaults>
    <w:rPrDefault>
      <w:rPr>
        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri"/>
        <w:sz w:val="22"/>
        <w:color w:val="333333"/>
      </w:rPr>
    </w:rPrDefault>
  </w:docDefaults>
</w:styles>""".trimIndent()
    }

    private fun getDocumentXml(minute: MeetingMinute): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
        sb.append("<w:body>")

        // Document Title Banner
        sb.append(
            """
            <w:p>
                <w:pPr>
                    <w:jc w:val="center"/>
                    <w:spacing w:before="200" w:after="100"/>
                </w:pPr>
                <w:r>
                    <w:rPr>
                        <w:b/>
                        <w:sz w:val="36"/>
                        <w:color w:val="1E3A8A"/>
                    </w:rPr>
                    <w:t>ACTA DE REUNIÓN</w:t>
                </w:r>
            </w:p>
            """.trimIndent()
        )

        // Subtitle Folio
        val folioText = if (minute.folioNumber.isBlank()) "Acta N° Sin Folio" else minute.folioNumber
        sb.append(
            """
            <w:p>
                <w:pPr>
                    <w:jc w:val="center"/>
                    <w:spacing w:after="300"/>
                </w:pPr>
                <w:r>
                    <w:rPr>
                        <w:b/>
                        <w:sz w:val="26"/>
                        <w:color w:val="475569"/>
                    </w:rPr>
                    <w:t>${escapeXml(folioText)}</w:t>
                </w:r>
            </w:p>
            """.trimIndent()
        )

        // Metadata Box Table
        sb.append(
            """
            <w:tbl>
                <w:tblPr>
                    <w:tblW w:w="5000" w:type="pct"/>
                    <w:tblBorders>
                        <w:top w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                        <w:bottom w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                        <w:left w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                        <w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                        <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
                        <w:insideV w:val="none"/>
                    </w:tblBorders>
                </w:tblPr>
                <w:tr>
                    <w:tc>
                        <w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="F1F5F9"/></w:tcPr>
                        <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Fecha y Hora:</w:t></w:r></w:p>
                    </w:tc>
                    <w:tc>
                        <w:p><w:r><w:t>${escapeXml(minute.dateTime.ifBlank { "No especificada" })}</w:t></w:r></w:p>
                    </w:tc>
                </w:tr>
                <w:tr>
                    <w:tc>
                        <w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="F1F5F9"/></w:tcPr>
                        <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Lugar / Sede:</w:t></w:r></w:p>
                    </w:tc>
                    <w:tc>
                        <w:p><w:r><w:t>${escapeXml(minute.location.ifBlank { "No especificado" })}</w:t></w:r></w:p>
                    </w:tc>
                </w:tr>
                <w:tr>
                    <w:tc>
                        <w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="F1F5F9"/></w:tcPr>
                        <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Tipo de Reunión:</w:t></w:r></w:p>
                    </w:tc>
                    <w:tc>
                        <w:p><w:r><w:t>${escapeXml(minute.meetingType)}</w:t></w:r></w:p>
                    </w:tc>
                </w:tr>
            </w:tbl>
            """.trimIndent()
        )

        // Spacing
        sb.append("<w:p><w:pPr><w:spacing w:after=\"200\"/></w:pPr></w:p>")

        // Section Helper
        fun appendSectionTitle(title: String) {
            sb.append(
                """
                <w:p>
                    <w:pPr>
                        <w:spacing w:before="240" w:after="120"/>
                        <w:pBdr>
                            <w:bottom w:val="single" w:sz="12" w:space="4" w:color="1E3A8A"/>
                        </w:pBdr>
                    </w:pPr>
                    <w:r>
                        <w:rPr>
                            <w:b/>
                            <w:sz w:val="28"/>
                            <w:color w:val="1E3A8A"/>
                        </w:rPr>
                        <w:t>${escapeXml(title)}</w:t>
                    </w:r>
                </w:p>
                """.trimIndent()
            )
        }

        fun appendParagraphBlock(content: String) {
            val lines = content.ifBlank { "(Sin información)" }.split("\n")
            for (line in lines) {
                sb.append(
                    """
                    <w:p>
                        <w:pPr><w:spacing w:after="100"/></w:pPr>
                        <w:r><w:t>${escapeXml(line)}</w:t></w:r>
                    </w:p>
                    """.trimIndent()
                )
            }
        }

        // 1. Asistentes y Ausentes
        appendSectionTitle("1. LISTA DE ASISTENTES Y AUSENTES")
        sb.append("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t>ASISTENTES:</w:t></w:r></w:p>")
        appendParagraphBlock(minute.attendees)

        sb.append("<w:p><w:pPr><w:spacing w:before=\"120\"/></w:pPr><w:r><w:rPr><w:b/></w:rPr><w:t>AUSENTES:</w:t></w:r></w:p>")
        appendParagraphBlock(minute.absentees)

        // 2. Orden del Día
        appendSectionTitle("2. ORDEN DEL DÍA (PUNTOS A TRATAR)")
        appendParagraphBlock(minute.agenda)

        // 3. Desarrollo y Debate
        appendSectionTitle("3. DESARROLLO Y DEBATE DE LA SESIÓN")
        appendParagraphBlock(minute.discussion)

        // 4. Compromisos y Tareas
        appendSectionTitle("4. COMPROMISOS Y TAREAS")
        if (minute.commitments.isEmpty()) {
            sb.append("<w:p><w:r><w:t>No se registraron compromisos específicos.</w:t></w:r></w:p>")
        } else {
            sb.append(
                """
                <w:tbl>
                    <w:tblPr>
                        <w:tblW w:w="5000" w:type="pct"/>
                        <w:tblBorders>
                            <w:top w:val="single" w:sz="6" w:space="0" w:color="1E3A8A"/>
                            <w:bottom w:val="single" w:sz="6" w:space="0" w:color="1E3A8A"/>
                            <w:left w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                            <w:right w:val="single" w:sz="6" w:space="0" w:color="CBD5E1"/>
                            <w:insideH w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
                            <w:insideV w:val="single" w:sz="4" w:space="0" w:color="E2E8F0"/>
                        </w:tblBorders>
                    </w:tblPr>
                    <w:tr>
                        <w:tc><w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="1E3A8A"/></w:tcPr><w:p><w:r><w:rPr><w:b/><w:color w:val="FFFFFF"/></w:rPr><w:t>Acuerdo / Tarea</w:t></w:r></w:p></w:tc>
                        <w:tc><w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="1E3A8A"/></w:tcPr><w:p><w:r><w:rPr><w:b/><w:color w:val="FFFFFF"/></w:rPr><w:t>Responsable</w:t></w:r></w:p></w:tc>
                        <w:tc><w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="1E3A8A"/></w:tcPr><w:p><w:r><w:rPr><w:b/><w:color w:val="FFFFFF"/></w:rPr><w:t>Fecha Límite</w:t></w:r></w:p></w:tc>
                    </w:tr>
                """.trimIndent()
            )

            for (item in minute.commitments) {
                sb.append(
                    """
                    <w:tr>
                        <w:tc><w:p><w:r><w:t>${escapeXml(item.agreement)}</w:t></w:r></w:p></w:tc>
                        <w:tc><w:p><w:r><w:t>${escapeXml(item.responsible)}</w:t></w:r></w:p></w:tc>
                        <w:tc><w:p><w:r><w:t>${escapeXml(item.dueDate)}</w:t></w:r></w:p></w:tc>
                    </w:tr>
                    """.trimIndent()
                )
            }
            sb.append("</w:tbl>")
        }

        // 5. Firmantes
        appendSectionTitle("5. FIRMANTES DE CONFORMIDAD")
        val secretaryName = minute.secretary.ifBlank { "Secretario/a" }
        val presidentName = minute.president.ifBlank { "Presidente / Pastor" }

        sb.append(
            """
            <w:p><w:pPr><w:spacing w:before="400" w:after="300"/></w:pPr></w:p>
            <w:tbl>
                <w:tblPr>
                    <w:tblW w:w="5000" w:type="pct"/>
                    <w:tblBorders>
                        <w:top w:val="none"/>
                        <w:bottom w:val="none"/>
                        <w:left w:val="none"/>
                        <w:right w:val="none"/>
                        <w:insideH w:val="none"/>
                        <w:insideV w:val="none"/>
                    </w:tblBorders>
                </w:tblPr>
                <w:tr>
                    <w:tc>
                        <w:p>
                            <w:pPr><w:jc w:val="center"/></w:pPr>
                            <w:r><w:t>_____________________________________</w:t></w:r>
                        </w:p>
                        <w:p>
                            <w:pPr><w:jc w:val="center"/></w:pPr>
                            <w:r><w:rPr><w:b/></w:rPr><w:t>${escapeXml(secretaryName)}</w:t></w:r>
                        </w:p>
                        <w:p>
                            <w:pPr><w:jc w:val="center"/></w:pPr>
                            <w:r><w:rPr><w:i/></w:rPr><w:t>Secretario(a) de Reunión</w:t></w:r>
                        </w:p>
                    </w:tc>
                    <w:tc>
                        <w:p>
                            <w:pPr><w:jc w:val="center"/></w:pPr>
                            <w:r><w:t>_____________________________________</w:t></w:r>
                        </w:p>
                        <w:p>
                            <w:pPr><w:jc w:val="center"/></w:pPr>
                            <w:r><w:rPr><w:b/></w:rPr><w:t>${escapeXml(presidentName)}</w:t></w:r>
                        </w:p>
                        <w:p>
                            <w:pPr><w:jc w:val="center"/></w:pPr>
                            <w:r><w:rPr><w:i/></w:rPr><w:t>Presidente / Pastor</w:t></w:r>
                        </w:p>
                    </w:tc>
                </w:tr>
            </w:tbl>
            """.trimIndent()
        )

        sb.append("</w:body>")
        sb.append("</w:document>")

        return sb.toString()
    }
}
