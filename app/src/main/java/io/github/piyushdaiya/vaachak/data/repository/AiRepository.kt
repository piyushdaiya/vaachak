package io.github.piyushdaiya.vaachak.data.repository

import android.util.Base64
import com.google.ai.client.generativeai.GenerativeModel
import io.github.piyushdaiya.vaachak.data.api.CloudflareAiApi
import io.github.piyushdaiya.vaachak.data.model.AiImageRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AiRepository @Inject constructor(
        private val settingsRepo: SettingsRepository // Injected Settings
) {
    val cloudflareClient = OkHttpClient.Builder()
        .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // The -m 30 flag
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS) // Time to establish connection
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // Time to wait for image data
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cloudflare.com/") // Placeholder, overridden by @Url
        .client(cloudflareClient) // ATTACH THE 30s TIMEOUT HERE
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val cloudflareApi = retrofit.create(CloudflareAiApi::class.java)
    // Helper to get the Gemini model with the latest key
    private suspend fun getGeminiModel(): GenerativeModel {
        val key = settingsRepo.geminiKey.first()
        if (key.isBlank()) throw Exception("Gemini API Key is missing. Please add it in Settings.")
        return GenerativeModel(modelName = "gemini-2.5-flash", apiKey = key)
    }

    suspend fun explainContext(selectedText: String): String {
        return try {
            val prompt = "Provide a simple, 2-sentence explanation for the term: $selectedText"
            getGeminiModel().generateContent(prompt).text ?: "No explanation."
        } catch (e: Exception) { "Error: ${e.localizedMessage}" }
    }

    suspend fun whoIsThis(selectedText: String, bookTitle: String, bookAuthor: String): String {
        return try {
            val prompt = "I am currently reading '$bookTitle' by $bookAuthor. I have not finished the bookIdentify the character '$selectedText'. STRICTLY avoid future plot spoilers."
            getGeminiModel().generateContent(prompt).text ?: "No identification."
        } catch (e: Exception) { "Error: ${e.localizedMessage}" }
    }

    suspend fun visualizeText(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val url = settingsRepo.cloudflareUrl.first().trim()
            val token = settingsRepo.cloudflareToken.first().trim()

            val response = cloudflareApi.generateImage(
                fullUrl = url,
                request = AiImageRequest(prompt),
                token = "Bearer $token"
            )

            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null) {
                    "BASE64_IMAGE:${android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)}"
                } else "Empty Body"
            } else {
                "Cloudflare Error: ${response.code()}"
            }
        } catch (e: java.net.SocketTimeoutException) {
            // This is triggered by the 30s limit
            "Timeout: Cloudflare took more than 30 seconds to generate the image."
        } catch (e: Exception) {
            "Total Failure: ${e.localizedMessage}"
        }
    }
    suspend fun getRecallSummary(bookTitle: String, context: String): String {
        val prompt = """
        Context: $context
        Task: Provide a 3-sentence 'Where I left off' summary for '$bookTitle'. 
        Focus on the current plot tension and key characters present and a brief 'Who's Who' of characters present
        STRICTLY avoid future spoilers.
    """.trimIndent()
        return getGeminiModel().generateContent(prompt).text ?: "Summary unavailable."
    }
}

