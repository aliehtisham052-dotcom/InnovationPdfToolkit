package com.innovation313.pdftoolkit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.RenderQuality
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.viewmodel.PdfToolViewModel
import com.innovation313.pdftoolkit.viewmodel.ToolState

@Composable
fun QualitySelector(viewModel: PdfToolViewModel) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(resolveLabel("quality_label"), style = MaterialTheme.typography.labelLarge)
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(
            RenderQuality.COMPRESSED to "quality_compressed",
            RenderQuality.STANDARD to "quality_standard",
            RenderQuality.HIGH to "quality_high"
        )
        options.forEach { (q, key) ->
            FilterChip(
                selected = viewModel.quality == q,
                onClick = { viewModel.setQuality(q) },
                label = { Text(resolveLabel(key)) }
            )
        }
    }
}

@Composable
fun ResultArea(state: ToolState) {
    Spacer(modifier = Modifier.height(24.dp))
    when (state) {
        is ToolState.Idle -> Unit
        is ToolState.Processing -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text(resolveLabel("processing"), style = MaterialTheme.typography.bodyMedium)
            }
        }
        is ToolState.Success -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(resolveLabel("success"), fontWeight = FontWeight.Bold)
                    Text("${resolveLabel("pages")}: ${state.pageCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        is ToolState.Failed -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(resolveLabel("failed"), fontWeight = FontWeight.Bold)
                    Text(state.message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
