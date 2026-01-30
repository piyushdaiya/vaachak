package io.github.piyushdaiya.vaachak.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context // Added back for file checking
) {

    // --- PREFERENCE KEYS ---
    companion object {
        val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val CF_URL = stringPreferencesKey("cloudflare_url")
        val CF_TOKEN = stringPreferencesKey("cloudflare_token")
        val IS_EINK_ENABLED = booleanPreferencesKey("is_eink_enabled")
        val AUTO_SAVE_RECAPS_KEY = booleanPreferencesKey("auto_save_recaps")
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val CONTRAST_KEY = floatPreferencesKey("eink_contrast")

        val DICTIONARY_FOLDER_KEY = stringPreferencesKey("dictionary_folder")
        val USE_EMBEDDED_DICT = booleanPreferencesKey("use_embedded_dict")
    }

    // --- READ FLOWS ---
    val geminiKey: Flow<String> = dataStore.data.map { it[GEMINI_KEY] ?: "" }
    val cfUrl: Flow<String> = dataStore.data.map { it[CF_URL] ?: "" }
    val cfToken: Flow<String> = dataStore.data.map { it[CF_TOKEN] ?: "" }
    val isEinkEnabled: Flow<Boolean> = dataStore.data.map { it[IS_EINK_ENABLED] ?: false }
    val isAutoSaveRecapsEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_SAVE_RECAPS_KEY] ?: true }
    val einkContrast: Flow<Float> = dataStore.data.map { it[CONTRAST_KEY] ?: 0.5f }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val name = prefs[THEME_KEY] ?: ThemeMode.E_INK.name
        try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.E_INK }
    }

    // Dictionary Flows
    fun getUseEmbeddedDictionary(): Flow<Boolean> = dataStore.data.map { it[USE_EMBEDDED_DICT] ?: false }
    fun getDictionaryFolder(): Flow<String> = dataStore.data.map { it[DICTIONARY_FOLDER_KEY] ?: "" }

    // --- WRITE ACTIONS ---
    suspend fun saveSettings(gemini: String, cfUrl: String, cfToken: String, isEnk: Boolean) {
        dataStore.edit { prefs ->
            prefs[GEMINI_KEY] = gemini.trim()

            var cleanUrl = cfUrl.trim()
            if (cleanUrl.endsWith("/")) cleanUrl = cleanUrl.removeSuffix("/")

            prefs[CF_URL] = cleanUrl
            prefs[CF_TOKEN] = cfToken.trim()
            prefs[IS_EINK_ENABLED] = isEnk
        }
    }

    suspend fun setAutoSaveRecaps(enabled: Boolean) {
        dataStore.edit { it[AUTO_SAVE_RECAPS_KEY] = enabled }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_KEY] = mode.name }
    }

    suspend fun setContrast(value: Float) {
        dataStore.edit { it[CONTRAST_KEY] = value }
    }

    suspend fun setUseEmbeddedDictionary(enabled: Boolean) {
        dataStore.edit { it[USE_EMBEDDED_DICT] = enabled }
    }

    suspend fun setDictionaryFolder(uri: String) {
        dataStore.edit { it[DICTIONARY_FOLDER_KEY] = uri }
    }

    suspend fun clearSettings() {
        dataStore.edit { it.clear() }
    }

    // --- VALIDATION LOGIC ---
    fun validateStarDictFolder(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val dir = DocumentFile.fromTreeUri(context, uri)

            // 1. Check if folder allows access
            if (dir == null || !dir.isDirectory || !dir.canRead()) return false

            // 2. Scan for at least one .idx file
            // StarDict requires .idx (index), .ifo (info), and .dict (data)
            // Finding an .idx is a strong enough indicator that this is the right folder.
            val files = dir.listFiles()
            files.any { it.name?.endsWith(".idx", ignoreCase = true) == true }
        } catch (e: Exception) {
            false
        }
    }
}