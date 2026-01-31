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
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses and queries StarDict dictionary files.
 * Supports .ifo, .idx, and .dict (including compressed .dict.dz) files.
 */
@Singleton
class StarDictParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Cache for dictionary metadata to avoid repeated folder scanning
    private var cachedDictionaries: List<StarDictMetadata>? = null
    private var lastLoadedFolder: String? = null

    // Cache to prevent re-searching words that definitely don't exist
    private val failedLookups = LinkedHashMap<String, Boolean>(100, 0.75f, true)

    data class StarDictMetadata(
        val baseName: String,
        val idxUri: Uri,
        val dictUri: Uri,
        val cdiUri: Uri?,
        val isCompressed: Boolean,
        val offsetBits: Int
    )

    data class IdxEntry(val offset: Long, val size: Int)

    /**
     * Scans the folder and caches metadata including offset bit-size (32 vs 64).
     */
    private fun ensureDictionariesLoaded(folderUriString: String) {
        if (cachedDictionaries != null && lastLoadedFolder == folderUriString) return

        // NEW: Clear the failed lookups cache because the source data has changed
        failedLookups.clear()

        val root = DocumentFile.fromTreeUri(context, Uri.parse(folderUriString)) ?: return
        val foundDicts = mutableListOf<StarDictMetadata>()

        // Find all master .ifo files
        root.listFiles().filter { it.name?.endsWith(".ifo") == true }.forEach { ifoFile ->
            val baseName = ifoFile.name?.removeSuffix(".ifo") ?: return@forEach
            val idxFile = root.findFile("$baseName.idx")
            val dictFile = root.findFile("$baseName.dict") ?: root.findFile("$baseName.dict.dz")
            val cdiFile = root.findFile("$baseName.cdi") // Optional category index

            if (idxFile != null && dictFile != null) {
                val bits = getIdxOffsetBits(ifoFile.uri)
                foundDicts.add(
                    StarDictMetadata(
                        baseName = baseName,
                        idxUri = idxFile.uri,
                        dictUri = dictFile.uri,
                        cdiUri = cdiFile?.uri,
                        isCompressed = dictFile.name?.endsWith(".dz") == true,
                        offsetBits = bits
                    )
                )
            }
        }

        cachedDictionaries = foundDicts
        lastLoadedFolder = folderUriString
        Log.d("StarDictParser", "Cached ${foundDicts.size} dictionaries from $folderUriString")
    }

    /**
     * Core lookup function: iterates through all local dictionaries.
     *
     * @param folderUriString The URI of the folder containing dictionary files.
     * @param word The word to look up.
     * @return The definition of the word, or null if not found.
     */
    suspend fun lookup(folderUriString: String, word: String): String? = withContext(Dispatchers.IO) {
        ensureDictionariesLoaded(folderUriString)
        val cleanTarget = word.trim().lowercase()

        if (failedLookups.containsKey(cleanTarget)) return@withContext null

        cachedDictionaries?.forEach { dict ->
            try {
                // 1. Search main index (.idx)
                var entry = findWordInIdx(dict.idxUri, cleanTarget, dict.offsetBits)

                // 2. Fallback: Search category index (.cdi) if word not in main
                if (entry == null && dict.cdiUri != null) {
                    entry = findWordInIdx(dict.cdiUri, cleanTarget, dict.offsetBits)
                }

                if (entry != null) {
                    val raw = readFromDict(dict.dictUri, entry.offset, entry.size, dict.isCompressed)
                    if (!raw.isNullOrBlank()) {
                        return@withContext polishDefinition(raw, dict.baseName)
                    }
                }
            } catch (e: Exception) {
                Log.e("StarDictParser", "Error searching ${dict.baseName}: ${e.message}")
            }
        }

        failedLookups[cleanTarget] = true
        null
    }

    /**
     * Performs binary-like stream search in .idx or .cdi files.
     */
    private fun findWordInIdx(uri: Uri, target: String, offsetBits: Int): IdxEntry? {
        // Explicitly define the return type for the .use block to avoid inference errors
        return context.contentResolver.openInputStream(uri)?.use { rawInput ->
            val bis = BufferedInputStream(rawInput)
            val wordBuffer = ByteArray(256)
            val offsetBytes = offsetBits / 8
            val dataBuffer = ByteArray(offsetBytes + 4)

            var result: IdxEntry? = null

            while (true) {
                var byteIdx = 0
                var b = bis.read()
                if (b == -1) break

                // 1. Read null-terminated word string
                while (b != 0 && b != -1) {
                    if (byteIdx < 255) wordBuffer[byteIdx++] = b.toByte()
                    b = bis.read()
                }
                val word = String(wordBuffer, 0, byteIdx, Charsets.UTF_8)

                // 2. Read binary data: Offset (4 or 8 bytes) + Size (4 bytes)
                val bytesRead = bis.read(dataBuffer)
                if (bytesRead < dataBuffer.size) break

                // 3. Comparison
                if (word.lowercase() == target) {
                    val offset = if (offsetBits == 64) {
                        readBigEndianLong(dataBuffer, 0)
                    } else {
                        readBigEndianInt(dataBuffer, 0).toLong()
                    }
                    val size = readBigEndianInt(dataBuffer, offsetBytes)
                    result = IdxEntry(offset, size)
                    break // Found it, exit the while loop
                }
            }
            result // This is the value returned by the .use block
        }
    }

    /**
     * Extracts data from .dict or .dict.dz using skip and read.
     */
    private fun readFromDict(uri: Uri, offset: Long, size: Int, isCompressed: Boolean): String? {
        return try {
            val contentResolver = context.contentResolver
            contentResolver.openInputStream(uri)?.use { input ->
                if (isCompressed) {
                    // Reverted to traditional constructor to fix 'builder' error
                    // Passing 'true' for decompressConcatenated
                    val gzis = GzipCompressorInputStream(input, true)
                    gzis.use { stream ->
                        stream.skip(offset)
                        val buffer = ByteArray(size)
                        stream.read(buffer)
                        String(buffer, Charsets.UTF_8)
                    }
                } else {
                    input.skip(offset)
                    val buffer = ByteArray(size)
                    input.read(buffer)
                    String(buffer, Charsets.UTF_8)
                }
            }
        } catch (e: Exception) {
            Log.e("StarDictParser", "Dict read error: ${e.message}")
            null
        }
    }

    private fun polishDefinition(text: String, sourceName: String): String {
        return text
            .replace("\n", "<br/>")
            .replace(Regex("\\[(.*?)\\]"), "<b>[$1]</b>")
            .plus("<br/><br/><small color='gray'><i>Source: $sourceName</i></small>")
    }

    private fun getIdxOffsetBits(ifoUri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(ifoUri)?.bufferedReader()?.use { reader ->
                // FIX: removed .use on the find result. .use belongs on the reader.
                val line = reader.lineSequence().find { it.startsWith("idxoffsetbits") }
                line?.split("=")?.getOrNull(1)?.trim()?.toInt() ?: 32
            } ?: 32
        } catch (e: Exception) {
            32
        }
    }

    private fun readBigEndianLong(bytes: ByteArray, pos: Int): Long {
        return (bytes[pos].toLong() and 0xFF shl 56) or
                (bytes[pos + 1].toLong() and 0xFF shl 48) or
                (bytes[pos + 2].toLong() and 0xFF shl 40) or
                (bytes[pos + 3].toLong() and 0xFF shl 32) or
                (bytes[pos + 4].toLong() and 0xFF shl 24) or
                (bytes[pos + 5].toLong() and 0xFF shl 16) or
                (bytes[pos + 6].toLong() and 0xFF shl 8) or
                (bytes[pos + 7].toLong() and 0xFF)
    }

    private fun readBigEndianInt(bytes: ByteArray, pos: Int): Int {
        return (bytes[pos].toInt() and 0xFF shl 24) or
                (bytes[pos + 1].toInt() and 0xFF shl 16) or
                (bytes[pos + 2].toInt() and 0xFF shl 8) or
                (bytes[pos + 3].toInt() and 0xFF)
    }
}