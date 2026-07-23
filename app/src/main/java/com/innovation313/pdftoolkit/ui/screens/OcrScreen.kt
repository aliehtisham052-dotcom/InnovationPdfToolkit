package com.innovation313.pdftoolkit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.TextExtractor
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var extractedText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isProcessing = true
            errorMessage = ""
            extractedText = ""
            scope.launch {
                val result = TextExtractor.extractText(context, uri)
                isProcessing = false
                if (result.success) {
                    extractedText = result.text.ifBlank { resolveLabelPlain(context, "ocr_no_text") }
                } else {
                    errorMessage = result.errorMessage ?: resolveLabelPlain(context, "failed")
                }
            }
        }
    }

    val saveText = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            TextExtractor.saveTextToFile(context, uri, extractedText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_ocr")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("ocr_instruction"), style = MaterialTheme.typography.bodyLarge)
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

            if (isProcessing) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(resolveLabel("ocr_processing"), style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            if (extractedText.isNotEmpty() && !isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        clipboardManager.setText(AnnotatedString(extractedText))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(resolveLabel("copy_result"))
                    }
                    OutlinedButton(onClick = { saveText.launch("extracted_text.txt") }) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(resolveLabel("save_as_text"))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SelectionContainer {
                    Text(
                        text = extractedText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else if (!isProcessing) {
                Spacer(modifier = Modifier.height(40.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Filled.TextFields,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
