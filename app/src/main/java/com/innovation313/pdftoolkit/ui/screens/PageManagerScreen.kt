package com.innovation313.pdftoolkit.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.PageItem
import com.innovation313.pdftoolkit.data.PageManager
import com.innovation313.pdftoolkit.data.RenderQuality
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private data class UiPage(val originalIndex: Int, val rotation: Int, val thumb: Bitmap)

private val ROW_HEIGHT = 96.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rowHeightPx = with(LocalDensity.current) { (ROW_HEIGHT + 10.dp).toPx() }

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    val pages = remember { mutableStateListOf<UiPage>() }
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    // Drag-to-reorder state
    var draggedKey by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            pages.clear()
            statusMessage = ""
            isLoading = true
            scope.launch {
                PageManager.pageBitmapFlow(context, uri, scale = 0.35f).collect { (index, bmp) ->
                    pages.add(UiPage(originalIndex = index, rotation = 0, thumb = bmp))
                }
                isLoading = false
            }
        }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val src = sourceUri
        if (destUri != null && src != null) {
            isSaving = true
            scope.launch {
                val result = PageManager.rebuildPdf(
                    context = context,
                    sourceUri = src,
                    pages = pages.map { PageItem(it.originalIndex, it.rotation) },
                    outputUri = destUri,
                    quality = RenderQuality.STANDARD
                )
                statusMessage = if (result.success) resolveLabelPlain(context, "scan_saved")
                else result.errorMessage ?: resolveLabelPlain(context, "failed")
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_pages")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    if (pages.isNotEmpty()) {
                        IconButton(
                            onClick = { createOutput.launch("edited.pdf") },
                            enabled = !isSaving && !isLoading
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = resolveLabel("save_pdf"))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedButton(
                onClick = { pickFile.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isSaving
            ) {
                Icon(Icons.Filled.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(resolveLabel("select_pdf"))
            }

            if (pages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    resolveLabel("drag_hint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (isSaving) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(resolveLabel("processing"), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(pages, key = { _, page -> page.originalIndex }) { index, page ->
                    val isDragged = draggedKey == page.originalIndex
                    PageRow(
                        position = index,
                        total = pages.size,
                        page = page,
                        isDragged = isDragged,
                        dragOffset = if (isDragged) dragOffset else 0f,
                        dragModifier = Modifier.pointerInput(page.originalIndex) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedKey = page.originalIndex
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val shift = (dragOffset / rowHeightPx).roundToInt()
                                    if (shift != 0) {
                                        val from = pages.indexOfFirst { it.originalIndex == page.originalIndex }
                                        if (from >= 0) {
                                            val to = (from + shift).coerceIn(0, pages.lastIndex)
                                            if (to != from) {
                                                val moved = pages.removeAt(from)
                                                pages.add(to, moved)
                                                dragOffset -= (to - from) * rowHeightPx
                                            }
                                        }
                                    }
                                },
                                onDragEnd = { draggedKey = null; dragOffset = 0f },
                                onDragCancel = { draggedKey = null; dragOffset = 0f }
                            )
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val tmp = pages[index - 1]; pages[index - 1] = pages[index]; pages[index] = tmp
                            }
                        },
                        onMoveDown = {
                            if (index < pages.size - 1) {
                                val tmp = pages[index + 1]; pages[index + 1] = pages[index]; pages[index] = tmp
                            }
                        },
                        onRotate = { pages[index] = page.copy(rotation = (page.rotation + 90) % 360) },
                        onDelete = { pages.removeAt(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PageRow(
    position: Int,
    total: Int,
    page: UiPage,
    isDragged: Boolean,
    dragOffset: Float,
    dragModifier: Modifier,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRotate: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDragged) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = if (isDragged) 8.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .graphicsLayer { translationY = dragOffset }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = resolveLabel("drag_handle"),
                modifier = dragModifier.padding(end = 6.dp)
            )
            Image(
                bitmap = page.thumb.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(60.dp).rotate(page.rotation.toFloat())
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${resolveLabel("page_label")} ${page.originalIndex + 1}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${position + 1} / $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onMoveUp, enabled = position > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = resolveLabel("move_up"))
            }
            IconButton(onClick = onMoveDown, enabled = position < total - 1) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = resolveLabel("move_down"))
            }
            IconButton(onClick = onRotate) {
                Icon(Icons.Filled.RotateRight, contentDescription = resolveLabel("rotate_page"))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = resolveLabel("delete_page"))
            }
        }
    }
}
