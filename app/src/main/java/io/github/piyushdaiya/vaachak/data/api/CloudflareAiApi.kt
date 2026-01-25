package io.github.piyushdaiya.vaachak.data.api

import io.github.piyushdaiya.vaachak.data.model.AiImageRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface CloudflareAiApi {
    @POST
    suspend fun generateImage(
        @Url fullUrl: String, // THE FIX: Accepts dynamic URL from settings
        @Body request: AiImageRequest,
        @Header("Authorization") token: String
    ): Response<ResponseBody>
}
