package com.innovation313.pdftoolkit.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.innovation313.pdftoolkit.data.StampEngine
import com.innovation313.pdftoolkit.data.StampPosition
import com.innovation313.pdftoolkit.ui.resolveLabel
import com.innovation313.pdftoolkit.ui.resolveLabelPlain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val strokes = remember { mutableStateListOf<MutableList<Offset>>() }
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var pageNumber by remember { mutableStateOf("1") }
    var position by remember { mutableStateOf(StampPosition.BOTTOM_RIGHT) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceUri = uri
            statusMessage = ""
        }
    }

    val createOutput = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { destUri ->
        val src = sourceUri
        if (destUri != null && src != null) {
            val stampBitmap = buildSignatureBitmap(strokes, canvasWidth, canvasHeight)
            if (stampBitmap == null) {
                statusMessage = resolveLabelPlain(context, "signature_empty")
                return@rememberLauncherForActivityResult
            }
            isProcessing = true
            scope.launch {
                val result = StampEngine.applyImageStamp(
                    context = context,
                    sourceUri = src,
                    outputUri = destUri,
                    stamp = stampBitmap,
                    pageNumber = pageNumber.toIntOrNull() ?: 1,
                    position = position
                )
                statusMessage = if (result.success) {
                    resolveLabelPlain(context, "scan_saved")
                } else {
                    result.errorMessage ?: resolveLabelPlain(context, "failed")
                }
                stampBitmap.recycle()
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(resolveLabel("tool_signature")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { strokes.clear() }) {
                        Icon(Icons.Filled.Delete, contentDescription = resolveLabel("clear_signature"))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(20.dp).fillMaxSize()) {
            Text(resolveLabel("signature_instruction"), style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> strokes.add(mutableListOf(offset)) },
                            onDrag = { change, _ ->
                                if (strokes.isNotEmpty()) {
                                    strokes[strokes.lastIndex].add(change.position)
                                }
                            }
                        )
                    }
            ) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()
                strokes.forEach { points ->
                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(path, Color.Black, style = Stroke(width = 5f))
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = pageNumber,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) pageNumber = it },
                label = { Text(resolveLabel("page_number")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(resolveLabel("position"), style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    StampPosition.BOTTOM_LEFT to "pos_bottom_left",
                    StampPosition.BOTTOM_RIGHT to "pos_bottom_right",
                    StampPosition.CENTER to "pos_center"
                ).forEach { (pos, key) ->
                    FilterChip(
                        selected = position == pos,
                        onClick = { position = pos },
                        label = { Text(resolveLabel(key)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { createOutput.launch("signed.pdf") },
                enabled = sourceUri != null && strokes.isNotEmpty() && !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(resolveLabel("apply_signature"))
            }

            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                }
            }
            if (statusMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Converts the drawn strokes into a transparent-background bitmap suitable for stamping. */
private fun buildSignatureBitmap(
    strokes: List<List<Offset>>,
    width: Int,
    height: Int
): Bitmap? {
    if (strokes.isEmpty() || width <= 0 || height <= 0) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = android.graphics.Paint.Cap.ROUND
        strokeJoin = android.graphics.Paint.Join.ROUND
    }
    strokes.forEach { points ->
        if (points.size > 1) {
            val path = android.graphics.Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
