package com.innovation313.pdftoolkit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

object PdfExporter {

    /**
     * Renders every page as a JPEG into the user-picked folder ([treeUri] from OpenDocumentTree).
     * Returns how many images were written.
     */
    suspend fun exportPagesAsImages(
        context: Context,
        sourceUri: Uri,
        treeUri: Uri,
        baseName: String,
        quality: RenderQuality = RenderQuality.HIGH
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            var written = 0

            resolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        renderer.openPage(i).use { page ->
                            val width = (page.width * quality.scale).toInt().coerceAtLeast(1)
                            val height = (page.height * quality.scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            val matrix = Matrix().apply { setScale(quality.scale, quality.scale) }
                            page.render(bmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            val fileName = "${baseName}_${i + 1}.jpg"
                            val fileUri = DocumentsContract.createDocument(
                                resolver, dirUri, "image/jpeg", fileName
                            )
                            if (fileUri != null) {
                                resolver.openOutputStream(fileUri)?.use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                }
                                written++
                            }
                            bmp.recycle()
                        }
                    }
                }
            }
            PdfOperationResult(success = true, pageCount = written)
        } catch (e: Exception) {
            PdfOperationResult(success = false, errorMessage = e.message ?: "Export failed")
        }
    }

    /** Hands the PDF to Android's system print dialog (physical printer, or "Save as PDF"). */
    fun printPdf(context: Context, sourceUri: Uri, jobName: String) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }
        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
