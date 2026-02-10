package io.github.piyushdaiya.vaachak.data.repository

import com.google.gson.Gson
import io.github.piyushdaiya.vaachak.data.remote.GutendexResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GutendexRepository @Inject constructor() {

    // Timeout of 60s for slow topic searches
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Fetches books from the provided absolute URL.
     * The URL is now constructed by the ViewModel, allowing for custom domains.
     */
    suspend fun fetchBooks(url: String): Result<GutendexResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gutendex Error: ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty Body"))
            val data = gson.fromJson(body, GutendexResponse::class.java)

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBook(url: String, destination: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful && response.body != null) {
                response.body!!.byteStream().use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}