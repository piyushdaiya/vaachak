package io.github.piyushdaiya.vaachak.ui.reader

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.readium.r2.shared.publication.Publication
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
    @param:ApplicationContext private val context: Context
) {
    // 1. Setup the HTTP Client
    private val httpClient = DefaultHttpClient()

    // 2. Setup the Asset Retriever
    private val assetRetriever = AssetRetriever(
        contentResolver = context.contentResolver,
        httpClient = httpClient
    )

    // 3. Setup the EPUB Parser (Passing null for PDF factory since we only need EPUB)
    private val publicationParser = DefaultPublicationParser(
        context = context,
        httpClient = httpClient,
        assetRetriever = assetRetriever,
        pdfFactory = null
    )

    // 4. Create the Opener (No named parameters needed here)
    private val opener = PublicationOpener(
        publicationParser
    )

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

            // In Readium 3.1.2, getOrNull() returns the Publication directly!
            opener.open(asset, allowUserInteraction = false).getOrNull()

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}