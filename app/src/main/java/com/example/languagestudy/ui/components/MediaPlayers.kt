package com.example.languagestudy.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun YouTubePlayer(
    videoId: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            YouTubePlayerView(context).apply {
                addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                })
            }
        }
    )
}

@Composable
fun SoundCloudPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        val uriString = uri.toString()
                        
                        // Handle SoundCloud app intents or specific deep links
                        if (uriString.startsWith("intent://") || uriString.startsWith("soundcloud://")) {
                            try {
                                val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
                                if (intent != null) {
                                    // Set CATEGORY_BROWSABLE to be safe for deep links
                                    intent.addCategory(Intent.CATEGORY_BROWSABLE)
                                    // Remove component to allow any app (browser or soundcloud) to handle it
                                    intent.component = null
                                    
                                    val packageManager = context.packageManager
                                    val info = packageManager.resolveActivity(intent, 0)
                                    if (info != null) {
                                        context.startActivity(intent)
                                    } else {
                                        // App not installed, fallback to browser
                                        val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                        val finalUrl = fallbackUrl ?: url
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                        context.startActivity(browserIntent)
                                    }
                                    return true
                                }
                            } catch (e: Exception) {
                                // On error, fallback to opening the original link in browser
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(browserIntent)
                                return true
                            }
                        }
                        
                        // For other links (like "Listen in browser"), let the system handle it if it's not the widget domain
                        if (!uriString.contains("w.soundcloud.com")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            return true
                        }
                        
                        return false
                    }
                }
                webChromeClient = WebChromeClient()
                
                // Construct the SoundCloud Widget URL
                val embedUrl = "https://w.soundcloud.com/player/?url=${url}&color=%23ff5500&auto_play=false&hide_related=false&show_comments=true&show_user=true&show_reposts=false&show_teaser=true"
                loadUrl(embedUrl)
            }
        }
    )
}
