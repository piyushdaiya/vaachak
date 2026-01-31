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

package io.github.piyushdaiya.vaachak.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages application-wide settings such as API keys, theme preferences, and dictionary configuration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    // --- STATE FLOWS ---
    private val _geminiKey = MutableStateFlow("")
    /**
     * The current Gemini API key.
     */
    val geminiKey = _geminiKey.asStateFlow()

    private val _cfUrl = MutableStateFlow("")
    /**
     * The current Cloudflare Worker URL.
     */
    val cfUrl = _cfUrl.asStateFlow()

    private val _cfToken = MutableStateFlow("")
    /**
     * The current Cloudflare API token.
     */
    val cfToken = _cfToken.asStateFlow()

    private val _isEinkEnabled = MutableStateFlow(false)
    /**
     * Indicates if E-ink optimization is enabled.
     */
    val isEinkEnabled = _isEinkEnabled.asStateFlow()

    private val _isAutoSaveRecapsEnabled = MutableStateFlow(true)
    /**
     * Indicates if generated recaps should be automatically saved as highlights.
     */
    val isAutoSaveRecapsEnabled = _isAutoSaveRecapsEnabled.asStateFlow()

    // NEW: Offline Mode State
    private val _isOfflineModeEnabled = MutableStateFlow(false)
    /**
     * Indicates if offline mode is enabled (disabling AI features).
     */
    val isOfflineModeEnabled = _isOfflineModeEnabled.asStateFlow()

    /**
     * The current theme mode of the application.
     */
    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.E_INK)

    /**
     * The contrast level for E-ink mode.
     */
    val einkContrast: StateFlow<Float> = settingsRepo.einkContrast
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)

    /**
     * Indicates if an embedded dictionary (StarDict) is used.
     */
    val useEmbeddedDictionary: StateFlow<Boolean> = settingsRepo.getUseEmbeddedDictionary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * The URI string of the folder containing dictionary files.
     */
    val dictionaryFolder: StateFlow<String> = settingsRepo.getDictionaryFolder()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        loadSettings()
    }

    private fun loadSettings() = viewModelScope.launch {
        _geminiKey.value = settingsRepo.geminiKey.first()
        _cfUrl.value = settingsRepo.cfUrl.first()
        _cfToken.value = settingsRepo.cfToken.first()
        _isEinkEnabled.value = settingsRepo.isEinkEnabled.first()
        _isAutoSaveRecapsEnabled.value = settingsRepo.isAutoSaveRecapsEnabled.first()
        _isOfflineModeEnabled.value = settingsRepo.isOfflineModeEnabled.first() // Load Offline Mode
    }

    // --- UI UPDATERS ---

    /**
     * Updates the Gemini API key in the UI state.
     *
     * @param valText The new API key.
     */
    fun updateGemini(valText: String) { _geminiKey.value = valText }

    /**
     * Updates the Cloudflare URL in the UI state.
     *
     * @param valText The new URL.
     */
    fun updateCfUrl(valText: String) { _cfUrl.value = valText }

    /**
     * Updates the Cloudflare token in the UI state.
     *
     * @param valText The new token.
     */
    fun updateCfToken(valText: String) { _cfToken.value = valText }

    /**
     * Updates the application theme mode.
     *
     * @param mode The new [ThemeMode].
     */
    fun updateTheme(mode: ThemeMode) = viewModelScope.launch {
        settingsRepo.setThemeMode(mode)
        _isEinkEnabled.value = (mode == ThemeMode.E_INK)
    }

    /**
     * Updates the contrast level for E-ink mode.
     *
     * @param newContrast The new contrast value (0.0 to 1.0).
     */
    fun updateContrast(newContrast: Float) = viewModelScope.launch {
        settingsRepo.setContrast(newContrast)
    }

    /**
     * Toggles the auto-save feature for recaps.
     *
     * @param enabled True to enable auto-save, false otherwise.
     */
    fun toggleAutoSaveRecaps(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoSaveRecaps(enabled)
        _isAutoSaveRecapsEnabled.value = enabled
    }

    /**
     * Toggles the use of an embedded dictionary.
     *
     * @param enabled True to use embedded dictionary, false otherwise.
     */
    fun toggleEmbeddedDictionary(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setUseEmbeddedDictionary(enabled)
    }

    /**
     * Updates the folder path for the embedded dictionary.
     *
     * @param uri The URI string of the folder.
     */
    fun updateDictionaryFolder(uri: String) = viewModelScope.launch {
        settingsRepo.setDictionaryFolder(uri)
    }

    // NEW: Toggle Offline Mode
    /**
     * Toggles offline mode.
     *
     * @param enabled True to enable offline mode, false otherwise.
     */
    fun toggleOfflineMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setOfflineMode(enabled)
        _isOfflineModeEnabled.value = enabled
    }

    // --- SAVE LOGIC (WITH CONTENT VALIDATION) ---

    /**
     * Saves the current settings to the repository.
     * Validates the dictionary folder if embedded dictionary is enabled.
     *
     * @throws Exception If validation fails (e.g., invalid dictionary folder).
     */
    suspend fun saveSettings() {
        val isDictEnabled = useEmbeddedDictionary.value
        val dictPath = dictionaryFolder.value

        if (isDictEnabled) {
            if (dictPath.isBlank()) {
                throw Exception("Please select a folder for the External Dictionary.")
            }
            val isValid = settingsRepo.validateStarDictFolder(dictPath)
            if (!isValid) {
                throw Exception("Invalid Dictionary Folder: No StarDict files (.idx/.dict/.ifo) found.")
            }
        } else {
            if (dictPath.isNotEmpty()) {
                settingsRepo.setDictionaryFolder("")
            }
        }

        settingsRepo.saveSettings(
            gemini = _geminiKey.value,
            cfUrl = _cfUrl.value,
            cfToken = _cfToken.value,
            isEnk = _isEinkEnabled.value
        )
    }

    /**
     * Resets all settings to their default values.
     */
    fun resetSettings() = viewModelScope.launch {
        settingsRepo.clearSettings()
        _geminiKey.value = ""
        _cfUrl.value = ""
        _cfToken.value = ""
        _isEinkEnabled.value = false
        _isAutoSaveRecapsEnabled.value = true
        _isOfflineModeEnabled.value = false
    }
}