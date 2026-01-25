package io.github.piyushdaiya.vaachak.data.model

import com.google.gson.annotations.SerializedName

data class AiImageRequest(
    val prompt: String,
    val width: Int = 384,
    val height: Int = 512,
    @SerializedName("num_inference_steps") val numInferenceSteps: Int = 20,
    @SerializedName("guidance_scale") val guidanceScale: Int = 7,
    val scheduler: String = "DPM++ 2M Karras"
)

