package io.github.langstudy.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
        factory = { ctx ->
            YouTubePlayerView(ctx).apply {
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
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val uri = request?.url ?: return false
                        val uriString = uri.toString()
                        android.util.Log.d("SoundCloudPlayer", "URL Loading: $uriString")

                        // 1. Check for SoundCloud's special "intent://" or "soundcloud://" schemes
                        if (uriString.startsWith("intent://") || uriString.startsWith("soundcloud://")) {
                            try {
                                val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
                                if (intent != null) {
                                    // Try to get a clean web URL from the intent first
                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                    val dataUrl = intent.dataString

                                    // Try to launch the app if it's there
                                    val packageManager = context.packageManager
                                    if (intent.`package` != null && packageManager.getLaunchIntentForPackage(
                                            intent.`package`!!
                                        ) != null
                                    ) {
                                        context.startActivity(intent)
                                        return true
                                    }

                                    // Fallback: If it's an intent we can't handle, open the best URL we have in a fresh browser intent
                                    val webUrl = fallbackUrl ?: dataUrl ?: url
                                    val browserIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
                                    context.startActivity(browserIntent)
                                    // Go back to the widget if possible to avoid staying on a white page
                                    if (view?.canGoBack() == true) {
                                        view.goBack()
                                    }
                                    return true
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SoundCloudPlayer", "Intent parsing failed", e)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                if (view?.canGoBack() == true) {
                                    view.goBack()
                                }
                                return true
                            }
                        }

                        // 2. Intercept standard HTTP links that are NOT the widget itself
                        // This catches the "Listen in browser" button which is a standard <a> tag
                        if (uri.scheme == "http" || uri.scheme == "https") {
                            if (!uriString.contains("w.soundcloud.com/player")) {
                                android.util.Log.d(
                                    "SoundCloudPlayer",
                                    "Opening standard link in browser: $uriString"
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                // Go back to the widget if possible to avoid staying on a white page
                                if (view?.canGoBack() == true) {
                                    view.goBack()
                                }
                                return true
                            }
                        }

                        return false
                    }
                }
                webChromeClient = WebChromeClient()

                // Construct the SoundCloud Widget URL - using visual=true and show_teaser=false
                // to remove the problematic "Play on SoundCloud" overlay buttons.
                val embedUrl =
                    "https://w.soundcloud.com/player/?url=${url}&color=%23ff5500&auto_play=false&hide_related=true&show_comments=false&show_user=true&show_reposts=false&show_teaser=false&visual=true"
                loadUrl(embedUrl)
            }
        }
    )
}
