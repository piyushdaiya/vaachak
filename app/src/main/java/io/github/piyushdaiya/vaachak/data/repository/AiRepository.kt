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

import android.util.Base64
import com.google.ai.client.generativeai.GenerativeModel
import io.github.piyushdaiya.vaachak.data.api.CloudflareAiApi
import io.github.piyushdaiya.vaachak.data.model.AiImageRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AiRepository @Inject constructor(
    private val settingsRepo: SettingsRepository
) {
    // 1. Setup Client with 30s Timeout (-m 30)
    private val cloudflareClient = OkHttpClient.Builder()
        .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // 2. Setup Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cloudflare.com/") // Placeholder
        .client(cloudflareClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val cloudflareApi = retrofit.create(CloudflareAiApi::class.java)

    // Helper to get Gemini
    private suspend fun getGeminiModel(): GenerativeModel {
        val key = settingsRepo.geminiKey.first()
        if (key.isBlank()) throw Exception("Gemini API Key is missing.")
        return GenerativeModel(modelName = "gemini-2.5-flash", apiKey = key)
    }

    suspend fun explainContext(selectedText: String): String {
        return try {
            val prompt = "Provide a simple, 2-sentence explanation for: $selectedText"
            getGeminiModel().generateContent(prompt).text ?: "No explanation."
        } catch (e: Exception) { "Error: ${e.localizedMessage}" }
    }

    suspend fun whoIsThis(selectedText: String, bookTitle: String, bookAuthor: String): String {
        return try {
            val prompt = "Reading '$bookTitle' by $bookAuthor. Identify character '$selectedText' without spoilers."
            getGeminiModel().generateContent(prompt).text ?: "No identification."
        } catch (e: Exception) { "Error: ${e.localizedMessage}" }
    }

    // --- FIX: Using correct variable names (cfUrl / cfToken) ---
    suspend fun visualizeText(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            // FIX 1: Referenced correct flow names from SettingsRepository
            val url = settingsRepo.cfUrl.first().trim()
            val token = settingsRepo.cfToken.first().trim()

            // FIX 2: Ensure we aren't sending empty requests
            if (url.isEmpty() || token.isEmpty()) {
                return@withContext "Error: Cloudflare settings are missing."
            }

            val response = cloudflareApi.generateImage(
                fullUrl = url,
                request = AiImageRequest(prompt),
                token = "Bearer $token"
            )

            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null) {
                    "BASE64_IMAGE:${Base64.encodeToString(bytes, Base64.DEFAULT)}"
                } else "Empty Body"
            } else {
                "Cloudflare Error: ${response.code()}"
            }
        } catch (e: java.net.SocketTimeoutException) {
            "Timeout: Cloudflare took >30s. Try again."
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

        suspend fun getQuickRecap(bookTitle: String, context: String): String {
        return try {
            val prompt = """
                I am reading book '$bookTitle'. And the current page text: "$context"
                
                Provide a quick 3-sentence recap of the plot leading up to this point and a brief 'Who's Who' of characters present in this specific scene.
                STRICTLY avoid future plot spoilers.
            """.trimIndent()
            getGeminiModel().generateContent(prompt).text ?: "Summary unavailable."
        } catch (e: Exception) { "Error: ${e.localizedMessage}" }
    }

    suspend fun getRecallSummary(
        bookTitle: String,
        highlightsContext: String
    ): String {
        return try {
            val prompt = """
                I am returning to read '$bookTitle'. 
                
                Based on my previous highlights:
                "$highlightsContext"
                               
                Provide a quick 3-sentence recap of the plot leading up to this point and a brief 'Who's Who' of characters until now.
                STRICTLY avoid future plot spoilers.
            """.trimIndent()

            getGeminiModel().generateContent(prompt).text ?: "Unable to generate recap."
        } catch (e: Exception) {
            "Error generating recap: ${e.localizedMessage}"
        }
    }
}