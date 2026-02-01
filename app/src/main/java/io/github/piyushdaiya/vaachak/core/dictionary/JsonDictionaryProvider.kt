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

package io.github.piyushdaiya.vaachak.core.dictionary

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonDictionaryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DictionaryProvider {

    private val jsonHandler = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var dictionaryMap: Map<String, JsonElement>? = null
    private var inflectionMap: Map<String, String>? = null
    private fun isEntryEmpty(element: JsonElement): Boolean {
        return try {
            val meanings = element.jsonObject["MEANINGS"]?.jsonArray
            meanings == null || meanings.isEmpty()
        } catch (e: Exception) {
            true
        }
    }
    override suspend fun lookup(word: String): String? = withContext(Dispatchers.IO) {
        try {
            ensureLoaded()

            // 1. Enhanced Cleaning: Remove hidden unicode characters and whitespace
            val cleanWord = word.trim().replace(Regex("[^\\p{L}\\p{N}]"), "").lowercase()

            // 2. Initial Lemma Lookup
            val lemma = inflectionMap?.get(cleanWord) ?: cleanWord

            // 3. Dictionary Lookup
            var entry = dictionaryMap?.get(lemma.uppercase())

            // 4. Fallback: If we found an entry but it's empty, try the lemma again
            // or if we didn't find the inflected form, try the root.
            if (entry == null || isEntryEmpty(entry)) {
                val fallbackLemma = inflectionMap?.get(cleanWord)
                if (fallbackLemma != null) {
                    entry = dictionaryMap?.get(fallbackLemma.uppercase())
                }
            }

            val definition = entry?.let { formatDefinition(it) }

            // Only return if the definition actually contains text
            if (!definition.isNullOrBlank()) {
                Log.d("VaachakDictDebug", "Word '$word' found with definition: ${definition.take(50)}...")
                return@withContext definition
            }

            Log.d("VaachakDictDebug", "Word '$word' (lemma: $lemma) has no valid definition in JSON")
            null
        } catch (e: Exception) {
            Log.e("VaachakDict", "JSON Lookup failed", e)
            null
        }
    }

    private fun ensureLoaded() {
        if (dictionaryMap == null) {
            val dictJson = context.assets.open("dictionary.json").bufferedReader().use { it.readText() }
            dictionaryMap = jsonHandler.decodeFromString<Map<String, JsonElement>>(dictJson)
        }
        if (inflectionMap == null) {
            val inflJson = context.assets.open("inflections.json").bufferedReader().use { it.readText() }
            inflectionMap = jsonHandler.decodeFromString<Map<String, String>>(inflJson)
        }
    }

    private fun formatDefinition(element: JsonElement): String {
        return try {
            // Based on your merged.json: "WORD": { "MEANINGS": [[Type, Def, Syns, Examples], ...] }
            val meanings = element.jsonObject["MEANINGS"]?.jsonArray
                ?: return "No meanings listed for this word."

            meanings.joinToString("\n\n") { item ->
                val parts = item.jsonArray
                val type = parts.getOrNull(0)?.jsonPrimitive?.content ?: ""
                val definition = parts.getOrNull(1)?.jsonPrimitive?.content ?: ""
                "<b>($type)</b> $definition" // Using B for bold to help e-ink contrast
            }
        } catch (e: Exception) {
            Log.e("VaachakDict", "Format error: ${e.message}")
            "Format error in dictionary data."
        }
    }
}