/*
 *  Copyright (c) 2026 Piyush Daiya
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 */

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

/**
 * Repository for managing application settings and user preferences.
 * Uses DataStore to persist settings such as API keys, theme, and reader preferences.
 */
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

    /**
     * Saves the core application settings.
     *
     * @param gemini The Gemini API key.
     * @param cfUrl The Cloudflare Worker URL.
     * @param cfToken The Cloudflare API token.
     * @param isEnk Whether E-ink optimization is enabled.
     */
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

    /**
     * Sets whether generated recaps should be automatically saved.
     *
     * @param enabled True to enable auto-save, false otherwise.
     */
    suspend fun setAutoSaveRecaps(enabled: Boolean) { dataStore.edit { it[AUTO_SAVE_RECAPS_KEY] = enabled } }

    /**
     * Sets the application theme mode.
     *
     * @param mode The [ThemeMode] to set.
     */
    suspend fun setThemeMode(mode: ThemeMode) { dataStore.edit { it[THEME_KEY] = mode.name } }

    /**
     * Sets the contrast level for E-ink mode.
     *
     * @param value The contrast value (0.0 to 1.0).
     */
    suspend fun setContrast(value: Float) { dataStore.edit { it[CONTRAST_KEY] = value } }

    /**
     * Sets whether to use the embedded dictionary.
     *
     * @param enabled True to use embedded dictionary, false otherwise.
     */
    suspend fun setUseEmbeddedDictionary(enabled: Boolean) { dataStore.edit { it[USE_EMBEDDED_DICT] = enabled } }

    /**
     * Sets the folder path for the embedded dictionary.
     *
     * @param uri The URI string of the dictionary folder.
     */
    suspend fun setDictionaryFolder(uri: String) { dataStore.edit { it[DICTIONARY_FOLDER_KEY] = uri } }

    /**
     * Sets whether offline mode is enabled.
     *
     * @param enabled True to enable offline mode, false otherwise.
     */
    suspend fun setOfflineMode(enabled: Boolean) { dataStore.edit { it[OFFLINE_MODE_KEY] = enabled } }

    /**
     * Clears all stored settings.
     */
    suspend fun clearSettings() { dataStore.edit { it.clear() } }

    // --- NEW: READER WRITE ACTIONS ---

    /**
     * Updates various reader preferences.
     * Only non-null parameters will be updated.
     *
     * @param fontFamily The font family name.
     * @param fontSize The font size multiplier.
     * @param textAlign The text alignment.
     * @param theme The reader theme (light, dark, sepia).
     * @param publisherStyles Whether to respect publisher styles.
     * @param letterSpacing The letter spacing value.
     * @param paraSpacing The paragraph spacing value.
     * @param marginSide The side margin value.
     * @param marginTop The top margin value.
     * @param marginBottom The bottom margin value.
     */
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

    /**
     * Resets reader layout preferences to their default values.
     * Does not reset font size or family.
     */
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

    /**
     * Validates if a folder contains valid StarDict dictionary files.
     *
     * @param uriString The URI string of the folder to check.
     * @return True if the folder contains at least one .idx file, false otherwise.
     */
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