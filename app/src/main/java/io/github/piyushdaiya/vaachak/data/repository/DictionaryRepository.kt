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

import android.util.Log
import io.github.piyushdaiya.vaachak.core.dictionary.JsonDictionaryProvider
import io.github.piyushdaiya.vaachak.core.dictionary.StarDictParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Repository for fetching word definitions from various dictionary sources.
 * Supports both an internal JSON dictionary and external StarDict dictionaries.
 */
@Singleton
class DictionaryRepository @Inject constructor(
    private val jsonProvider: JsonDictionaryProvider,
    private val starDictParser: StarDictParser,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Retrieves a definition from the internal JSON dictionary.
     * Only performs lookup if the embedded dictionary setting is enabled.
     *
     * @param word The word to define.
     * @return The definition if found, or null otherwise.
     */
    suspend fun getjsonDefinition(word: String): String? = withContext(Dispatchers.IO) {
        // 1. Check if user wants to use embedded dictionary
        if (settingsRepository.getUseEmbeddedDictionary().first()) {
            val internal = jsonProvider.lookup(word)
            if (internal != null){
                Log.d("VaachakDictDebug", "Word '$word'  found in internal JSON dictionary with definition '$internal'")
                return@withContext internal
            }
            Log.d("VaachakDictDebug", "Word '$word' not found in internal JSON dictionary")
        }
        null // Final return for the block if nothing is found
    }

    /**
     * Retrieves a definition for a word, prioritizing external StarDict dictionaries.
     * If not found in StarDict (or if not configured), falls back to the internal JSON dictionary.
     *
     * @param word The word to define.
     * @return The definition if found in either source, or null otherwise.
     */
    suspend fun getDefinition(word: String): String? = withContext(Dispatchers.IO) {
        // 1. Check if user wants to use embedded dictionary
        if (settingsRepository.getUseEmbeddedDictionary().first()) {
            //Check in StarDict
            val folderUri = settingsRepository.getDictionaryFolder().first()
            if (folderUri.isNotEmpty()) {
                Log.d("VaachakDictDebug", "Searching external StarDict folder...")
                val externalResult = starDictParser.lookup(folderUri, word)
                if (externalResult != null) {
                    Log.d("VaachakDictDebug", "Found in external StarDict folder with definition '$externalResult'")
                    return@withContext externalResult // Correct labeled return
                }
            }
        }
        //Fallback to Json
        val internal = jsonProvider.lookup(word)
        if (internal != null){
            Log.d("VaachakDictDebug", "Word '$word'  found in internal JSON dictionary with definition '$internal'")
            return@withContext internal
        }
        Log.d("VaachakDictDebug", "Word '$word' not found in internal JSON dictionary- External fallback used")



        null // Final return for the block if nothing is found
    }
}