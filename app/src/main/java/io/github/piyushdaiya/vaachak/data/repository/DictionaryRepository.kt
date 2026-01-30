package io.github.piyushdaiya.vaachak.data.repository

import android.util.Log
import io.github.piyushdaiya.vaachak.core.dictionary.JsonDictionaryProvider
import io.github.piyushdaiya.vaachak.core.dictionary.StarDictParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DictionaryRepository @Inject constructor(
    private val jsonProvider: JsonDictionaryProvider,
    private val starDictParser: StarDictParser,
    private val settingsRepository: SettingsRepository
) {
    suspend fun getDefinition(word: String): String? = withContext(Dispatchers.IO) {
        // 1. Check if user wants to use embedded dictionary
        if (settingsRepository.getUseEmbeddedDictionary().first()) {
            val internal = jsonProvider.lookup(word)
            if (internal != null){
                Log.d("VaachakDictDebug", "Word '$word'  found in internal JSON dictionary with definition '$internal'")
                return@withContext internal
            }
            Log.d("VaachakDictDebug", "Word '$word' not found in internal JSON dictionary")
        }

        // 2. Fallback to StarDict if configured
        val folderUri = settingsRepository.getDictionaryFolder().first()
        if (folderUri.isNotEmpty()) {
            Log.d("VaachakDictDebug", "Searching external StarDict folder...")
            val externalResult = starDictParser.lookup(folderUri, word)
            if (externalResult != null) {
                Log.d("VaachakDictDebug", "Found in external StarDict folder with definition '$externalResult'")
                return@withContext externalResult // Correct labeled return
            }
        }

        null // Final return for the block if nothing is found
    }
}
