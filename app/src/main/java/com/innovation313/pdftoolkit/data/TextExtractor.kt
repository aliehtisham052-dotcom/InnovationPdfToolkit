package com.innovation313.pdftoolkit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class OcrResult(val success: Boolean, val text: String = "", val pagesProcessed: Int = 0, val errorMessage: String? = null)

object TextExtractor {

    /** Runs on-device OCR over every page of the PDF at [uri] and returns the combined text. */
    suspend fun extractText(context: Context, uri: Uri): OcrResult = withContext(Dispatchers.IO) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val combined = StringBuilder()
            var pageCount = 0

            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val scale = 2.0f // higher resolution improves OCR accuracy
                            val width = (page.width * scale).toInt().coerceAtLeast(1)
                            val height = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            val matrix = Matrix().apply { setScale(scale, scale) }
                            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val image = InputImage.fromBitmap(bitmap, 0)
                            val result = recognizer.process(image).await()
                            if (result.text.isNotBlank()) {
                                if (combined.isNotEmpty()) combined.append("\n\n--- ${i + 1} ---\n\n")
                                combined.append(result.text)
                            }
                            pageCount++
                            bitmap.recycle()
                        }
                    }
                }
            }
            recognizer.close()
            OcrResult(success = true, text = combined.toString(), pagesProcessed = pageCount)
        } catch (e: Exception) {
            OcrResult(success = false, errorMessage = e.message ?: "OCR failed")
        }
    }

    /** Saves extracted text to a plain .txt file at [outputUri]. */
    fun saveTextToFile(context: Context, outputUri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(outputUri)?.use { out ->
                out.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
