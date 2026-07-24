package com.innovation313.pdftoolkit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.AdvancedTools
import com.innovation313.pdftoolkit.data.PdfEngine
import com.innovation313.pdftoolkit.data.RenderQuality
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

private enum class CompressMode { QUALITY, TARGET_SIZE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var mode by remember { mutableStateOf(CompressMode.QUALITY) }
    var quality by remember { mutableStateOf(RenderQuality.COMPRESSED) }
    var targetMb by remember { mutableStateOf("2") }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { sourceUri = uri; statusMessage = "" }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val src = sourceUri
        if (destUri != null && src != null) {
            isProcessing = true
            progressText = ""
            scope.launch {
                if (mode == CompressMode.QUALITY) {
                    val result = PdfEngine.compressPdf(context, src, destUri, quality)
                    statusMessage = if (result.success) resolveLabelPlain(context, "scan_saved")
                    else result.errorMessage ?: resolveLabelPlain(context, "failed")
                } else {
                    val targetBytes = ((targetMb.toDoubleOrNull() ?: 2.0) * 1024 * 1024).toLong()
                    val result = AdvancedTools.compressToTargetSize(
                        context = context,
                        sourceUri = src,
                        outputUri = destUri,
                        targetBytes = targetBytes,
                        onProgress = { attempt -> progressText = "${resolveLabelPlain(context, "attempt")} $attempt" }
                    )
                    statusMessage = if (result.success) {
                        val mb = result.finalSizeBytes / (1024.0 * 1024.0)
                        "${resolveLabelPlain(context, "final_size")}: ${"%.2f".format(mb)} MB"
                    } else result.errorMessage ?: resolveLabelPlain(context, "failed")
                }
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_compress")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("compress_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { pickFile.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_pdf"))
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == CompressMode.QUALITY, onClick = { mode = CompressMode.QUALITY })
                Text(resolveLabel("compress_by_quality"))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = mode == CompressMode.TARGET_SIZE, onClick = { mode = CompressMode.TARGET_SIZE })
                Text(resolveLabel("compress_by_size"))
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (mode == CompressMode.QUALITY) {
                Text(resolveLabel("quality_label"), style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RenderQuality.COMPRESSED to "quality_compressed",
                        RenderQuality.STANDARD to "quality_standard",
                        RenderQuality.HIGH to "quality_high"
                    ).forEach { (q, key) ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { quality = q },
                            label = { Text(resolveLabel(key)) }
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = targetMb,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) targetMb = it },
                    label = { Text(resolveLabel("target_size_mb")) },
                    supportingText = { Text(resolveLabel("target_size_hint")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("compressed.pdf") },
                enabled = sourceUri != null && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(resolveLabel("compress_now")) }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (progressText.isEmpty()) resolveLabel("processing") else progressText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
