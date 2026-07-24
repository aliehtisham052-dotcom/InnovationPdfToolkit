package com.innovation313.pdftoolkit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdCardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var backUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pickFront = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { frontUri = uri; statusMessage = "" }
    }
    val pickBack = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { backUri = uri; statusMessage = "" }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val front = frontUri
        if (destUri != null && front != null) {
            isProcessing = true
            scope.launch {
                val result = AdvancedTools.makeIdCardPdf(context, front, backUri, destUri)
                statusMessage = if (result.success) resolveLabelPlain(context, "scan_saved")
                else result.errorMessage ?: resolveLabelPlain(context, "failed")
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_idcard")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("idcard_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { pickFront.launch(arrayOf("image/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Icon(
                    if (frontUri != null) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_front"))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { pickBack.launch(arrayOf("image/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            ) {
                Icon(
                    if (backUri != null) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_back"))
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { createOutput.launch("id_card.pdf") },
                enabled = frontUri != null && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(resolveLabel("create_pdf")) }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                }
            }
            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
