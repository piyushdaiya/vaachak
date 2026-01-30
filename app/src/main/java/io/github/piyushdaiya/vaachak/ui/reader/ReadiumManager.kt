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

    fun closePublication() {
        _publication.value?.close()
        _publication.value = null
    }

    suspend fun getPublicationCover(publication: Publication): Bitmap? {
        return try {
            publication.cover()
        } catch (e: Exception) {
            null
        }
    }
}