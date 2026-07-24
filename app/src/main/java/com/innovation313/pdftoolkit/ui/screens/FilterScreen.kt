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
import com.innovation313.pdftoolkit.data.AdvancedTools
import com.innovation313.pdftoolkit.data.PageFilter
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var filter by remember { mutableStateOf(PageFilter.GRAYSCALE) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { sourceUri = uri; statusMessage = "" }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val src = sourceUri
        if (destUri != null && src != null) {
            isProcessing = true
            scope.launch {
                val result = AdvancedTools.applyFilter(context, src, destUri, filter)
                statusMessage = if (result.success) resolveLabelPlain(context, "scan_saved")
                else result.errorMessage ?: resolveLabelPlain(context, "failed")
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_filter")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("filter_instruction"), style = MaterialTheme.typography.bodyLarge)
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

            Spacer(modifier = Modifier.height(20.dp))
            Text(resolveLabel("filter_type"), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    PageFilter.GRAYSCALE to "filter_grayscale",
                    PageFilter.BLACK_WHITE to "filter_bw",
                    PageFilter.ENHANCE to "filter_enhance"
                ).forEach { (f, key) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = filter == f, onClick = { filter = f })
                        Text(resolveLabel(key))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("filtered.pdf") },
                enabled = sourceUri != null && !isProcessing,
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
