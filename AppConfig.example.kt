package io.github.piyushdaiya.vaachak

object AppConfig {
    // 1. Paste your Google Gemini 2.5 API Key here
    const val GEMINI_API_KEY = "your_gemini_key_here"

    // 2. Paste your Cloudflare Worker URL (Keep the trailing slash /)
    const val CLOUDFLARE_WORKER_URL = "[https://your-worker.workers.dev/](https://your-worker.workers.dev/)"

    // 3. Paste your Cloudflare API Token (if your worker requires authorization)
    const val CLOUDFLARE_AUTH_TOKEN = "your_cloudflare_token_here"
}
