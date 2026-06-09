package com.example.languagestudy.utils

import android.net.Uri
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

object UrlUtils {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    fun getYouTubeId(url: String): String? {
        val regex = "(?:youtube(?:-nocookie)?\\.com\\/(?:.*[?&]v=|(?:v|embed|shorts)\\/)|youtu\\.be\\/)([\\w-]{11})".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }

    fun isSoundCloudUrl(url: String): Boolean {
        val regex = "^https?:\\/\\/(soundcloud\\.com|snd\\.sc|on\\.soundcloud\\.com)\\/.*".toRegex()
        return regex.matches(url)
    }

    fun getPortfolioType(url: String): String? {
        if (getYouTubeId(url) != null) return "youtube"
        if (isSoundCloudUrl(url)) return "soundcloud"
        return null
    }

    fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;")
    }

    fun sanitizeHttpUrl(url: String?): String? {
        if (url == null) return null
        return try {
            val parsed = URL(url)
            if (parsed.protocol == "http" || parsed.protocol == "https") {
                parsed.toString()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private data class SoundCloudOEmbed(val html: String?)

    suspend fun resolveSoundCloudPortfolioLink(rawUrl: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(rawUrl)
                val host = uri.host ?: return@withContext rawUrl
                val needsResolve = host.contains("on.soundcloud.com") || host.contains("snd.sc")
                if (!needsResolve) return@withContext rawUrl

                val oEmbedUrl = "https://soundcloud.com/oembed?format=json&url=${Uri.encode(rawUrl)}&iframe=true"
                val request = Request.Builder().url(oEmbedUrl).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext rawUrl
                    
                    val body = response.body?.string() ?: return@withContext rawUrl
                    val data = gson.fromJson(body, SoundCloudOEmbed::class.java)
                    
                    val html = data.html ?: return@withContext rawUrl
                    val srcRegex = "src=\"([^\"]+)\"".toRegex()
                    val srcMatch = srcRegex.find(html)
                    val src = srcMatch?.groupValues?.get(1) ?: return@withContext rawUrl
                    
                    val playerUri = Uri.parse(src)
                    val resolved = playerUri.getQueryParameter("url")
                    resolved ?: rawUrl
                }
            } catch (e: Exception) {
                rawUrl
            }
        }
    }
}
