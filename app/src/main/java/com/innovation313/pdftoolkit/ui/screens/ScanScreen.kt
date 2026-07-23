package com.innovation313.pdftoolkit.ui.screens

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
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
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.innovation313.pdftoolkit.data.PdfEngine
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingPdfUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val source = pendingPdfUri
        if (destUri != null && source != null) {
            isSaving = true
            scope.launch {
                val ok = PdfEngine.copyContentUri(context, source, destUri)
                statusMessage = if (ok) resolveLabelPlain(context, "scan_saved") else resolveLabelPlain(context, "scan_save_failed")
                isSaving = false
            }
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pdf = scanResult?.pdf
            if (pdf != null) {
                pendingPdfUri = pdf.uri
                statusMessage = resolveLabelPlain(context, "scan_ready")
                createOutput.launch("scan.pdf")
            }
        }
    }

    fun startScan() {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(context as Activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                statusMessage = e.message ?: resolveLabelPlain(context, "scan_start_failed")
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_scan")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(resolveLabel("scan_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { startScan() }, enabled = !isSaving) {
                Text(resolveLabel("start_scan"))
            }

            if (isSaving) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator()
            }

            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
