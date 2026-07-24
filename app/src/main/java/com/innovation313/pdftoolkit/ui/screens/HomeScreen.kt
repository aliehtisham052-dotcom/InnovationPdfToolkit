package com.innovation313.pdftoolkit.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.AppLanguage
import com.innovation313.pdftoolkit.data.ThemeMode
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.viewmodel.AppViewModel

enum class PdfTool(val icon: ImageVector, val titleKey: String, val subtitleKey: String) {
    SCAN(Icons.Filled.DocumentScanner, "tool_scan", "tool_scan_desc"),
    VIEWER(Icons.Filled.MenuBook, "tool_viewer", "tool_viewer_desc"),
    PAGES(Icons.Filled.Reorder, "tool_pages", "tool_pages_desc"),
    MERGE(Icons.Filled.CallMerge, "tool_merge", "tool_merge_desc"),
    SPLIT(Icons.Filled.CallSplit, "tool_split", "tool_split_desc"),
    IMAGES_TO_PDF(Icons.Filled.Image, "tool_images_to_pdf", "tool_images_to_pdf_desc"),
    COMPRESS(Icons.Filled.Compress, "tool_compress", "tool_compress_desc"),
    OCR(Icons.Filled.TextFields, "tool_ocr", "tool_ocr_desc"),
    WATERMARK(Icons.Filled.WaterDrop, "tool_watermark", "tool_watermark_desc"),
    SIGNATURE(Icons.Filled.Draw, "tool_signature", "tool_signature_desc"),
    EXPORT(Icons.Filled.IosShare, "tool_export", "tool_export_desc"),
    FILTER(Icons.Filled.FilterBAndW, "tool_filter", "tool_filter_desc"),
    INSERT(Icons.Filled.PostAdd, "tool_insert", "tool_insert_desc"),
    ID_CARD(Icons.Filled.Badge, "tool_idcard", "tool_idcard_desc"),
    BATCH(Icons.Filled.ContentCopy, "tool_batch", "tool_batch_desc")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    onToolSelected: (PdfTool) -> Unit,
    onRecentOpened: (Uri) -> Unit
) {
    val recents by appViewModel.recentFiles.collectAsState()
    val themeMode by appViewModel.themeMode.collectAsState()
    val language by appViewModel.language.collectAsState()
    var languageMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("app_name"), fontWeight = FontWeight.SemiBold) },
                actions = {
                    Box {
                        IconButton(onClick = { languageMenuOpen = true }) {
                            Icon(Icons.Filled.Language, contentDescription = resolveLabel("language"))
                        }
                        DropdownMenu(
                            expanded = languageMenuOpen,
                            onDismissRequest = { languageMenuOpen = false }
                        ) {
                            listOf(
                                AppLanguage.SYSTEM to "lang_system",
                                AppLanguage.ENGLISH to "lang_english",
                                AppLanguage.URDU to "lang_urdu",
                                AppLanguage.ROMAN_URDU to "lang_roman_urdu"
                            ).forEach { (lang, key) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            resolveLabel(key),
                                            fontWeight = if (language == lang) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        appViewModel.setLanguage(lang)
                                        languageMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        val next = when (themeMode) {
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                        }
                        appViewModel.setThemeMode(next)
                    }) {
                        val icon = when (themeMode) {
                            ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                            ThemeMode.LIGHT -> Icons.Filled.LightMode
                            ThemeMode.DARK -> Icons.Filled.DarkMode
                        }
                        Icon(icon, contentDescription = resolveLabel("theme_toggle"))
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            if (recents.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = resolveLabel("recent_files"),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { appViewModel.clearRecents() }) {
                                Text(resolveLabel("clear"))
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recents) { file ->
                                AssistChip(
                                    onClick = { onRecentOpened(Uri.parse(file.uri)) },
                                    label = { Text(file.name.take(22)) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            items(PdfTool.entries) { tool ->
                ToolCard(tool = tool, onClick = { onToolSelected(tool) })
            }
        }
    }
}

@Composable
private fun ToolCard(tool: PdfTool, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().height(140.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = resolveLabel(tool.titleKey),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = resolveLabel(tool.subtitleKey),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}
