package com.innovation313.pdftoolkit.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.viewmodel.PdfToolViewModel
import com.innovation313.pdftoolkit.viewmodel.ToolState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitScreen(viewModel: PdfToolViewModel, onBack: () -> Unit) {
    val context = LocalContext.current

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.setSelectedFiles(listOf(uri))
            viewModel.loadPageCount(context, uri)
        }
    }
    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) viewModel.runSplit(context, uri)
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_pdf"))
            }

            if (viewModel.totalPagesInSource > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${resolveLabel("total_pages")}: ${viewModel.totalPagesInSource}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.fromPage,
                        onValueChange = viewModel::onFromPageChange,
                        label = { Text(resolveLabel("from_page")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = viewModel.toPage,
                        onValueChange = viewModel::onToPageChange,
                        label = { Text(resolveLabel("to_page")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            QualitySelector(viewModel)

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("split.pdf") },
                enabled = viewModel.selectedUris.isNotEmpty() && viewModel.state !is ToolState.Processing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(resolveLabel("split_now"))
            }

            ResultArea(viewModel.state)
        }
    }
}
