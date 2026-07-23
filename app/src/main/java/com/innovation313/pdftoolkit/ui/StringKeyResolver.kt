package com.innovation313.pdftoolkit.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun resolveLabel(key: String): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else key
}

/** Non-Composable variant for use inside callbacks/coroutines where LocalContext isn't available. */
fun resolveLabelPlain(context: Context, key: String): String {
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) context.getString(resId) else key
}
