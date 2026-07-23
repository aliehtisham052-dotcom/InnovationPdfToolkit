package com.innovation313.pdftoolkit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.StampEngine
import com.innovation313.pdftoolkit.data.WatermarkSettings
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var watermarkText by remember { mutableStateOf("") }
    var opacity by remember { mutableStateOf(25f) }
    var diagonal by remember { mutableStateOf(true) }
    var pageNumbers by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            statusMessage = ""
        }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val src = sourceUri
        if (destUri != null && src != null) {
            isProcessing = true
            scope.launch {
                val result = StampEngine.applyWatermark(
                    context = context,
                    sourceUri = src,
                    outputUri = destUri,
                    settings = WatermarkSettings(
                        text = watermarkText,
                        opacityPercent = opacity.toInt(),
                        diagonal = diagonal,
                        addPageNumbers = pageNumbers
                    )
                )
                statusMessage = if (result.success) {
                    resolveLabelPlain(context, "scan_saved")
                } else {
                    result.errorMessage ?: resolveLabelPlain(context, "failed")
                }
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_watermark")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("watermark_instruction"), style = MaterialTheme.typography.bodyLarge)
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = watermarkText,
                onValueChange = { watermarkText = it },
                label = { Text(resolveLabel("watermark_text")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("${resolveLabel("opacity")}: ${opacity.toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                valueRange = 5f..100f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(resolveLabel("diagonal"), modifier = Modifier.weight(1f))
                Switch(checked = diagonal, onCheckedChange = { diagonal = it })
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(resolveLabel("add_page_numbers"), modifier = Modifier.weight(1f))
                Switch(checked = pageNumbers, onCheckedChange = { pageNumbers = it })
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("watermarked.pdf") },
                enabled = sourceUri != null && !isProcessing &&
                    (watermarkText.isNotBlank() || pageNumbers),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(resolveLabel("apply_now"))
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(resolveLabel("processing"), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
