package com.innovation313.pdftoolkit.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pdftoolkit_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** BCP-47 tags matching the app's resource qualifiers; SYSTEM follows the device setting. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    URDU("ur"),
    ROMAN_URDU("ur-Latn")
}

data class RecentFile(val uri: String, val name: String, val timestamp: Long) {
    fun encode(): String = "$uri::$name::$timestamp"

    companion object {
        fun decode(s: String): RecentFile? {
            val parts = s.split("::")
            return if (parts.size == 3) {
                RecentFile(parts[0], parts[1], parts[2].toLongOrNull() ?: 0L)
            } else null
        }
    }
}

class AppPreferences(private val context: Context) {

    private object Keys {
        val RECENTS = stringPreferencesKey("recent_files")
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("app_language")
    }

    private val separator = "|||"

    val recentFilesFlow: Flow<List<RecentFile>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENTS]
            ?.split(separator)
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { RecentFile.decode(it) }
            ?: emptyList()
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.LANGUAGE]) {
            "ENGLISH" -> AppLanguage.ENGLISH
            "URDU" -> AppLanguage.URDU
            "ROMAN_URDU" -> AppLanguage.ROMAN_URDU
            else -> AppLanguage.SYSTEM
        }
    }

    /** Adds a file to the top of the recents list, de-duplicated, capped at 8 entries. */
    suspend fun addRecentFile(file: RecentFile) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENTS]
                ?.split(separator)
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { RecentFile.decode(it) }
                ?.toMutableList() ?: mutableListOf()
            current.removeAll { it.uri == file.uri }
            current.add(0, file)
            prefs[Keys.RECENTS] = current.take(8).joinToString(separator) { it.encode() }
        }
    }

    suspend fun clearRecents() {
        context.dataStore.edit { prefs -> prefs[Keys.RECENTS] = "" }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = language.name }
    }
}
