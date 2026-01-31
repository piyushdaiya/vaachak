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

package io.github.piyushdaiya.vaachak.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle and operations of Readium publications.
 * Handles opening, closing, and retrieving metadata/assets from e-books.
 */
@Singleton
class ReadiumManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context,
            httpClient,
            assetRetriever,
            null
        )
    )

    private val _publication = MutableStateFlow<Publication?>(null)
    val publication = _publication.asStateFlow()

    /**
     * Opens an EPUB publication from a given URI.
     * Caches the file locally to optimize subsequent opens.
     *
     * @param uri The URI of the EPUB file to open.
     * @return The opened [Publication] object, or null if opening failed.
     */
    suspend fun openEpubFromUri(uri: Uri): Publication? {
        return try {
            // OPTIMIZATION: Use a hashed filename based on URI to cache book
            // This prevents re-copying the file on every open if it exists.
            val fileName = "book_${uri.toString().hashCode()}.epub"
            val tempFile = File(context.cacheDir, fileName)

            if (!tempFile.exists()) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val assetResult = assetRetriever.retrieve(tempFile)
            val asset = assetResult.getOrNull() ?: return null

            val pub = publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
            pub?.let {
                // Pre-calculate positions for accurate page numbers
                it.positions()
            }
            _publication.value = pub
            pub

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Closes the currently open publication and releases resources.
     */
    fun closePublication() {
        _publication.value?.close()
        _publication.value = null
    }

    /**
     * Retrieves the cover image of a publication.
     *
     * @param publication The publication to retrieve the cover from.
     * @return The cover [Bitmap], or null if not available.
     */
    suspend fun getPublicationCover(publication: Publication): Bitmap? {
        return try {
            publication.cover()
        } catch (e: Exception) {
            null
        }
    }
}