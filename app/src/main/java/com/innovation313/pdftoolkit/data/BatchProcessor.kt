package com.innovation313.pdftoolkit.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class BatchOperation { COMPRESS, GRAYSCALE, BLACK_WHITE, ENHANCE }

data class BatchResult(val success: Boolean, val filesProcessed: Int, val errorMessage: String? = null)

object BatchProcessor {

    /**
     * Runs [operation] over every file in [sourceUris], writing results into [treeUri].
     * [onProgress] reports (completed, total) so the UI can show real progress rather than a
     * blank spinner on what can be a long job.
     */
    suspend fun process(
        context: Context,
        sourceUris: List<Uri>,
        treeUri: Uri,
        operation: BatchOperation,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): BatchResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri, DocumentsContract.getTreeDocumentId(treeUri)
            )
            var done = 0

            for ((index, src) in sourceUris.withIndex()) {
                val baseName = displayName(context, src).substringBeforeLast('.', "document")
                val suffix = when (operation) {
                    BatchOperation.COMPRESS -> "compressed"
                    BatchOperation.GRAYSCALE -> "grayscale"
                    BatchOperation.BLACK_WHITE -> "bw"
                    BatchOperation.ENHANCE -> "enhanced"
                }
                val outUri = DocumentsContract.createDocument(
                    resolver, dirUri, "application/pdf", "${baseName}_$suffix.pdf"
                )
                if (outUri != null) {
                    val result = when (operation) {
                        BatchOperation.COMPRESS ->
                            PdfEngine.compressPdf(context, src, outUri, RenderQuality.COMPRESSED)
                        BatchOperation.GRAYSCALE ->
                            AdvancedTools.applyFilter(context, src, outUri, PageFilter.GRAYSCALE)
                        BatchOperation.BLACK_WHITE ->
                            AdvancedTools.applyFilter(context, src, outUri, PageFilter.BLACK_WHITE)
                        BatchOperation.ENHANCE ->
                            AdvancedTools.applyFilter(context, src, outUri, PageFilter.ENHANCE)
                    }
                    if (result.success) done++
                }
                onProgress(index + 1, sourceUris.size)
            }
            BatchResult(success = true, filesProcessed = done)
        } catch (e: Exception) {
            BatchResult(false, 0, e.message ?: "Batch processing failed")
        }
    }

    private fun displayName(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else "document.pdf"
            } ?: "document.pdf"
        } catch (e: Exception) {
            "document.pdf"
        }
    }
}
