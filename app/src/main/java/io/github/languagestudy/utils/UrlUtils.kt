package io.github.languagestudy.utils

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
        .followRedirects(true)
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

    /**
     * Resolves short URLs (on.soundcloud.com, snd.sc, youtu.be) to their full canonical URLs
     * so they are stored consistently and go to the correct site.
     */
    suspend fun resolveCanonicalUrl(rawUrl: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(rawUrl)
                val host = uri.host ?: return@withContext rawUrl
                
                // YouTube short URL normalization
                val youtubeId = getYouTubeId(rawUrl)
                if (host == "youtu.be" && youtubeId != null) {
                    return@withContext "https://www.youtube.com/watch?v=$youtubeId"
                }

                // SoundCloud short URL resolution
                val isShortSoundCloud = host == "on.soundcloud.com" || host == "snd.sc"
                if (!isShortSoundCloud) return@withContext rawUrl

                val request = Request.Builder()
                    .url(rawUrl)
                    .head() // Use HEAD to just get the redirect location
                    .build()

                client.newCall(request).execute().use { response ->
                    val resolvedUrl = response.request.url.toString()
                    // Filter out API URLs if it somehow redirected there, but usually it goes to the track page
                    if (resolvedUrl.contains("api.soundcloud.com")) {
                        rawUrl
                    } else {
                        resolvedUrl
                    }
                }
            } catch (e: Exception) {
                rawUrl
            }
        }
    }
}
