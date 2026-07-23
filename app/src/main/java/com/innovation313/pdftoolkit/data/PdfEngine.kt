package com.innovation313.pdftoolkit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Rendering fidelity used when re-drawing existing PDF pages into a new document.
 * Since PdfRenderer only exposes rasterized page content (no vector copy API is
 * available in the Android framework), merge/split output is image-based —
 * visually faithful to the original but not text-selectable. This is a documented
 * trade-off of the no-external-library approach; see README.
 */
enum class RenderQuality(val scale: Float) {
    COMPRESSED(1.0f),   // ~72dpi — smallest file size
    STANDARD(2.0f),     // ~144dpi — good screen/print balance (default)
    HIGH(3.0f)          // ~216dpi — closer to original fidelity, larger file
}

data class PdfOperationResult(val success: Boolean, val pageCount: Int = 0, val errorMessage: String? = null)

object PdfEngine {

    suspend fun mergePdfs(
        context: Context,
        sourceUris: List<Uri>,
        outputUri: Uri,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var totalPages = 0

            for (sourceUri in sourceUris) {
                context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { page ->
                                totalPages++
                                addRenderedPage(document, page, totalPages, quality)
                            }
                        }
                    }
                }
            }

            writeDocument(context, document, outputUri)
            document.close()
            PdfOperationResult(success = true, pageCount = totalPages)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Unknown error during merge")
        }
    }

    suspend fun splitPdf(
        context: Context,
        sourceUri: Uri,
        fromPage: Int, // 1-indexed, inclusive
        toPage: Int,   // 1-indexed, inclusive
        outputUri: Uri,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var pagesWritten = 0

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val start = (fromPage - 1).coerceIn(0, renderer.pageCount - 1)
                    val end = (toPage - 1).coerceIn(0, renderer.pageCount - 1)
                    if (start > end) {
                        return@withContext PdfOperationResult(
                            success = false,
                            errorMessage = "Invalid page range"
                        )
                    }
                    for (i in start..end) {
                        renderer.openPage(i).use { page ->
                            pagesWritten++
                            addRenderedPage(document, page, pagesWritten, quality)
                        }
                    }
                }
            }

            writeDocument(context, document, outputUri)
            document.close()
            PdfOperationResult(success = true, pageCount = pagesWritten)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Unknown error during split")
        }
    }

    suspend fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        outputUri: Uri
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            imageUris.forEachIndexed { index, uri ->
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input) ?: return@forEachIndexed
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                    bitmap.recycle()
                }
            }

            writeDocument(context, document, outputUri)
            document.close()
            PdfOperationResult(success = true, pageCount = imageUris.size)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Unknown error converting images")
        }
    }

    suspend fun compressPdf(
        context: Context,
        sourceUri: Uri,
        outputUri: Uri,
        quality: RenderQuality
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var pageCount = 0

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            pageCount++
                            addRenderedPage(document, page, pageCount, quality)
                        }
                    }
                }
            }

            writeDocument(context, document, outputUri)
            document.close()
            PdfOperationResult(success = true, pageCount = pageCount)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Unknown error during compression")
        }
    }

    /** Returns the page count of a PDF without loading/rendering any page content — used for quick preview info. */
    suspend fun getPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer -> renderer.pageCount }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun addRenderedPage(document: PdfDocument, page: PdfRenderer.Page, pageNumber: Int, quality: RenderQuality) {
        val width = (page.width * quality.scale).toInt().coerceAtLeast(1)
        val height = (page.height * quality.scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        val matrix = Matrix().apply { setScale(quality.scale, quality.scale) }
        page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

        val pdfPageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
        val pdfPage = document.startPage(pdfPageInfo)
        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(pdfPage)
        bitmap.recycle()
    }

    private fun writeDocument(context: Context, document: PdfDocument, outputUri: Uri) {
        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            document.writeTo(out)
        } ?: throw IllegalStateException("Could not open output stream for the destination file")
    }
}
