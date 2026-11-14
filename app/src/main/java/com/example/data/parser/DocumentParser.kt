package com.example.data.parser

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.text.Html
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.example.model.BookSourceFormat
import com.example.model.NarrationLanguage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import java.io.InputStream
import java.io.Writer
import java.net.URI
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ParsedBookDocument(
    val fileName: String,
    val fileSizeBytes: Long,
    val title: String,
    val author: String,
    val format: BookSourceFormat,
    val language: NarrationLanguage = NarrationLanguage.TURKISH,
    val extractedText: String,
    val pageCount: Int = 1,
    val previewBitmap: Bitmap? = null
) {
    val formattedFileSize: String
        get() = when {
            fileSizeBytes >= 1_048_576L -> String.format("%.2f MB", fileSizeBytes / 1_048_576f)
            fileSizeBytes >= 1024L -> String.format("%d KB", fileSizeBytes / 1024L)
            else -> "$fileSizeBytes B"
        }
}

object DocumentParser {
    private const val MAX_FILE_BYTES = 32 * 1024 * 1024
    internal const val MAX_TEXT_CHARS = 2_000_000
    private const val MAX_EPUB_ENTRIES = 4096

    private fun checkTextLength(length: Int) {
        require(length <= MAX_TEXT_CHARS) {
            "Metin 2 milyon karakteri aşıyor. Kitabı bölümlere ayırıp yeniden seçin."
        }
    }

    private fun input(context: Context, uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri)
            ?: error("Seçilen dosya açılamadı.")

    private suspend fun copyBounded(input: InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            coroutineContext.ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_FILE_BYTES) { "Dosya 32 MB sınırını aşıyor." }
            output.write(buffer, 0, count)
        }
    }

    suspend fun parseDocumentUri(
        context: Context,
        uri: Uri,
        language: NarrationLanguage = NarrationLanguage.TURKISH
    ): ParsedBookDocument = withContext(Dispatchers.IO) {
        val (fileName, fileSize) = resolveFileMeta(context, uri)
        require(fileSize <= MAX_FILE_BYTES) { "Dosya 32 MB sınırını aşıyor." }
        val mimeType = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        val lowerName = fileName.lowercase()

        val cleanTitle = fileName
            .substringBeforeLast(".")
            .replace("_", " ")
            .replace("-", " ")
            .trim()
            .ifBlank { "Yüklenen Roman (${language.badgeShort})" }

        when {
            lowerName.endsWith(".epub") || mimeType.contains("epub") -> {
                parseEpub(context, uri, fileName, fileSize, cleanTitle, language)
            }
            lowerName.endsWith(".pdf") || mimeType.contains("pdf") -> {
                parsePdf(context, uri, fileName, fileSize, cleanTitle, language)
            }
            lowerName.endsWith(".txt") || mimeType == "text/plain" -> {
                parsePlainText(context, uri, fileName, fileSize, cleanTitle, language)
            }
            else -> error("Desteklenen dosya türleri PDF, EPUB ve TXT.")
        }
    }

    /**
     * Generates a real, standards-compliant PDF or EPUB file in cache storage in the selected language
     * (TR, EN, FR, IT, ES) and parses it so users can test multi-voice emotional narration in any language.
     */
    suspend fun createAndParseSampleFile(
        context: Context,
        format: BookSourceFormat,
        language: NarrationLanguage = NarrationLanguage.TURKISH
    ): ParsedBookDocument = withContext(Dispatchers.IO) {
        val (sampleTitle, sampleAuthor, sampleText) = getSampleBookContentForLanguage(language, format)

        when (format) {
            BookSourceFormat.PDF -> {
                val pdfFile = File(context.cacheDir, "Sample_${language.code.uppercase()}_Novel.pdf")
                val pdfDoc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.parseColor("#FDFBF7"))

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1D1528")
                    textSize = 20f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#2D243B")
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                }

                canvas.drawText("$sampleTitle (${language.labelTr} PDF)", 44f, 64f, titlePaint)
                var y = 110f
                sampleText.lines().forEach { line ->
                    val words = line.split(" ")
                    var currentLine = ""
                    for (w in words) {
                        if ((currentLine + " " + w).length > 68) {
                            canvas.drawText(currentLine.trim(), 44f, y, bodyPaint)
                            y += 22f
                            currentLine = w
                        } else {
                            currentLine = "$currentLine $w"
                        }
                    }
                    if (currentLine.isNotBlank()) {
                        canvas.drawText(currentLine.trim(), 44f, y, bodyPaint)
                        y += 28f
                    }
                }
                pdfDoc.finishPage(page)
                FileOutputStream(pdfFile).use { out -> pdfDoc.writeTo(out) }
                pdfDoc.close()

                var previewBmp: Bitmap? = null
                runCatching {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            renderer.openPage(0).use { p ->
                                val bmp = Bitmap.createBitmap(420, 594, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                p.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                previewBmp = bmp
                            }
                        }
                    }
                }

                ParsedBookDocument(
                    fileName = pdfFile.name,
                    fileSizeBytes = pdfFile.length(),
                    title = sampleTitle,
                    author = sampleAuthor,
                    format = BookSourceFormat.PDF,
                    language = language,
                    extractedText = sampleText,
                    pageCount = 1,
                    previewBitmap = previewBmp
                )
            }

            else -> {
                val epubFile = File(context.cacheDir, "Sample_${language.code.uppercase()}_Edition.epub")
                val paragraphsHtml = sampleText.lines()
                    .filter { it.isNotBlank() }
                    .joinToString("\n") { "<p>$it</p>" }

                val xhtml = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <html xmlns="http://www.w3.org/1999/xhtml">
                    <head><title>$sampleTitle</title></head>
                    <body>
                      <h1>$sampleTitle</h1>
                      $paragraphsHtml
                    </body>
                    </html>
                """.trimIndent()

                val opf = """
                    <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="book-id">
                      <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                        <dc:identifier id="book-id">urn:romanses:sample:${language.code}</dc:identifier>
                        <dc:title>$sampleTitle</dc:title>
                        <dc:creator>$sampleAuthor</dc:creator>
                        <dc:language>${language.code}</dc:language>
                        <meta property="dcterms:modified">2026-11-13T00:00:00Z</meta>
                      </metadata>
                      <manifest>
                        <item id="chapter" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                        <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
                      </manifest>
                      <spine><itemref idref="chapter"/></spine>
                    </package>
                """.trimIndent()

                val container = """
                    <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                      <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
                    </container>
                """.trimIndent()
                val nav = """
                    <html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
                      <head><title>İçindekiler</title></head>
                      <body><nav epub:type="toc"><ol><li><a href="chapter1.xhtml">$sampleTitle</a></li></ol></nav></body>
                    </html>
                """.trimIndent()
                ZipOutputStream(FileOutputStream(epubFile)).use { zip ->
                    val mimetype = "application/epub+zip".toByteArray(StandardCharsets.UTF_8)
                    zip.putNextEntry(ZipEntry("mimetype").apply {
                        method = ZipEntry.STORED
                        size = mimetype.size.toLong()
                        crc = java.util.zip.CRC32().apply { update(mimetype) }.value
                    })
                    zip.write(mimetype)
                    zip.closeEntry()
                    for ((name, value) in mapOf(
                        "META-INF/container.xml" to container,
                        "OEBPS/content.opf" to opf,
                        "OEBPS/chapter1.xhtml" to xhtml,
                        "OEBPS/nav.xhtml" to nav
                    )) {
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(value.toByteArray(StandardCharsets.UTF_8))
                        zip.closeEntry()
                    }
                }

                parseEpub(context, Uri.fromFile(epubFile), epubFile.name, epubFile.length(), sampleTitle, language)

            }
        }
    }

    private fun getSampleBookContentForLanguage(
        language: NarrationLanguage,
        format: BookSourceFormat
    ): Triple<String, String, String> {
        return when (language) {
            NarrationLanguage.TURKISH -> if (format == BookSourceFormat.PDF) {
                Triple(
                    "Suç ve Ceza • İtiraf Sahnesi",
                    "Fyodor Dostoyevski",
                    """
                    Tavan arasındaki küçük odada sarı lambanın ışığı titriyordu.
                    "Bütün bunlara artık nasıl dayanacağım Sonya!" diye bağırdı Raskolnikov, büyük bir öfke ve çaresizlikle.
                    Sonya yavaşça ayağa kalktı, gözlerinde derin bir merhamet ve hüzün vardı.
                    "Acı çekmekten korkma Rodion, gerçeği itiraf ettiğinde ruhun yeniden nefes alacak," diye fısıldadı Sonya, sakin ama titrek bir sesle.
                    Kapının önünde bekleyen küçük Polya merakla içeri baktı.
                    "Abla, dışarıda yağmur dindi, gökyüzünde yıldızlar çıktı!" dedi Polya, çocuksu bir neşeyle.
                    "Gerçekten bir umut kalmış olabilir mi?" diye sordu Raskolnikov, şaşkınlık ve hayretle.
                    """.trimIndent()
                )
            } else {
                Triple(
                    "Çalıkuşu • Zeyniler Günlüğü",
                    "Reşat Nuri Güntekin",
                    """
                    Kar fırtınası okulun tahta pencerelerini sarsarken Feride elindeki lambayı masaya bıraktı.
                    "Bu ıssız köyde tek başıma nasıl öğretmenlik yapacağım Kamran?" diye fısıldadı Feride, hüzünlü ve kırgın bir sesle.
                    Kapı yavaşça açıldı ve küçük Munise çekingen adımlarla içeri girdi.
                    "Öğretmen hanım, üşüdüyseniz size sıcak ıhlamur getirdim!" dedi Munise, neşeli ve içten bir çocuk sesiyle.
                    İhtiyar Muhtar kapının eşiğinden başını uzattı.
                    "Korkma hocanım! Bu köyün insanı seni kendi evladı gibi korur!" diye seslendi Muhtar, tok ve güven veren bir sesle.
                    """.trimIndent()
                )
            }

            NarrationLanguage.ENGLISH -> Triple(
                "Pride and Prejudice • Evening Confrontation",
                "Jane Austen",
                """
                The candles flickered softly inside the grand drawing room as rain tapped against the tall windows.
                "How could you conceal the truth from my sister for so many months?!" shouted Elizabeth, her voice trembling with anger.
                Darcy stepped forward into the warm light, his expression filled with deep sorrow and regret.
                "I acted out of pride and fear, Elizabeth, yet every word I wrote in this letter is the honest secret of my heart," whispered Darcy softly.
                Young Clara peeked through the doorway with a bright smile.
                "Sister! Look at the garden, the storm has passed and the morning sun is shining!" said Clara with joyful delight.
                "Can it truly be that our misunderstandings are finally over?!" asked Elizabeth in astonishment and surprise.
                """.trimIndent()
            )

            NarrationLanguage.FRENCH -> Triple(
                "Les Misérables • La Promesse de la Nuit",
                "Victor Hugo",
                """
                Une brume froide enveloppait les ruelles silencieuses de Montfermeil sous la lueur pâle des lanternes.
                "Pourquoi avez-vous attendu si longtemps avant de venir me sauver ?!" cria Fantine avec colère et désespoir.
                Valjean posa doucement sa main sur l'épaule de la malheureuse mère avec une profonde tristesse.
                "Écoutez ce secret à voix basse : je protégerai votre enfant contre toutes les ombres du monde," chuchota Valjean avec calme.
                La petite Cosette courut vers la porte en riant de bonheur.
                "Regardez cette magnifique poupée dans la vitrine, comme je suis heureuse !" dit Cosette avec une joie enfantine.
                "C'est incroyable ! Est-ce vraiment un miracle qui commence ce soir ?!" demanda Fantine, surprise et émue.
                """.trimIndent()
            )

            NarrationLanguage.ITALIAN -> Triple(
                "I Promessi Sposi • Notte sul Lago di Como",
                "Alessandro Manzoni",
                """
                Le onde del lago brillavano sotto la luna mentre la piccola barca si allontanava silenziosamente dalla riva.
                "Come possiamo fuggire dalla rabbia di Don Rodrigo senza perdere la nostra casa?!" gridò Renzo, furioso e agitato.
                Lucia guardò le montagne lontane con gli occhi pieni di lacrime e malinconia.
                "Ascolta questo segreto nel silenzio del cuore: la speranza non ci abbandonerà mai," sussurrò Lucia con dolcezza.
                Il piccolo Marco indicò il cielo stellato con un grande sorriso.
                "Guardate laggiù, le campane del villaggio suonano a festa per noi!" disse Marco con gioia e felicità.
                "Incredibile! Siamo davvero arrivati in salvo sull'altra sponda?!" chiese Renzo con grande sorpresa.
                """.trimIndent()
            )

            NarrationLanguage.SPANISH -> Triple(
                "Don Quijote • El Secreto de los Molinos",
                "Miguel de Cervantes",
                """
                El viento de la llanura movía las enormes aspas blancas bajo el cielo dorado del atardecer en La Mancha.
                "¡Esos no son molinos de viento, amigo Sancho, sino gigantes furiosos que desafían nuestro honor!" gritó Quijote con ira y pasión dramática.
                Dulcinea contemplaba el horizonte desde el balcón con lágrimas de profunda tristeza.
                "Escucha este secreto en voz baja: mi corazón teme que la tormenta de la noche los alcance," susurró Dulcinea con miedo y melancolía.
                La pequeña Lucía corrió por el sendero sonriendo con entusiasmo.
                "¡Miren qué alegría, los músicos han llegado a la plaza del pueblo!" dijo Lucía muy feliz.
                "¡No puedo creerlo! ¿Acaso hemos encontrado el castillo encantado?!" preguntó Sancho sorprendido.
                """.trimIndent()
            )
        }
    }

    private fun resolveFileMeta(context: Context, uri: Uri): Pair<String, Long> {
        var name: String? = null
        var size = 0L
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) name = it.getString(nameIdx)
                    val sizeIdx = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !it.isNull(sizeIdx)) size = it.getLong(sizeIdx)
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/') ?: "Yuklenen_Kitap.pdf"
        }
        return name!! to size.coerceAtLeast(1024L)
    }

    private suspend fun parseEpub(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        fallbackTitle: String,
        language: NarrationLanguage
    ): ParsedBookDocument {
        val entries = mutableMapOf<String, ByteArray>()
        var expandedBytes = 0
        var entryCount = 0
        val buffer = ByteArray(8192)
        ZipInputStream(input(context, uri)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(++entryCount <= MAX_EPUB_ENTRIES) { "EPUB çok fazla dosya içeriyor." }
                val keep = !entry.isDirectory && entry.name.substringAfterLast('.').lowercase() in
                    setOf("xml", "opf", "xhtml", "html", "htm")
                val out = if (keep) ByteArrayOutputStream() else null
                while (true) {
                    coroutineContext.ensureActive()
                    val count = zip.read(buffer)
                    if (count < 0) break
                    expandedBytes += count
                    require(expandedBytes <= MAX_FILE_BYTES) { "Açılan EPUB verisi 32 MB sınırını aşıyor." }
                    out?.write(buffer, 0, count)
                }
                if (out != null) entries[entry.name] = out.toByteArray()
            }
        }
        val container = parsePackageXml(entries["META-INF/container.xml"] ?: error("EPUB kapsayıcısı eksik."))
        val packagePath = container.firstOrNull { it.name == "rootfile" }?.attributes?.get("full-path")
            ?: error("EPUB paket yolu bulunamadı.")
        val pkg = parsePackageXml(entries[packagePath] ?: error("EPUB paket dosyası bulunamadı."))
        val manifest = pkg.filter { it.name == "item" }.associateBy { it.attributes["id"] }
        val spine = pkg.filter { it.name == "itemref" && it.attributes["linear"] != "no" }
        val text = StringBuilder()
        var chapters = 0
        for (reference in spine) {
            coroutineContext.ensureActive()
            val item = manifest[reference.attributes["idref"]] ?: error("EPUB bölüm kaydı eksik.")
            if (item.attributes["properties"].orEmpty().split(' ').contains("nav")) continue
            val href = URI(item.attributes["href"] ?: error("EPUB bölüm yolu eksik."))
            require(!href.isAbsolute && href.rawAuthority == null) { "EPUB bölüm bağlantısı geçersiz." }
            val path = URI(null, null, "/$packagePath", null).resolve(href).normalize().path.removePrefix("/")
            val bytes = entries[path] ?: error("EPUB bölüm metni bulunamadı.")
            val chapter = htmlToStructuredPlainText(bytes.toString(StandardCharsets.UTF_8))
            if (chapter.isNotBlank()) {
                checkTextLength(text.length + chapter.length + 2)
                if (text.isNotEmpty()) text.append("\n\n")
                text.append(chapter)
                chapters++
            }
        }
        require(text.isNotBlank()) { "EPUB içinde okunabilir metin bulunamadı." }
        return ParsedBookDocument(
            fileName, fileSize, pkg.firstOrNull { it.name == "title" }?.text?.takeIf { it.isNotBlank() } ?: fallbackTitle,
            pkg.firstOrNull { it.name == "creator" }?.text?.takeIf { it.isNotBlank() } ?: "Bilinmeyen yazar",
            BookSourceFormat.EPUB, language, text.toString(), chapters
        )
    }

    private data class PackageElement(val name: String, val attributes: Map<String, String>, val text: String)

    private fun parsePackageXml(bytes: ByteArray): List<PackageElement> {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
            setInput(bytes.inputStream(), null)
        }
        val elements = mutableListOf<PackageElement>()
        val stack = ArrayDeque<Pair<PackageElement, StringBuilder>>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.DOCDECL -> error("EPUB paketinde DTD desteklenmiyor.")
                XmlPullParser.START_TAG -> {
                    require(stack.size < 64) { "EPUB paket yapısı çok derin." }
                    val attributes = (0 until parser.attributeCount).associate {
                        parser.getAttributeName(it) to parser.getAttributeValue(it)
                    }
                    stack.addLast(PackageElement(parser.name, attributes, "") to StringBuilder())
                }
                XmlPullParser.TEXT, XmlPullParser.CDSECT, XmlPullParser.ENTITY_REF -> {
                    stack.lastOrNull()?.second?.append(parser.text.orEmpty())
                }
                XmlPullParser.END_TAG -> {
                    val (element, text) = stack.removeLast()
                    elements.add(element.copy(text = text.toString().trim()))
                }
            }
            parser.nextToken()
        }
        return elements
    }

    private suspend fun parsePdf(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        fallbackTitle: String,
        language: NarrationLanguage
    ): ParsedBookDocument {
        PDFBoxResourceLoader.init(context.applicationContext)
        val temporaryPdf = File.createTempFile("import-", ".pdf", context.cacheDir)
        try {
            input(context, uri).use { source ->
                temporaryPdf.outputStream().use { copyBounded(source, it) }
            }
            val result = PDDocument.load(
                temporaryPdf,
                MemoryUsageSetting.setupTempFileOnly(64L * 1024 * 1024).setTempDir(context.cacheDir)
            ).use { pdf ->
                require(pdf.currentAccessPermission.canExtractContent()) {
                    "Bu PDF metin çıkarmaya izin vermiyor."
                }
                val text = StringBuilder()
                val importContext = coroutineContext
                val writer = object : Writer() {
                    override fun write(chars: CharArray, offset: Int, length: Int) {
                        importContext.ensureActive()
                        checkTextLength(text.length + length)
                        text.append(chars, offset, length)
                    }
                    override fun flush() {}
                    override fun close() {}
                }
                PDFTextStripper().apply { sortByPosition = true }.writeText(pdf, writer)
                require(text.isNotBlank()) {
                    "PDF metin katmanı bulunamadı. Sayfa Analizi’nde metni cihazda çıkarıp kontrol edebilirsiniz."
                }
                ParsedBookDocument(
                    fileName, fileSize, pdf.documentInformation.title?.takeIf { it.isNotBlank() } ?: fallbackTitle,
                    pdf.documentInformation.author?.takeIf { it.isNotBlank() } ?: "Bilinmeyen yazar",
                    BookSourceFormat.PDF, language, text.toString().trim(), pdf.numberOfPages
                )
            }
            return result.copy(
                previewBitmap = BookPageImageProcessor.renderPdfPageFromUri(context, Uri.fromFile(temporaryPdf), 0)?.first
            )
        } finally {
            temporaryPdf.delete()
        }
    }

    private suspend fun parsePlainText(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        fallbackTitle: String,
        language: NarrationLanguage
    ): ParsedBookDocument {
        val text = StringBuilder()
        val decoder = StandardCharsets.UTF_8.newDecoder()
        InputStreamReader(input(context, uri), decoder).use { reader ->
            val buffer = CharArray(8192)
            while (true) {
                coroutineContext.ensureActive()
                val count = reader.read(buffer)
                if (count < 0) break
                checkTextLength(text.length + count)
                text.append(buffer, 0, count)
            }
        }
        val content = text.toString().removePrefix("\uFEFF").trim()
        require(content.isNotBlank()) { "TXT dosyasında okunabilir metin bulunamadı." }
        return ParsedBookDocument(
            fileName, fileSize, fallbackTitle, "Bilinmeyen yazar", BookSourceFormat.TXT,
            language, content, ((content.length + 1799) / 1800).coerceAtLeast(1)
        )
    }

    private fun htmlToStructuredPlainText(html: String): String {
        val body = html.replace(Regex("<(head|style|script)\\b[^>]*>.*?</\\1>", setOf(
            RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL
        )), "")
        return Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }
}
