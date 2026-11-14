package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.SamplePlaceholderAiRepository
import com.example.data.parser.DocumentParser
import com.example.model.BookSourceFormat
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ImportRegressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun blankTextFailsAndLongTextRetainsTail() = runBlocking {
        val file = File(context.cacheDir, "test-import.txt")
        file.writeText("   ")
        assertTrue(runCatching { DocumentParser.parseDocumentUri(context, Uri.fromFile(file)) }.isFailure)
        val text = "Metin satırı. ".repeat(2000) + "Son cümle."
        file.writeText(text)
        assertEquals(text, DocumentParser.parseDocumentUri(context, Uri.fromFile(file)).extractedText)
    }

    @Test
    fun epubUsesSpineOrderAndOmitsNavigation() = runBlocking {
        val file = File(context.cacheDir, "ordered.epub")
        ZipOutputStream(file.outputStream()).use { zip ->
            val entries = mapOf(
                "META-INF/container.xml" to """
                    <container><rootfiles><rootfile full-path="OPS/book.opf"/></rootfiles></container>
                """.trimIndent(),
                "OPS/book.opf" to """
                    <package><manifest>
                      <item id="nav" href="toc.xhtml" properties="nav"/>
                      <item id="first" href="z.xhtml"/>
                      <item id="second" href="a.xhtml"/>
                    </manifest><spine><itemref idref="nav"/><itemref idref="first"/>
                      <itemref idref="second"/></spine></package>
                """.trimIndent(),
                "OPS/a.xhtml" to "<p>İkinci bölüm.</p>",
                "OPS/z.xhtml" to "<p>Birinci bölüm.</p>",
                "OPS/toc.xhtml" to "<p>İçindekiler.</p>"
            )
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        val book = DocumentParser.parseDocumentUri(context, Uri.fromFile(file))
        assertEquals("Birinci bölüm.\n\nİkinci bölüm.", book.extractedText)
        assertEquals(2, book.pageCount)
    }

    @Test
    fun pdfExtractsShortRealTextAndRejectsBlankPage() = runBlocking {
        PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "short.pdf")
        PDDocument().use { pdf ->
            val page = PDPage()
            pdf.addPage(page)
            PDPageContentStream(pdf, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 12f)
                content.newLineAtOffset(40f, 40f)
                content.showText("Short but real text.")
                content.endText()
            }
            pdf.save(file)
        }
        assertEquals("Short but real text.", DocumentParser.parseDocumentUri(context, Uri.fromFile(file)).extractedText)
        PDDocument().use { pdf ->
            pdf.addPage(PDPage())
            pdf.save(file)
        }
        val error = runCatching { DocumentParser.parseDocumentUri(context, Uri.fromFile(file)) }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("metin katmanı"))
    }

    @Test
    fun localAnalysisKeepsNarrationAndAllDialogueBeyondNinetyLines() = runBlocking {
        val raw = "İlk anlatım.\n".repeat(91) + "Başlangıç. “Merhaba!” dedi Leyla. “Hoşça kal!” dedi Selim."
        val result = SamplePlaceholderAiRepository()
            .analyzeBookTextAndBuildCast("Roman", raw, BookSourceFormat.TXT)
        assertEquals(
            listOf("Başlangıç.", "Merhaba!", "dedi Leyla.", "Hoşça kal!", "dedi Selim."),
            result.scriptLines.takeLast(5).map { it.text }
        )
    }
}
