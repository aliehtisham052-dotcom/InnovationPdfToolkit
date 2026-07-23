package com.innovation313.pdftoolkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.ui.resolveLabel

enum class PdfTool(val icon: ImageVector, val titleKey: String, val subtitleKey: String) {
    SCAN(Icons.Filled.DocumentScanner, "tool_scan", "tool_scan_desc"),
    MERGE(Icons.Filled.CallMerge, "tool_merge", "tool_merge_desc"),
    SPLIT(Icons.Filled.CallSplit, "tool_split", "tool_split_desc"),
    IMAGES_TO_PDF(Icons.Filled.Image, "tool_images_to_pdf", "tool_images_to_pdf_desc"),
    COMPRESS(Icons.Filled.Compress, "tool_compress", "tool_compress_desc"),
    OCR(Icons.Filled.TextFields, "tool_ocr", "tool_ocr_desc")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onToolSelected: (PdfTool) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(resolveLabel("app_name"), fontWeight = FontWeight.SemiBold) })
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(PdfTool.entries) { tool ->
                ToolCard(tool = tool, onClick = { onToolSelected(tool) })
            }
        }
    }
}

@Composable
private fun ToolCard(tool: PdfTool, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = resolveLabel(tool.titleKey),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = resolveLabel(tool.subtitleKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}
