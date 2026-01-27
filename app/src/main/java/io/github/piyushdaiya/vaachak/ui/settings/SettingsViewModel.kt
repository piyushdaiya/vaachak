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
}