package com.innovation313.pdftoolkit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.AdvancedTools
import com.innovation313.pdftoolkit.data.PdfEngine
import com.innovation313.pdftoolkit.data.SplitMode
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf(SplitMode.RANGE) }
    var fromPage by remember { mutableStateOf("1") }
    var toPage by remember { mutableStateOf("1") }
    var pagesPerFile by remember { mutableStateOf("2") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            statusMessage = ""
            scope.launch {
                totalPages = PdfEngine.getPageCount(context, uri)
                toPage = totalPages.toString()
            }
        }
    }

    // RANGE mode writes a single file
    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val src = sourceUri
        if (destUri != null && src != null) {
            isProcessing = true
            scope.launch {
                val result = PdfEngine.splitPdf(
                    context, src,
                    fromPage.toIntOrNull() ?: 1,
                    toPage.toIntOrNull() ?: 1,
                    destUri
                )
                statusMessage = if (result.success) {
                    "${resolveLabelPlain(context, "scan_saved")} (${result.pageCount} ${resolveLabelPlain(context, "pages")})"
                } else result.errorMessage ?: resolveLabelPlain(context, "failed")
                isProcessing = false
            }
        }
    }

    // EVERY_PAGE / EVERY_N_PAGES write many files into a folder
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        val src = sourceUri
        if (treeUri != null && src != null) {
            isProcessing = true
            scope.launch {
                val result = AdvancedTools.splitIntoFiles(
                    context = context,
                    sourceUri = src,
                    treeUri = treeUri,
                    mode = mode,
                    pagesPerFile = pagesPerFile.toIntOrNull() ?: 1
                )
                statusMessage = if (result.success) {
                    "${resolveLabelPlain(context, "files_created")}: ${result.pageCount}"
                } else result.errorMessage ?: resolveLabelPlain(context, "failed")
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_split")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("split_instruction"), style = MaterialTheme.typography.bodyLarge)
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

            if (totalPages > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("${resolveLabel("total_pages")}: $totalPages", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(resolveLabel("split_mode"), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    SplitMode.RANGE to "split_mode_range",
                    SplitMode.EVERY_PAGE to "split_mode_every_page",
                    SplitMode.EVERY_N_PAGES to "split_mode_every_n"
                ).forEach { (m, key) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = mode == m, onClick = { mode = m })
                        Text(resolveLabel(key))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (mode) {
                SplitMode.RANGE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = fromPage,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) fromPage = it },
                            label = { Text(resolveLabel("from_page")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toPage,
                            onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) toPage = it },
                            label = { Text(resolveLabel("to_page")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                SplitMode.EVERY_N_PAGES -> {
                    OutlinedTextField(
                        value = pagesPerFile,
                        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) pagesPerFile = it },
                        label = { Text(resolveLabel("pages_per_file")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SplitMode.EVERY_PAGE -> Unit
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (mode == SplitMode.RANGE) createOutput.launch("split.pdf") else pickFolder.launch(null)
                },
                enabled = sourceUri != null && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (mode != SplitMode.RANGE) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(resolveLabel("split_now"))
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
