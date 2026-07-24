package com.innovation313.pdftoolkit

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.innovation313.pdftoolkit.data.AppLanguage
import com.innovation313.pdftoolkit.data.ThemeMode
import com.innovation313.pdftoolkit.ui.screens.*
import com.innovation313.pdftoolkit.ui.theme.PdfToolkitTheme
import com.innovation313.pdftoolkit.viewmodel.AppViewModel
import com.innovation313.pdftoolkit.viewmodel.PdfToolViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: PdfToolViewModel by viewModels()
    private val appViewModel: AppViewModel by viewModels()

    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUri = extractPdfUri(intent)

        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()
            val language by appViewModel.language.collectAsState()

            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val baseContext = LocalContext.current
            val localizedContext = remember(language, baseContext) {
                if (language == AppLanguage.SYSTEM) {
                    baseContext
                } else {
                    val locale = Locale.forLanguageTag(language.tag)
                    val config = Configuration(baseContext.resources.configuration)
                    config.setLocale(locale)
                    baseContext.createConfigurationContext(config)
                }
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                PdfToolkitTheme(darkTheme = isDark) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(
                            viewModel = viewModel,
                            appViewModel = appViewModel,
                            incomingUri = incomingUri,
                            onIncomingHandled = { incomingUri = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri = extractPdfUri(intent)
    }

    /** Pulls a PDF Uri out of an ACTION_VIEW or ACTION_SEND intent, if present. */
    private fun extractPdfUri(intent: Intent?): Uri? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
    }
}

@Composable
private fun AppNavHost(
    viewModel: PdfToolViewModel,
    appViewModel: AppViewModel,
    incomingUri: Uri?,
    onIncomingHandled: () -> Unit
) {
    var currentTool by remember { mutableStateOf<PdfTool?>(null) }
    var viewerUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // A PDF opened from another app jumps straight into the viewer.
    if (incomingUri != null) {
        viewerUri = incomingUri
        currentTool = PdfTool.VIEWER
        onIncomingHandled()
    }

    fun goHome() {
        viewModel.reset()
        viewerUri = null
        currentTool = null
    }

    when (currentTool) {
        null -> HomeScreen(
            appViewModel = appViewModel,
            onToolSelected = { tool ->
                viewModel.reset()
                viewerUri = null
                currentTool = tool
            },
            onRecentOpened = { uri ->
                viewerUri = uri
                currentTool = PdfTool.VIEWER
            }
        )
        PdfTool.SCAN -> ScanScreen(onBack = ::goHome)
        PdfTool.VIEWER -> ViewerScreen(
            onBack = ::goHome,
            initialUri = viewerUri,
            onFileOpened = { uri -> appViewModel.rememberFile(context, uri) }
        )
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
