package io.github.piyushdaiya.vaachak.data.repository

import io.github.piyushdaiya.vaachak.data.local.OpdsDao
import io.github.piyushdaiya.vaachak.data.local.OpdsEntity
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Try.Success
import org.readium.r2.shared.util.Try.Failure
import org.readium.r2.opds.OPDS1Parser
import org.readium.r2.opds.OPDS2Parser
import org.readium.r2.shared.opds.ParseData
import org.readium.r2.shared.util.AbsoluteUrl
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import android.util.Log

@Singleton
class OpdsRepository @Inject constructor(
    private val opdsDao: OpdsDao
) {
    // User Agents
    private val UA_CHROME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val UA_CALIBRE = "Stanza/3.2 iPhone/5.1"

    // In-Memory Cookie Store
    private val cookieStore = HashMap<String, List<Cookie>>()

    // --- CLIENTS ---
    private val safeClient: OkHttpClient = createClient(unsafe = false)
    private val unsafeClient: OkHttpClient = createClient(unsafe = true)

    private fun createClient(unsafe: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val existing = cookieStore[url.host] ?: emptyList()
                    val updated = ArrayList(existing)
                    updated.addAll(cookies)
                    cookieStore[url.host] = updated
                }
                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    return cookieStore[url.host] ?: emptyList()
                }
            })
            // Browser Headers Interceptor
            .addNetworkInterceptor(Interceptor { chain ->
                val original = chain.request()
                val url = original.url.toString()

                // Calibre Detection (Local IP)
                val isLocal = url.contains("192.168.") || url.contains("10.") || url.contains(".local")
                val userAgent = if (isLocal) UA_CALIBRE else UA_CHROME

                val requestBuilder = original.newBuilder()
                    .header("User-Agent", userAgent)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,application/opds+json,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")

                chain.proceed(requestBuilder.build())
            })

        if (unsafe) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) { e.printStackTrace() }
        }
        return builder.build()
    }

    // --- SMART CONFIG LOOKUP ---
    private suspend fun findEffectiveFeed(urlStr: String): OpdsEntity? {
        val cleanUrl = urlStr.trim()
        var feed = opdsDao.getFeedByUrl(cleanUrl)
            ?: opdsDao.getFeedByUrl(cleanUrl.removeSuffix("/"))
            ?: opdsDao.getFeedByUrl("$cleanUrl/")
        if (feed != null) return feed

        if (cleanUrl.contains("/opds")) {
            val base = cleanUrl.substringBefore("/opds") + "/opds"
            feed = opdsDao.getFeedByUrl(base) ?: opdsDao.getFeedByUrl("$base/")
            if (feed != null) return feed
        }

        try {
            val host = java.net.URL(cleanUrl).host
            if (host.startsWith("192.168.") || host.startsWith("10.") || host.endsWith(".local")) {
                return OpdsEntity(title = "Local", url = cleanUrl, allowInsecure = true)
            }
        } catch (e: Exception) {}
        return null
    }

    private suspend fun executeRequest(url: String): Response {
        val storedFeed = findEffectiveFeed(url)
        val allowInsecure = storedFeed?.allowInsecure ?: false
        val client = if (allowInsecure) unsafeClient else safeClient

        val requestBuilder = Request.Builder().url(url)

        if (storedFeed?.username != null && storedFeed.password != null) {
            requestBuilder.header("Authorization", Credentials.basic(storedFeed.username, storedFeed.password))
        }

        return withContext(Dispatchers.IO) {
            client.newCall(requestBuilder.build()).execute()
        }
    }

    // --- PARSER LOGIC ---
    suspend fun parseFeed(rawUrl: String): Try<ParseData, Exception> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = rawUrl.trim()
            val absoluteUrl = AbsoluteUrl(cleanUrl)
                ?: return@withContext Try.Failure<ParseData, Exception>(Exception("Invalid URL"))

            val response = try {
                executeRequest(cleanUrl)
            } catch (e: Exception) {
                return@withContext Try.Failure<ParseData, Exception>(e)
            }

            if (!response.isSuccessful) {
                val msg = "HTTP ${response.code}: ${response.message}"
                Log.e("OpdsRepository", msg)
                response.close()
                return@withContext Try.Failure<ParseData, Exception>(Exception(msg))
            }

            val bodyBytes = response.body?.bytes()
            response.close()

            if (bodyBytes == null || bodyBytes.isEmpty()) {
                return@withContext Try.Failure<ParseData, Exception>(Exception("Empty Response"))
            }

            val contentStr = String(bodyBytes, Charset.forName("UTF-8")).trim()
            if (contentStr.contains("<!DOCTYPE html", true) || contentStr.contains("<html", true)) {
                return@withContext Try.Failure<ParseData, Exception>(Exception("Server returned HTML (Login/Block Page)."))
            }

            // Apply ManyBooks Fix
            val safeBytes = sanitizeXml(bodyBytes)

            // 1. Try OPDS 2.0 (JSON)
            try {
                val opds2 = OPDS2Parser.parse(safeBytes, absoluteUrl)
                if (opds2 is ParseData) return@withContext Try.Success(opds2)
                if (opds2 is Success<*, *>) return@withContext Try.Success(opds2.value as ParseData)
            } catch (_: Exception) {}

            // 2. Try OPDS 1.x (XML)
            try {
                // BOM Stripping
                val cleanBytes = if (safeBytes.size >= 3 && safeBytes[0] == 0xEF.toByte() && safeBytes[1] == 0xBB.toByte() && safeBytes[2] == 0xBF.toByte()) {
                    safeBytes.copyOfRange(3, safeBytes.size)
                } else safeBytes

                val result = OPDS1Parser.parse(cleanBytes, absoluteUrl)

                return@withContext when (result) {
                    is ParseData -> Try.Success(result)
                    is Success<*, *> -> Try.Success(result.value as ParseData)
                    is Failure<*, *> -> Try.Failure(Exception("XML Parser Failed: ${result.value}"))
                    else -> {
                        if (result is ParseData) Try.Success(result)
                        else Try.Failure(Exception("Unknown Parser State"))
                    }
                }
            } catch (e: Exception) {
                return@withContext Try.Failure<ParseData, Exception>(Exception("XML Error: ${e.message}"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Try.Failure<ParseData, Exception>(e)
        }
    }

    // --- DOWNLOADER ---
    suspend fun downloadPublication(downloadUrl: String, destinationFile: File, originalFeedUrl: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val feedUrlForConfig = originalFeedUrl ?: downloadUrl
            val storedFeed = findEffectiveFeed(feedUrlForConfig)
            val allowInsecure = storedFeed?.allowInsecure ?: false
            val client = if (allowInsecure) unsafeClient else safeClient

            val requestBuilder = Request.Builder().url(downloadUrl)

            if (storedFeed?.username != null && storedFeed.password != null) {
                requestBuilder.header("Authorization", Credentials.basic(storedFeed.username, storedFeed.password))
            }

            val response = client.newCall(requestBuilder.build()).execute()

            if (response.isSuccessful && response.body != null) {
                val inputStream = response.body!!.byteStream()
                val outputStream = FileOutputStream(destinationFile)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                response.close()
                return@withContext true
            } else {
                response.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    // --- HELPER: Fix Bad XML (ManyBooks) ---
    private fun sanitizeXml(rawXml: ByteArray): ByteArray {
        try {
            val xmlString = String(rawXml, Charset.forName("UTF-8"))

            // 1. Remove XHTML Namespace in titles (ManyBooks specific issue)
            // Replaces <title...><div...>Title</div></title> with <title>Title</title>
            var cleanXml = xmlString.replace(Regex("<title[^>]*>\\s*<div[^>]*>(.*?)</div>\\s*</title>", RegexOption.DOT_MATCHES_ALL)) {
                "<title>${it.groupValues[1].trim()}</title>"
            }

            return cleanXml.toByteArray(Charset.forName("UTF-8"))
        } catch (e: Exception) {
            // If text conversion fails, return original bytes
            return rawXml
        }
    }
}