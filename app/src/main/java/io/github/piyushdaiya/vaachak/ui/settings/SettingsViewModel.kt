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

    private val _geminiKey = MutableStateFlow("")
    val geminiKey = _geminiKey.asStateFlow()

    private val _cfUrl = MutableStateFlow("")
    val cfUrl = _cfUrl.asStateFlow()

    private val _cfToken = MutableStateFlow("")
    val cfToken = _cfToken.asStateFlow()
    // NEW: E-ink state
    private val _isEinkEnabled = MutableStateFlow(false)
    val isEinkEnabled = _isEinkEnabled.asStateFlow()

    init { loadSettings() }

    private fun loadSettings() = viewModelScope.launch {
        _geminiKey.value = settingsRepo.geminiKey.first()
        _cfUrl.value = settingsRepo.cloudflareUrl.first()
        _cfToken.value = settingsRepo.cloudflareToken.first()
        // Load from repository
        _isEinkEnabled.value = settingsRepo.isEinkEnabled.first()
    }

    fun updateGemini(valText: String) { _geminiKey.value = valText }
    fun updateCfUrl(valText: String) { _cfUrl.value = valText }
    fun updateCfToken(valText: String) { _cfToken.value = valText }
    // NEW: Toggle function
    fun toggleEink(enabled: Boolean) {
        _isEinkEnabled.value = enabled
    }
    fun saveSettings() = viewModelScope.launch {
        settingsRepo.saveSettings(_geminiKey.value, _cfUrl.value, _cfToken.value,_isEinkEnabled.value)
    }
    fun resetSettings() = viewModelScope.launch {
        settingsRepo.clearSettings()
        // Refresh local state to reflect empty values
        _geminiKey.value = ""
        _cfUrl.value = ""
        _cfToken.value = ""
        _isEinkEnabled.value = false
    }
    private val _isAutoSaveRecapsEnabled = MutableStateFlow(true) // Default to true
    val isAutoSaveRecapsEnabled = _isAutoSaveRecapsEnabled.asStateFlow()

    fun toggleAutoSaveRecaps(enabled: Boolean) {
        viewModelScope.launch {
            // Save to your Preferences DataStore/SharedPreferences
            settingsRepo.setAutoSaveRecaps(enabled)
            _isAutoSaveRecapsEnabled.value = enabled
        }
    }
    // Expose the theme as a StateFlow for the UI to observe
    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.E_INK
        )

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepo.setThemeMode(mode)
        }
    }
    val einkContrast = settingsRepo.einkContrast.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f)

    fun setContrast(value: Float) {
        viewModelScope.launch { settingsRepo.setContrast(value) }
    }

    // 1. Add the state flow for contrast

    // 2. Add the update function
    fun updateContrast(newContrast: Float) {
        viewModelScope.launch {
            settingsRepo.setContrast(newContrast)
        }
    }

    fun toggleEmbeddedDictionary(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setUseEmbeddedDictionary(enabled)
        }
    }
    val useEmbeddedDictionary: StateFlow<Boolean> = settingsRepo.getUseEmbeddedDictionary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dictionaryFolder: StateFlow<String> = settingsRepo.getDictionaryFolder() .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setUseEmbeddedDictionary(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setUseEmbeddedDictionary(enabled)
        }
    }
    fun updateDictionaryFolder(uri: String) {
        viewModelScope.launch {
            settingsRepo.setDictionaryFolder(uri)
        }
    }
}