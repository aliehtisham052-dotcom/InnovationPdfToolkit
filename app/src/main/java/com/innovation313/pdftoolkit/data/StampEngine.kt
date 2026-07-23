package com.innovation313.pdftoolkit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class StampPosition { TOP_LEFT, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }

data class WatermarkSettings(
    val text: String = "",
    val opacityPercent: Int = 25,
    val diagonal: Boolean = true,
    val addPageNumbers: Boolean = false
)

object StampEngine {

    /**
     * Re-renders every page and overlays an optional diagonal/centered text watermark
     * and optional page numbers. Output is image-based, same trade-off as merge/split.
     */
    suspend fun applyWatermark(
        context: Context,
        sourceUri: Uri,
        outputUri: Uri,
        settings: WatermarkSettings,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var written = 0

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    for (i in 0 until total) {
                        renderer.openPage(i).use { page ->
                            val width = (page.width * quality.scale).toInt().coerceAtLeast(1)
                            val height = (page.height * quality.scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            val matrix = Matrix().apply { setScale(quality.scale, quality.scale) }
                            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            val canvas = Canvas(bmp)

                            if (settings.text.isNotBlank()) {
                                drawWatermarkText(canvas, width, height, settings)
                            }
                            if (settings.addPageNumbers) {
                                drawPageNumber(canvas, width, height, i + 1, total)
                            }

                            written++
                            val info = PdfDocument.PageInfo.Builder(width, height, written).create()
                            val pdfPage = document.startPage(info)
                            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                            document.finishPage(pdfPage)
                            bmp.recycle()
                        }
                    }
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { out -> document.writeTo(out) }
                ?: throw IllegalStateException("Could not open output stream")
            document.close()
            PdfOperationResult(success = true, pageCount = written)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Watermark failed")
        }
    }

    /**
     * Stamps a signature (or any transparent PNG bitmap) onto one page of the PDF.
     * [pageNumber] is 1-indexed; [widthFraction] sets the stamp width relative to page width.
     */
    suspend fun applyImageStamp(
        context: Context,
        sourceUri: Uri,
        outputUri: Uri,
        stamp: Bitmap,
        pageNumber: Int,
        position: StampPosition,
        widthFraction: Float = 0.3f,
        quality: RenderQuality = RenderQuality.STANDARD
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var written = 0

            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val width = (page.width * quality.scale).toInt().coerceAtLeast(1)
                            val height = (page.height * quality.scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(Color.WHITE)
                            val matrix = Matrix().apply { setScale(quality.scale, quality.scale) }
                            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            if (i == pageNumber - 1) {
                                drawStamp(Canvas(bmp), width, height, stamp, position, widthFraction)
                            }

                            written++
                            val info = PdfDocument.PageInfo.Builder(width, height, written).create()
                            val pdfPage = document.startPage(info)
                            pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                            document.finishPage(pdfPage)
                            bmp.recycle()
                        }
                    }
                }
            }

            context.contentResolver.openOutputStream(outputUri)?.use { out -> document.writeTo(out) }
                ?: throw IllegalStateException("Could not open output stream")
            document.close()
            PdfOperationResult(success = true, pageCount = written)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Signature stamp failed")
        }
    }

    private fun drawWatermarkText(canvas: Canvas, width: Int, height: Int, settings: WatermarkSettings) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            alpha = (settings.opacityPercent.coerceIn(5, 100) * 255 / 100)
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        // Size the text so it spans roughly 70% of the page width
        var size = width / 6f
        paint.textSize = size
        val bounds = Rect()
        paint.getTextBounds(settings.text, 0, settings.text.length, bounds)
        if (bounds.width() > 0) {
            size *= (width * 0.7f) / bounds.width()
            paint.textSize = size
        }

        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        if (settings.diagonal) canvas.rotate(-35f)
        canvas.drawText(settings.text, 0f, 0f, paint)
        canvas.restore()
    }

    private fun drawPageNumber(canvas: Canvas, width: Int, height: Int, page: Int, total: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textAlign = Paint.Align.CENTER
            textSize = width / 40f
        }
        canvas.drawText("$page / $total", width / 2f, height - (height * 0.03f), paint)
    }

    private fun drawStamp(
        canvas: Canvas,
        width: Int,
        height: Int,
        stamp: Bitmap,
        position: StampPosition,
        widthFraction: Float
    ) {
        val targetWidth = (width * widthFraction.coerceIn(0.1f, 0.9f)).toInt().coerceAtLeast(1)
        val ratio = stamp.height.toFloat() / stamp.width.toFloat()
        val targetHeight = (targetWidth * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(stamp, targetWidth, targetHeight, true)

        val margin = width * 0.06f
        val (x, y) = when (position) {
            StampPosition.TOP_LEFT -> margin to margin
            StampPosition.TOP_RIGHT -> (width - targetWidth - margin) to margin
            StampPosition.CENTER -> ((width - targetWidth) / 2f) to ((height - targetHeight) / 2f)
            StampPosition.BOTTOM_LEFT -> margin to (height - targetHeight - margin)
            StampPosition.BOTTOM_RIGHT -> (width - targetWidth - margin) to (height - targetHeight - margin)
        }

        canvas.drawBitmap(scaled, x, y, null)
        if (scaled != stamp) scaled.recycle()
    }
}
