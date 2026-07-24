package com.innovation313.pdftoolkit.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.PageManager
import com.innovation313.pdftoolkit.data.PdfExporter
import com.innovation313.pdftoolkit.ui.resolveLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    initialUri: Uri? = null,
    onFileOpened: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pageBitmaps = remember { mutableStateListOf<Bitmap>() }
    var isLoading by remember { mutableStateOf(false) }
    var hasOpened by remember { mutableStateOf(false) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }

    // Pinch-to-zoom state, shared across the page column
    var scale by remember { mutableStateOf(1f) }
    val listState = rememberLazyListState()
    var showThumbnails by remember { mutableStateOf(false) }

    fun loadPdf(uri: Uri) {
        pageBitmaps.clear()
        currentUri = uri
        hasOpened = true
        isLoading = true
        scale = 1f
        onFileOpened(uri)
        scope.launch {
            PageManager.pageBitmapFlow(context, uri, scale = 1.4f).collect { (_, bmp) ->
                pageBitmaps.add(bmp)
            }
            isLoading = false
        }
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null) loadPdf(initialUri)
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) loadPdf(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_viewer")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    if (hasOpened) {
                        IconButton(onClick = { showThumbnails = !showThumbnails }) {
                            Icon(Icons.Filled.GridView, contentDescription = resolveLabel("thumbnails"))
                        }
                        IconButton(onClick = { scale = 1f }) {
                            Icon(Icons.Filled.ZoomOutMap, contentDescription = resolveLabel("reset_zoom"))
                        }
                        IconButton(onClick = {
                            currentUri?.let { PdfExporter.printPdf(context, it, "Innovation PDF Toolkit") }
                        }) {
                            Icon(Icons.Filled.Print, contentDescription = resolveLabel("print_pdf"))
                        }
                    }
                    IconButton(onClick = { pickFile.launch(arrayOf("application/pdf")) }, enabled = !isLoading) {
                        Icon(Icons.Filled.UploadFile, contentDescription = resolveLabel("select_pdf"))
                    }
                }
            )
        }
    ) { padding ->
        if (!hasOpened) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(resolveLabel("viewer_instruction"), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { pickFile.launch(arrayOf("application/pdf")) }) {
                    Text(resolveLabel("select_pdf"))
                }
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (showThumbnails) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItemsIndexed(pageBitmaps) { index, bmp ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    showThumbnails = false
                                    scope.launch { listState.scrollToItem(index) }
                                }
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                            }
                        }
                ) {
                    itemsIndexed(pageBitmaps) { index, bmp ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${index + 1} / ${pageBitmaps.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
