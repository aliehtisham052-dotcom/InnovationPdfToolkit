package com.innovation313.pdftoolkit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class PageItem(val originalIndex: Int, val rotationDegrees: Int = 0)

object PageManager {

    /**
     * Streams one rendered bitmap per page so the UI can display pages progressively
     * as they become ready instead of blocking until the whole document is rendered.
     */
    fun pageBitmapFlow(context: Context, uri: Uri, scale: Float): Flow<Pair<Int, Bitmap>> = flow {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    val bitmap = renderer.openPage(i).use { page ->
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        val matrix = Matrix().apply { setScale(scale, scale) }
                        page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                    emit(i to bitmap)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Rebuilds the PDF using the given page list — order defines the new page order,
     * missing indices are dropped (deleted), and per-page rotation is applied.
     */
    suspend fun rebuildPdf(
        context: Context,
        sourceUri: Uri,
        pages: List<PageItem>,
        outputUri: Uri,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            if (pages.isEmpty()) {
                return@withContext PdfOperationResult(success = false, errorMessage = "No pages to save")
            }
            val document = PdfDocument()
            var written = 0
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (item in pages) {
                        if (item.originalIndex < 0 || item.originalIndex >= renderer.pageCount) continue
                        renderer.openPage(item.originalIndex).use { page ->
                            val width = (page.width * quality.scale).toInt().coerceAtLeast(1)
                            val height = (page.height * quality.scale).toInt().coerceAtLeast(1)
                            var bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            val matrix = Matrix().apply { setScale(quality.scale, quality.scale) }
                            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            if (item.rotationDegrees % 360 != 0) {
                                val rot = Matrix().apply { postRotate(item.rotationDegrees.toFloat()) }
                                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, rot, true)
                                bmp.recycle()
                                bmp = rotated
                            }

                            written++
                            val info = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, written).create()
                            val pdfPage = document.startPage(info)
                            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                            document.finishPage(pdfPage)
                            bmp.recycle()
                        }
                    }
                }
            }
            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                document.writeTo(out)
            } ?: throw IllegalStateException("Could not open output stream")
            document.close()
            PdfOperationResult(success = true, pageCount = written)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Rebuild failed")
        }
    }
}
