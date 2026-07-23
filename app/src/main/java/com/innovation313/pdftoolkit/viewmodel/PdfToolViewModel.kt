package com.innovation313.pdftoolkit.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.innovation313.pdftoolkit.data.PdfEngine
import com.innovation313.pdftoolkit.data.RenderQuality
import kotlinx.coroutines.launch

sealed class ToolState {
    data object Idle : ToolState()
    data object Processing : ToolState()
    data class Success(val pageCount: Int) : ToolState()
    data class Failed(val message: String) : ToolState()
}

class PdfToolViewModel : ViewModel() {

    var selectedUris by mutableStateOf<List<Uri>>(emptyList())
        private set
    var totalPagesInSource by mutableStateOf(0)
        private set
    var fromPage by mutableStateOf("1")
        private set
    var toPage by mutableStateOf("1")
        private set
    var quality by mutableStateOf(RenderQuality.STANDARD)
        private set
    var state by mutableStateOf<ToolState>(ToolState.Idle)
        private set

    fun setSelectedFiles(uris: List<Uri>) {
        selectedUris = uris
        state = ToolState.Idle
    }

    fun updateQuality(q: RenderQuality) {
        quality = q
    }

    fun onFromPageChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) fromPage = value
    }

    fun onToPageChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) toPage = value
    }

    fun loadPageCount(context: Context, uri: Uri) {
        viewModelScope.launch {
            totalPagesInSource = PdfEngine.getPageCount(context, uri)
            toPage = totalPagesInSource.toString()
        }
    }

    fun reset() {
        selectedUris = emptyList()
        totalPagesInSource = 0
        fromPage = "1"
        toPage = "1"
        state = ToolState.Idle
    }

    fun runMerge(context: Context, outputUri: Uri) {
        if (selectedUris.size < 2) {
            state = ToolState.Failed("Kam az kam 2 PDF files chunein")
            return
        }
        state = ToolState.Processing
        viewModelScope.launch {
            val result = PdfEngine.mergePdfs(context, selectedUris, outputUri, quality)
            state = if (result.success) ToolState.Success(result.pageCount)
            else ToolState.Failed(result.errorMessage ?: "Kuch ghalat ho gaya")
        }
    }

    fun runSplit(context: Context, outputUri: Uri) {
        val from = fromPage.toIntOrNull()
        val to = toPage.toIntOrNull()
        if (selectedUris.isEmpty() || from == null || to == null || from > to) {
            state = ToolState.Failed("Sahi page range chunein")
            return
        }
        state = ToolState.Processing
        viewModelScope.launch {
            val result = PdfEngine.splitPdf(context, selectedUris.first(), from, to, outputUri, quality)
            state = if (result.success) ToolState.Success(result.pageCount)
            else ToolState.Failed(result.errorMessage ?: "Kuch ghalat ho gaya")
        }
    }

    fun runImagesToPdf(context: Context, outputUri: Uri) {
        if (selectedUris.isEmpty()) {
            state = ToolState.Failed("Kam az kam 1 image chunein")
            return
        }
        state = ToolState.Processing
        viewModelScope.launch {
            val result = PdfEngine.imagesToPdf(context, selectedUris, outputUri)
            state = if (result.success) ToolState.Success(result.pageCount)
            else ToolState.Failed(result.errorMessage ?: "Kuch ghalat ho gaya")
        }
    }

    fun runCompress(context: Context, outputUri: Uri) {
        if (selectedUris.isEmpty()) {
            state = ToolState.Failed("PDF file chunein")
            return
        }
        state = ToolState.Processing
        viewModelScope.launch {
            val result = PdfEngine.compressPdf(context, selectedUris.first(), outputUri, quality)
            state = if (result.success) ToolState.Success(result.pageCount)
            else ToolState.Failed(result.errorMessage ?: "Kuch ghalat ho gaya")
        }
    }
}
