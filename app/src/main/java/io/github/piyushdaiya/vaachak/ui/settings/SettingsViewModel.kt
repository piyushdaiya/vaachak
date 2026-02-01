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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    // --- STATE FLOWS ---
    private val _geminiKey = MutableStateFlow("")
    val geminiKey = _geminiKey.asStateFlow()

    private val _cfUrl = MutableStateFlow("")
    val cfUrl = _cfUrl.asStateFlow()

    private val _cfToken = MutableStateFlow("")
    val cfToken = _cfToken.asStateFlow()

    private val _isEinkEnabled = MutableStateFlow(false)
    val isEinkEnabled = _isEinkEnabled.asStateFlow()

    private val _isAutoSaveRecapsEnabled = MutableStateFlow(true)
    val isAutoSaveRecapsEnabled = _isAutoSaveRecapsEnabled.asStateFlow()

    // NEW: Offline Mode State
    private val _isOfflineModeEnabled = MutableStateFlow(false)
    val isOfflineModeEnabled = _isOfflineModeEnabled.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.E_INK)

    val einkContrast: StateFlow<Float> = settingsRepo.einkContrast
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)

    val useEmbeddedDictionary: StateFlow<Boolean> = settingsRepo.getUseEmbeddedDictionary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
    fun updateGemini(valText: String) { _geminiKey.value = valText }
    fun updateCfUrl(valText: String) { _cfUrl.value = valText }
    fun updateCfToken(valText: String) { _cfToken.value = valText }

    fun updateTheme(mode: ThemeMode) = viewModelScope.launch {
        settingsRepo.setThemeMode(mode)
        _isEinkEnabled.value = (mode == ThemeMode.E_INK)
    }

    fun updateContrast(newContrast: Float) = viewModelScope.launch {
        settingsRepo.setContrast(newContrast)
    }

    fun toggleAutoSaveRecaps(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoSaveRecaps(enabled)
        _isAutoSaveRecapsEnabled.value = enabled
    }

    fun toggleEmbeddedDictionary(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setUseEmbeddedDictionary(enabled)
    }

    fun updateDictionaryFolder(uri: String) = viewModelScope.launch {
        settingsRepo.setDictionaryFolder(uri)
    }

    // NEW: Toggle Offline Mode
    fun toggleOfflineMode(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setOfflineMode(enabled)
        _isOfflineModeEnabled.value = enabled
    }

    // --- SAVE LOGIC (WITH CONTENT VALIDATION) ---
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