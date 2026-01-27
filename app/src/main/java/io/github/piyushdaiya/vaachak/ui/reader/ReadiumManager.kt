package io.github.piyushdaiya.vaachak.ui.reader

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import android.graphics.Bitmap
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.publication.services.positions


@Singleton
class ReadiumManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)

    private val publicationOpener = PublicationOpener(
        publicationParser = DefaultPublicationParser(
            context,
            httpClient,assetRetriever,
            null
        )
    )

    // THE CRITICAL PART: This variable MUST be named 'publication' and be public
    private val _publication = MutableStateFlow<Publication?>(null)
    val publication = _publication.asStateFlow()

    suspend fun openEpubFromUri(uri: Uri): Publication? {
        return try {
            val tempFile = File(context.cacheDir, "current_book.epub")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val assetResult = assetRetriever.retrieve(tempFile)
            val asset = assetResult.getOrNull() ?: return null

            val pub = publicationOpener.open(asset, allowUserInteraction = false).getOrNull()
            pub?.let {
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

    suspend fun getPublicationCover(publication: org.readium.r2.shared.publication.Publication): Bitmap? {
        return try {
            // This is the suspending Readium 3.1.2 service call
            publication.cover()
        } catch (e: Exception) {
            null
        }
    }
}