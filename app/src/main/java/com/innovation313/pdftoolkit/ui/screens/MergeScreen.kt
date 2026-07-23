package com.innovation313.pdftoolkit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.viewmodel.PdfToolViewModel
import com.innovation313.pdftoolkit.viewmodel.ToolState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeScreen(viewModel: PdfToolViewModel, onBack: () -> Unit) {
    val context = LocalContext.current

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.setSelectedFiles(uris)
    }
    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.runMerge(context, uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_merge")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("merge_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { pickFiles.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_pdfs"))
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${viewModel.selectedUris.size} ${resolveLabel("files_selected")}",
                style = MaterialTheme.typography.bodyMedium
            )

            QualitySelector(viewModel)

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("merged.pdf") },
                enabled = viewModel.selectedUris.size >= 2 && viewModel.state !is ToolState.Processing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(resolveLabel("merge_now"))
            }

            ResultArea(viewModel.state)
        }
    }
}
