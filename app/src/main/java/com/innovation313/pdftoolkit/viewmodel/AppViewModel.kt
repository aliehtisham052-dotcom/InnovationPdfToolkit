package com.innovation313.pdftoolkit.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.innovation313.pdftoolkit.data.AppLanguage
import com.innovation313.pdftoolkit.data.AppPreferences
import com.innovation313.pdftoolkit.data.RecentFile
import com.innovation313.pdftoolkit.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    val recentFiles: StateFlow<List<RecentFile>> = prefs.recentFilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = prefs.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val language: StateFlow<AppLanguage> = prefs.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.SYSTEM)

    fun rememberFile(context: Context, uri: Uri) {
        val name = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "document.pdf"
        viewModelScope.launch {
            prefs.addRecentFile(RecentFile(uri.toString(), name, System.currentTimeMillis()))
        }
    }

    fun clearRecents() {
        viewModelScope.launch { prefs.clearRecents() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setLanguage(lang: AppLanguage) {
        viewModelScope.launch { prefs.setLanguage(lang) }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
