package com.innovation313.pdftoolkit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class PageFilter { NONE, GRAYSCALE, BLACK_WHITE, ENHANCE }

enum class SplitMode { RANGE, EVERY_PAGE, EVERY_N_PAGES }

object AdvancedTools {

    // ---------- Filters ----------

    /** Re-renders each page through a colour filter (grayscale / high-contrast B&W / enhance). */
    suspend fun applyFilter(
        context: Context,
        sourceUri: Uri,
        outputUri: Uri,
        filter: PageFilter,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var written = 0
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = buildColorFilter(filter)
            }

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val w = (page.width * quality.scale).toInt().coerceAtLeast(1)
                            val h = (page.height * quality.scale).toInt().coerceAtLeast(1)
                            val src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            src.eraseColor(Color.WHITE)
                            page.render(src, null, Matrix().apply { setScale(quality.scale, quality.scale) },
                                PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            written++
                            val info = PdfDocument.PageInfo.Builder(w, h, written).create()
                            val pdfPage = document.startPage(info)
                            pdfPage.canvas.drawBitmap(src, 0f, 0f, paint)
                            document.finishPage(pdfPage)
                            src.recycle()
                        }
                    }
                }
            }
            context.contentResolver.openOutputStream(outputUri)?.use { document.writeTo(it) }
                ?: throw IllegalStateException("Could not open output stream")
            document.close()
            PdfOperationResult(success = true, pageCount = written)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Filter failed")
        }
    }

    private fun buildColorFilter(filter: PageFilter): ColorMatrixColorFilter? = when (filter) {
        PageFilter.NONE -> null
        PageFilter.GRAYSCALE -> ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        PageFilter.BLACK_WHITE -> {
            // Desaturate, then push contrast hard so mid-tones collapse to black or white.
            val gray = ColorMatrix().apply { setSaturation(0f) }
            val contrast = 12f
            val translate = (-0.5f * contrast + 0.5f) * 255f
            val hard = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            gray.postConcat(hard)
            ColorMatrixColorFilter(gray)
        }
        PageFilter.ENHANCE -> {
            // Moderate contrast lift with a slight brightness boost — closer to a scanner "clean up".
            val contrast = 1.5f
            val translate = (-0.5f * contrast + 0.5f) * 255f + 10f
            ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )))
        }
    }

    // ---------- Target-size compression ----------

    /**
     * Renders repeatedly at decreasing resolution until the output fits under [targetBytes].
     * Writes each attempt to a cache file first so the user's destination is only touched once.
     * If even the lowest setting misses the target, the smallest result is still saved and
     * [PdfOperationResult.errorMessage] stays null — the UI reports the achieved size.
     */
    suspend fun compressToTargetSize(
        context: Context,
        sourceUri: Uri,
        outputUri: Uri,
        targetBytes: Long,
        onProgress: (Int) -> Unit = {}
    ): CompressResult = withContext(Dispatchers.IO) {
        val scales = listOf(1.5f, 1.2f, 1.0f, 0.8f, 0.65f, 0.5f, 0.4f, 0.3f)
        var bestFile: File? = null
        try {
            for ((index, scale) in scales.withIndex()) {
                onProgress(index + 1)
                val temp = File(context.cacheDir, "compress_attempt.pdf")
                if (temp.exists()) temp.delete()
                renderAtScale(context, sourceUri, temp, scale)
                bestFile?.delete()
                bestFile = temp.copyTo(File(context.cacheDir, "compress_best.pdf"), overwrite = true)
                temp.delete()
                if (bestFile.length() <= targetBytes) break
            }

            val finalFile = bestFile ?: return@withContext CompressResult(false, 0L, "Compression failed")
            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                finalFile.inputStream().use { it.copyTo(out) }
            } ?: throw IllegalStateException("Could not open output stream")
            val size = finalFile.length()
            finalFile.delete()
            CompressResult(success = true, finalSizeBytes = size)
        } catch (e: Exception) {
            bestFile?.delete()
            CompressResult(false, 0L, e.message ?: "Compression failed")
        }
    }

    private fun renderAtScale(context: Context, sourceUri: Uri, dest: File, scale: Float) {
        val document = PdfDocument()
        context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val w = (page.width * scale).toInt().coerceAtLeast(1)
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, Matrix().apply { setScale(scale, scale) },
                            PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        val info = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                        val pdfPage = document.startPage(info)
                        pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                        document.finishPage(pdfPage)
                        bmp.recycle()
                    }
                }
            }
        }
        dest.outputStream().use { document.writeTo(it) }
        document.close()
    }

    // ---------- Insert pages ----------

    /** Inserts every page of [insertUri] into [baseUri] after 1-indexed page [afterPage] (0 = at the start). */
    suspend fun insertPages(
        context: Context,
        baseUri: Uri,
        insertUri: Uri,
        afterPage: Int,
        outputUri: Uri,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var written = 0

            fun appendAll(uri: Uri, from: Int, to: Int) {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val start = from.coerceAtLeast(0)
                        val end = to.coerceAtMost(renderer.pageCount - 1)
                        for (i in start..end) {
                            renderer.openPage(i).use { page ->
                                val w = (page.width * quality.scale).toInt().coerceAtLeast(1)
                                val h = (page.height * quality.scale).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, Matrix().apply { setScale(quality.scale, quality.scale) },
                                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                written++
                                val info = PdfDocument.PageInfo.Builder(w, h, written).create()
                                val pdfPage = document.startPage(info)
                                pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                                document.finishPage(pdfPage)
                                bmp.recycle()
                            }
                        }
                    }
                }
            }

            val basePageCount = PdfEngine.getPageCount(context, baseUri)
            val cut = afterPage.coerceIn(0, basePageCount)
            if (cut > 0) appendAll(baseUri, 0, cut - 1)
            appendAll(insertUri, 0, Int.MAX_VALUE - 1)
            if (cut < basePageCount) appendAll(baseUri, cut, Int.MAX_VALUE - 1)

            context.contentResolver.openOutputStream(outputUri)?.use { document.writeTo(it) }
                ?: throw IllegalStateException("Could not open output stream")
            document.close()
            PdfOperationResult(success = true, pageCount = written)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Insert failed")
        }
    }

    // ---------- Multi-file split ----------

    /** Splits a PDF into several files inside the user-picked folder ([treeUri]). */
    suspend fun splitIntoFiles(
        context: Context,
        sourceUri: Uri,
        treeUri: Uri,
        mode: SplitMode,
        pagesPerFile: Int = 1,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri, DocumentsContract.getTreeDocumentId(treeUri)
            )
            val chunk = if (mode == SplitMode.EVERY_PAGE) 1 else pagesPerFile.coerceAtLeast(1)
            var filesWritten = 0

            resolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    var start = 0
                    while (start < total) {
                        val end = (start + chunk - 1).coerceAtMost(total - 1)
                        val document = PdfDocument()
                        var pageNo = 0
                        for (i in start..end) {
                            renderer.openPage(i).use { page ->
                                val w = (page.width * quality.scale).toInt().coerceAtLeast(1)
                                val h = (page.height * quality.scale).toInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, Matrix().apply { setScale(quality.scale, quality.scale) },
                                    PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                pageNo++
                                val info = PdfDocument.PageInfo.Builder(w, h, pageNo).create()
                                val pdfPage = document.startPage(info)
                                pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                                document.finishPage(pdfPage)
                                bmp.recycle()
                            }
                        }
                        val name = if (chunk == 1) "page_${start + 1}.pdf" else "part_${start + 1}-${end + 1}.pdf"
                        val fileUri = DocumentsContract.createDocument(resolver, dirUri, "application/pdf", name)
                        if (fileUri != null) {
                            resolver.openOutputStream(fileUri)?.use { document.writeTo(it) }
                            filesWritten++
                        }
                        document.close()
                        start = end + 1
                    }
                }
            }
            PdfOperationResult(success = true, pageCount = filesWritten)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Split failed")
        }
    }

    // ---------- ID card layout ----------

    /**
     * Places a front and (optional) back image stacked on a single A4-proportioned page —
     * the usual layout for copying a CNIC or any ID card.
     */
    suspend fun makeIdCardPdf(
        context: Context,
        frontUri: Uri,
        backUri: Uri?,
        outputUri: Uri
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            // A4 at ~150dpi
            val pageWidth = 1240
            val pageHeight = 1754
            val document = PdfDocument()
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(info)
            val canvas: Canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val margin = pageWidth * 0.08f
            val slotWidth = pageWidth - margin * 2
            val slotHeight = (pageHeight - margin * 3) / 2

            fun drawImage(uri: Uri, top: Float) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input) ?: return
                    val ratio = minOf(slotWidth / bmp.width, slotHeight / bmp.height)
                    val w = (bmp.width * ratio).toInt().coerceAtLeast(1)
                    val h = (bmp.height * ratio).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
                    val x = margin + (slotWidth - w) / 2f
                    canvas.drawBitmap(scaled, x, top, null)
                    if (scaled != bmp) scaled.recycle()
                    bmp.recycle()
                }
            }

            drawImage(frontUri, margin)
            if (backUri != null) drawImage(backUri, margin * 2 + slotHeight)

            document.finishPage(page)
            context.contentResolver.openOutputStream(outputUri)?.use { document.writeTo(it) }
                ?: throw IllegalStateException("Could not open output stream")
            document.close()
            PdfOperationResult(success = true, pageCount = 1)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "ID card layout failed")
        }
    }
}

data class CompressResult(val success: Boolean, val finalSizeBytes: Long, val errorMessage: String? = null)
