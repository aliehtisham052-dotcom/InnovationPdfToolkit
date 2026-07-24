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
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsertScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseUri by remember { mutableStateOf<Uri?>(null) }
    var insertUri by remember { mutableStateOf<Uri?>(null) }
    var basePages by remember { mutableStateOf(0) }
    var afterPage by remember { mutableStateOf("0") }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pickBase = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            baseUri = uri
            statusMessage = ""
            scope.launch { basePages = PdfEngine.getPageCount(context, uri) }
        }
    }
    val pickInsert = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { insertUri = uri; statusMessage = "" }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val base = baseUri
        val insert = insertUri
        if (destUri != null && base != null && insert != null) {
            isProcessing = true
            scope.launch {
                val result = AdvancedTools.insertPages(
                    context = context,
                    baseUri = base,
                    insertUri = insert,
                    afterPage = afterPage.toIntOrNull() ?: 0,
                    outputUri = destUri
                )
                statusMessage = if (result.success) resolveLabelPlain(context, "scan_saved")
                else result.errorMessage ?: resolveLabelPlain(context, "failed")
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_insert")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("insert_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { pickBase.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_main_pdf"))
            }
            if (basePages > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("${resolveLabel("total_pages")}: $basePages", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { pickInsert.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_insert_pdf"))
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = afterPage,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) afterPage = it },
                label = { Text(resolveLabel("insert_after_page")) },
                supportingText = { Text(resolveLabel("insert_after_hint")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("combined.pdf") },
                enabled = baseUri != null && insertUri != null && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(resolveLabel("apply_now")) }

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
