package com.example.data.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

enum class PageFilterMode(val labelTr: String, val subtitleTr: String) {
    ORIGINAL("Orijinal", "Ham sayfa görüntüsü"),
    HIGH_CONTRAST("Netleştir", "Metin ve çizgi kontrastını artırır"),
    WARM_ILLUSTRATION("İllüstrasyon", "Sıcak tonları ve sahne detaylarını öne çıkarır"),
    INK_ENGRAVING("Gravür / Mürekkep", "Siyah-beyaz eskiz ve baskı analizi")
}

enum class PageFocusRegion(val labelTr: String) {
    FULL_PAGE("Tam Sayfa"),
    TOP_HALF("Üst Görsel Alanı"),
    CENTER_FOCUS("Merkez Sahne"),
    BOTTOM_HALF("Alt Sayfa & Altyazı")
}

data class PageVisualTelemetry(
    val widthPx: Int,
    val heightPx: Int,
    val brightnessPercent: Int,
    val contrastPercent: Int,
    val warmthLabel: String,
    val dominantColorsHex: List<Long>
)

object BookPageImageProcessor {

    suspend fun processPageBitmap(
        source: Bitmap,
        filterMode: PageFilterMode,
        focusRegion: PageFocusRegion
    ): Pair<Bitmap, PageVisualTelemetry> = withContext(Dispatchers.Default) {
        val cropped = cropToRegion(source, focusRegion)
        val filtered = applyFilter(cropped, filterMode)
        val telemetry = extractVisualTelemetry(filtered)
        filtered to telemetry
    }

    suspend fun renderPdfPageFromUri(
        context: Context,
        uri: Uri,
        pageIndex: Int
    ): Pair<Bitmap, Int>? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount.coerceAtLeast(1)
                    val safePage = pageIndex.coerceIn(0, totalPages - 1)
                    renderer.openPage(safePage).use { page ->
                        val scale = (1600f / maxOf(page.width, page.height)).coerceAtMost(1.5f)
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp to totalPages
                    }
                }
            }
        }.getOrNull()
    }

    private fun cropToRegion(source: Bitmap, region: PageFocusRegion): Bitmap {
        val w = source.width
        val h = source.height
        if (w < 40 || h < 40 || region == PageFocusRegion.FULL_PAGE) return source

        return when (region) {
            PageFocusRegion.FULL_PAGE -> source
            PageFocusRegion.TOP_HALF -> Bitmap.createBitmap(source, 0, 0, w, (h * 0.62f).toInt().coerceAtLeast(1))
            PageFocusRegion.CENTER_FOCUS -> {
                val cropX = (w * 0.08f).toInt()
                val cropY = (h * 0.15f).toInt()
                val cropW = (w * 0.84f).toInt().coerceAtLeast(1)
                val cropH = (h * 0.70f).toInt().coerceAtLeast(1)
                Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)
            }
            PageFocusRegion.BOTTOM_HALF -> {
                val startY = (h * 0.38f).toInt()
                Bitmap.createBitmap(source, 0, startY, w, h - startY)
            }
        }
    }

    private fun applyFilter(source: Bitmap, mode: PageFilterMode): Bitmap {
        if (mode == PageFilterMode.ORIGINAL) return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = ColorMatrix()

        when (mode) {
            PageFilterMode.ORIGINAL -> {}
            PageFilterMode.HIGH_CONTRAST -> {
                val contrast = 1.35f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                matrix.set(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            PageFilterMode.WARM_ILLUSTRATION -> {
                matrix.setScale(1.14f, 1.04f, 0.88f, 1f)
            }
            PageFilterMode.INK_ENGRAVING -> {
                matrix.setSaturation(0f)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    fun extractVisualTelemetry(bitmap: Bitmap): PageVisualTelemetry {
        val w = bitmap.width.coerceAtLeast(1)
        val h = bitmap.height.coerceAtLeast(1)

        var totalLuminance = 0L
        var minLum = 255
        var maxLum = 0
        var totalRed = 0L
        var totalBlue = 0L
        var sampleCount = 0

        val gridSteps = 8
        val swatchColors = mutableListOf<Long>()

        for (gx in 1..gridSteps) {
            for (gy in 1..gridSteps) {
                val px = ((w * gx) / (gridSteps + 1)).coerceIn(0, w - 1)
                val py = ((h * gy) / (gridSteps + 1)).coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(px, py)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()

                totalLuminance += lum
                minLum = min(minLum, lum)
                maxLum = max(maxLum, lum)
                totalRed += r
                totalBlue += b
                sampleCount++

                if (swatchColors.size < 5 && (gx + gy) % 3 == 0) {
                    val argbLong = (0xFFL shl 24) or ((r.toLong() and 0xFF) shl 16) or ((g.toLong() and 0xFF) shl 8) or (b.toLong() and 0xFF)
                    swatchColors.add(argbLong)
                }
            }
        }

        val avgLum = if (sampleCount > 0) (totalLuminance / sampleCount).toInt() else 128
        val brightnessPct = ((avgLum / 255f) * 100f).toInt().coerceIn(1, 100)
        val contrastPct = (((maxLum - minLum) / 255f) * 100f).toInt().coerceIn(5, 100)
        val warmth = when {
            totalRed > totalBlue * 1.15 -> "Sıcak / Kehribar Tonlu"
            totalBlue > totalRed * 1.12 -> "Soğuk / Gece Tonlu"
            else -> "Dengeli / Nötr Işık"
        }

        return PageVisualTelemetry(
            widthPx = w,
            heightPx = h,
            brightnessPercent = brightnessPct,
            contrastPercent = contrastPct,
            warmthLabel = warmth,
            dominantColorsHex = swatchColors.ifEmpty { listOf(0xFFF4A261, 0xFF2A9D8F, 0xFFE76F51) }
        )
    }
}
