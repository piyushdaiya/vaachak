package io.github.piyushdaiya.vaachak.data.repository

import android.util.Base64
import com.google.ai.client.generativeai.GenerativeModel
import io.github.piyushdaiya.vaachak.data.api.CloudflareAiApi
import io.github.piyushdaiya.vaachak.data.model.AiImageRequest
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AiRepository @Inject constructor(
    private val cloudflareApi: CloudflareAiApi,
    private val settingsRepo: SettingsRepository // Injected Settings
) {
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

    suspend fun visualizeText(selectedText: String): String {
        return try {
            val cfUrl = settingsRepo.cloudflareUrl.first()
            val cfToken = settingsRepo.cloudflareToken.first()

            if (cfUrl.isBlank()) throw Exception("Cloudflare URL missing in Settings.")

            val prompt = "Simple minimalist line art of $selectedText, black ink on white background, high contrast, no shading, clean lines."

            // Pass the dynamic URL and token
            val response = cloudflareApi.generateImage(cfUrl, AiImageRequest(prompt), "Bearer $cfToken")

            if (response.isSuccessful && response.body() != null) {
                val imageBytes = response.body()!!.bytes()
                if (imageBytes.isEmpty() || imageBytes[0].toInt().toChar() == '{') {
                    throw Exception("Cloudflare returned JSON error.")
                }
                "BASE64_IMAGE:" + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            } else {
                throw Exception("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            val cfError = e.message ?: "Network timeout"
            try {
                val fallbackText = getGeminiModel().generateContent("You are an artist. Describe '$selectedText' in vivid sensory detail. Under 50 words.").text
                "⚠️ Visualization Failed. Fallback Description:\n$fallbackText"
            } catch (geminiEx: Exception) {
                "Total Failure. Please check API Keys in Settings."
            }
        }
    }
    // --- NEW RECAP FUNCTIONALITY ---
    suspend fun generateRecap(
        bookTitle: String,
        highlightsContext: String,
        currentPageText: String
    ): String {
        return try {
            val prompt = """
                I am returning to read '$bookTitle'. 
                
                Based on my previous highlights:
                "$highlightsContext"
                
                And the current page text:
                "$currentPageText"
                
                Provide a quick 3-sentence recap of the plot leading up to this point and a brief 'Who's Who' of characters present in this specific scene.
                STRICTLY avoid future plot spoilers.
            """.trimIndent()

            getGeminiModel().generateContent(prompt).text ?: "Unable to generate recap."
        } catch (e: Exception) {
            "Error generating recap: ${e.localizedMessage}"
        }
    }
    suspend fun getRecallSummary(bookTitle: String, context: String): String {
        val prompt = """
        Context: $context
        Task: Provide a 2-sentence 'Where I left off' summary for '$bookTitle'. 
        Focus on the current plot tension and key characters present. 
        STRICTLY avoid future spoilers.
    """.trimIndent()
        return getGeminiModel().generateContent(prompt).text ?: "Summary unavailable."
    }
}

