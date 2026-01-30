package io.github.piyushdaiya.vaachak.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode

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
        _cfUrl.value = settingsRepo.cfUrl.first() // Fixed: use public flow name
        _cfToken.value = settingsRepo.cfToken.first() // Fixed: use public flow name
        _isEinkEnabled.value = settingsRepo.isEinkEnabled.first()
        _isAutoSaveRecapsEnabled.value = settingsRepo.isAutoSaveRecapsEnabled.first()
    }

    // --- UI UPDATERS ---
    fun updateGemini(valText: String) { _geminiKey.value = valText }
    fun updateCfUrl(valText: String) { _cfUrl.value = valText }
    fun updateCfToken(valText: String) { _cfToken.value = valText }


    fun updateTheme(mode: ThemeMode) = viewModelScope.launch {
        settingsRepo.setThemeMode(mode)
        // 2. Sync the Boolean: If theme is E_INK, enable the flag automatically
        // This fixes the "Dead Code" issue
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

    // --- SAVE LOGIC (WITH CONTENT VALIDATION) ---
    suspend fun saveSettings() {
        val isDictEnabled = useEmbeddedDictionary.value
        val dictPath = dictionaryFolder.value

        if (isDictEnabled) {
            // 1. Basic Empty Check
            if (dictPath.isBlank()) {
                throw Exception("Please select a folder for the External Dictionary.")
            }

            // 2. Content Validation Check
            // We verify if the folder actually contains StarDict files (.idx)
            val isValid = settingsRepo.validateStarDictFolder(dictPath)
            if (!isValid) {
                throw Exception("Invalid Dictionary Folder: No StarDict files (.idx/.dict/.ifo) found in selected location.")
            }

        } else {
            // Cleanup if disabled
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
    }
}