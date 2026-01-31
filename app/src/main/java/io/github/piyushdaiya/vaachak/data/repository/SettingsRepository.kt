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
    @ApplicationContext private val context: Context
) {

    companion object {
        // --- APP GLOBAL SETTINGS ---
        val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val CF_URL = stringPreferencesKey("cloudflare_url")
        val CF_TOKEN = stringPreferencesKey("cloudflare_token")
        val IS_EINK_ENABLED = booleanPreferencesKey("is_eink_enabled")
        val AUTO_SAVE_RECAPS_KEY = booleanPreferencesKey("auto_save_recaps")
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val CONTRAST_KEY = floatPreferencesKey("eink_contrast")
        val DICTIONARY_FOLDER_KEY = stringPreferencesKey("dictionary_folder")
        val USE_EMBEDDED_DICT = booleanPreferencesKey("use_embedded_dict")
        val OFFLINE_MODE_KEY = booleanPreferencesKey("offline_mode")

        // --- NEW: READER PREFERENCES (v1.13) ---
        val READER_FONT_FAMILY = stringPreferencesKey("reader_font_family")
        val READER_FONT_SIZE = doublePreferencesKey("reader_font_size") // Readium uses Double/Float
        val READER_TEXT_ALIGN = stringPreferencesKey("reader_text_align")
        val READER_THEME = stringPreferencesKey("reader_theme") // LIGHT, DARK, SEPIA
        val READER_PUBLISHER_STYLES = booleanPreferencesKey("reader_publisher_styles")

        // Layout Sliders
        val READER_LETTER_SPACING = doublePreferencesKey("reader_letter_spacing")
        val READER_PARAGRAPH_SPACING = doublePreferencesKey("reader_para_spacing")
        val READER_MARGIN_SIDE = doublePreferencesKey("reader_margin_side")
        val READER_MARGIN_TOP = doublePreferencesKey("reader_margin_top")
        val READER_MARGIN_BOTTOM = doublePreferencesKey("reader_margin_bottom")
    }

    // --- APP FLOWS ---
    val geminiKey: Flow<String> = dataStore.data.map { it[GEMINI_KEY] ?: "" }
    val cfUrl: Flow<String> = dataStore.data.map { it[CF_URL] ?: "" }
    val cfToken: Flow<String> = dataStore.data.map { it[CF_TOKEN] ?: "" }
    val isEinkEnabled: Flow<Boolean> = dataStore.data.map { it[IS_EINK_ENABLED] ?: false }
    val isAutoSaveRecapsEnabled: Flow<Boolean> = dataStore.data.map { it[AUTO_SAVE_RECAPS_KEY] ?: true }
    val einkContrast: Flow<Float> = dataStore.data.map { it[CONTRAST_KEY] ?: 0.5f }
    val isOfflineModeEnabled: Flow<Boolean> = dataStore.data.map { it[OFFLINE_MODE_KEY] ?: false }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val name = prefs[THEME_KEY] ?: ThemeMode.E_INK.name
        try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.E_INK }
    }

    // Dictionary Flows
    fun getUseEmbeddedDictionary(): Flow<Boolean> = dataStore.data.map { it[USE_EMBEDDED_DICT] ?: false }
    fun getDictionaryFolder(): Flow<String> = dataStore.data.map { it[DICTIONARY_FOLDER_KEY] ?: "" }

    // --- NEW: READER PREF FLOWS ---
    val readerFontFamily: Flow<String?> = dataStore.data.map { it[READER_FONT_FAMILY] }
    val readerFontSize: Flow<Double> = dataStore.data.map { it[READER_FONT_SIZE] ?: 1.0 } // 1.0 = 100%
    val readerTextAlign: Flow<String> = dataStore.data.map { it[READER_TEXT_ALIGN] ?: "justify" }
    val readerTheme: Flow<String> = dataStore.data.map { it[READER_THEME] ?: "light" }
    val readerPublisherStyles: Flow<Boolean> = dataStore.data.map { it[READER_PUBLISHER_STYLES] ?: true } // Default to True

    val readerLetterSpacing: Flow<Double?> = dataStore.data.map { it[READER_LETTER_SPACING] }
    val readerParaSpacing: Flow<Double?> = dataStore.data.map { it[READER_PARAGRAPH_SPACING] }
    val readerMarginSide: Flow<Double> = dataStore.data.map { it[READER_MARGIN_SIDE] ?: 1.0 }
    val readerMarginTop: Flow<Double> = dataStore.data.map { it[READER_MARGIN_TOP] ?: 1.0 }
    val readerMarginBottom: Flow<Double> = dataStore.data.map { it[READER_MARGIN_BOTTOM] ?: 1.0 }

    // --- WRITE ACTIONS (App) ---
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

    suspend fun setAutoSaveRecaps(enabled: Boolean) { dataStore.edit { it[AUTO_SAVE_RECAPS_KEY] = enabled } }
    suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[THEME_KEY] = mode.name } }
    suspend fun setContrast(value: Float) { dataStore.edit { it[CONTRAST_KEY] = value } }
    suspend fun setUseEmbeddedDictionary(enabled: Boolean) { dataStore.edit { it[USE_EMBEDDED_DICT] = enabled } }
    suspend fun setDictionaryFolder(uri: String) { dataStore.edit { it[DICTIONARY_FOLDER_KEY] = uri } }
    suspend fun setOfflineMode(enabled: Boolean) { dataStore.edit { it[OFFLINE_MODE_KEY] = enabled } }
    suspend fun clearSettings() { dataStore.edit { it.clear() } }

    // --- NEW: READER WRITE ACTIONS ---
    suspend fun updateReaderPreferences(
        fontFamily: String? = null,
        fontSize: Double? = null,
        textAlign: String? = null,
        theme: String? = null,
        publisherStyles: Boolean? = null,
        letterSpacing: Double? = null,
        paraSpacing: Double? = null,
        marginSide: Double? = null,
        marginTop: Double? = null,
        marginBottom: Double? = null
    ) {
        dataStore.edit { prefs ->
            fontFamily?.let { prefs[READER_FONT_FAMILY] = it }
            fontSize?.let { prefs[READER_FONT_SIZE] = it }
            textAlign?.let { prefs[READER_TEXT_ALIGN] = it }
            theme?.let { prefs[READER_THEME] = it }
            publisherStyles?.let { prefs[READER_PUBLISHER_STYLES] = it }
            letterSpacing?.let { prefs[READER_LETTER_SPACING] = it }
            paraSpacing?.let { prefs[READER_PARAGRAPH_SPACING] = it }
            marginSide?.let { prefs[READER_MARGIN_SIDE] = it }
            marginTop?.let { prefs[READER_MARGIN_TOP] = it }
            marginBottom?.let { prefs[READER_MARGIN_BOTTOM] = it }
        }
    }

    suspend fun resetReaderLayout() {
        dataStore.edit { prefs ->
            prefs.remove(READER_TEXT_ALIGN)
            prefs.remove(READER_PUBLISHER_STYLES)
            prefs.remove(READER_LETTER_SPACING)
            prefs.remove(READER_PARAGRAPH_SPACING)
            prefs.remove(READER_MARGIN_SIDE)
            prefs.remove(READER_MARGIN_TOP)
            prefs.remove(READER_MARGIN_BOTTOM)
            // Note: We usually keep Font Size/Family across resets as they are accessibility features
        }
    }

    // --- VALIDATION ---
    fun validateStarDictFolder(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            val dir = DocumentFile.fromTreeUri(context, uri)
            if (dir == null || !dir.isDirectory || !dir.canRead()) return false
            val files = dir.listFiles()
            files.any { it.name?.endsWith(".idx", ignoreCase = true) == true }
        } catch (e: Exception) { false }
    }
}