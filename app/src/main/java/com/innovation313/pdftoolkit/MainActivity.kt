package com.innovation313.pdftoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.innovation313.pdftoolkit.ui.screens.*
import com.innovation313.pdftoolkit.ui.theme.PdfToolkitTheme
import com.innovation313.pdftoolkit.viewmodel.PdfToolViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PdfToolViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfToolkitTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(viewModel)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(viewModel: PdfToolViewModel) {
    var currentTool by remember { mutableStateOf<PdfTool?>(null) }

    fun goHome() {
        viewModel.reset()
        currentTool = null
    }

    when (currentTool) {
        null -> HomeScreen(onToolSelected = { tool ->
            viewModel.reset()
            currentTool = tool
        })
        PdfTool.SCAN -> ScanScreen(onBack = ::goHome)
        PdfTool.VIEWER -> ViewerScreen(onBack = ::goHome)
        PdfTool.PAGES -> PageManagerScreen(onBack = ::goHome)
        PdfTool.MERGE -> MergeScreen(viewModel, onBack = ::goHome)
        PdfTool.SPLIT -> SplitScreen(viewModel, onBack = ::goHome)
        PdfTool.IMAGES_TO_PDF -> ImagesToPdfScreen(viewModel, onBack = ::goHome)
        PdfTool.COMPRESS -> CompressScreen(viewModel, onBack = ::goHome)
        PdfTool.OCR -> OcrScreen(onBack = ::goHome)
        PdfTool.WATERMARK -> WatermarkScreen(onBack = ::goHome)
        PdfTool.SIGNATURE -> SignatureScreen(onBack = ::goHome)
        PdfTool.EXPORT -> ExportScreen(onBack = ::goHome)
    }
}
