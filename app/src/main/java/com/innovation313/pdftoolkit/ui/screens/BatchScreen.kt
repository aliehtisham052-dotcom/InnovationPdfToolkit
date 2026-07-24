package com.innovation313.pdftoolkit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
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
import com.innovation313.pdftoolkit.data.BatchOperation
import com.innovation313.pdftoolkit.data.BatchProcessor
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var operation by remember { mutableStateOf(BatchOperation.COMPRESS) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) { sourceUris = uris; statusMessage = "" }
    }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null && sourceUris.isNotEmpty()) {
            isProcessing = true
            progressText = ""
            scope.launch {
                val result = BatchProcessor.process(
                    context = context,
                    sourceUris = sourceUris,
                    treeUri = treeUri,
                    operation = operation,
                    onProgress = { done, total -> progressText = "$done / $total" }
                )
                statusMessage = if (result.success) {
                    "${resolveLabelPlain(context, "files_processed")}: ${result.filesProcessed}"
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
                title = { Text(resolveLabel("tool_batch")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("batch_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { pickFiles.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_pdfs"))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${sourceUris.size} ${resolveLabel("files_selected")}", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(20.dp))
            Text(resolveLabel("batch_operation"), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    BatchOperation.COMPRESS to "tool_compress",
                    BatchOperation.GRAYSCALE to "filter_grayscale",
                    BatchOperation.BLACK_WHITE to "filter_bw",
                    BatchOperation.ENHANCE to "filter_enhance"
                ).forEach { (op, key) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = operation == op, onClick = { operation = op })
                        Text(resolveLabel(key))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { pickFolder.launch(null) },
                enabled = sourceUris.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("choose_folder_and_run"))
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(24.dp))
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
