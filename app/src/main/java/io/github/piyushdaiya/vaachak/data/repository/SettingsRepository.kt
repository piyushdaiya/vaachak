package io.github.piyushdaiya.vaachak.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit

import kotlinx.coroutines.flow.map

// Create the DataStore instance
private val Context.dataStore by preferencesDataStore(name = "user_settings")
val Context.settingsDataStore by preferencesDataStore(name = "settings")
val AUTO_SAVE_RECAPS_KEY = booleanPreferencesKey("auto_save_recaps")
@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    val isAutoSaveRecapsEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[AUTO_SAVE_RECAPS_KEY] ?: true
        }

    suspend fun setAutoSaveRecaps(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_SAVE_RECAPS_KEY] = enabled
        }
    }
    companion object {
        val GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val CF_URL = stringPreferencesKey("cloudflare_url")
        val CF_TOKEN = stringPreferencesKey("cloudflare_token")
        // NEW: Boolean key for E-ink preference
        val IS_EINK_ENABLED = booleanPreferencesKey("is_eink_enabled")
    }

    val geminiKey: Flow<String> = context.dataStore.data.map { it[GEMINI_KEY] ?: "" }
    val cloudflareUrl: Flow<String> = context.dataStore.data.map { it[CF_URL] ?: "" }
    val cloudflareToken: Flow<String> = context.dataStore.data.map { it[CF_TOKEN] ?: "" }
    // NEW: Flow for E-ink mode
    val isEinkEnabled: Flow<Boolean> = context.dataStore.data.map { it[IS_EINK_ENABLED] ?: false }
    suspend fun saveSettings(gemini: String, cfUrl: String, cfToken: String, isEnk: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[GEMINI_KEY] = gemini
            // Ensure URL ends with a slash for safety
            prefs[CF_URL] = if (cfUrl.isNotEmpty() && !cfUrl.endsWith("/")) "$cfUrl/" else cfUrl
            prefs[CF_TOKEN] = cfToken
            // Save E-ink preference
            prefs[IS_EINK_ENABLED] = isEnk
        }
    }
    suspend fun clearSettings() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

}

