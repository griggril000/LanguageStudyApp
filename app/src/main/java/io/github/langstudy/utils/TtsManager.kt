package io.github.langstudy.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

val LocalTtsManager = staticCompositionLocalOf<TtsManager?> { null }

class TtsManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        } else {
            Log.e("TtsManager", "Initialization failed")
        }
    }

    fun speak(text: String, language: String) {
        if (!isInitialized) {
            Log.w("TtsManager", "TTS not initialized yet")
            return
        }

        val locale = mapLanguageToLocale(language)
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun mapLanguageToLocale(language: String): Locale {
        return when (language.lowercase()) {
            "spanish" -> Locale("es", "ES")
            "french" -> Locale.FRENCH
            "japanese" -> Locale.JAPANESE
            "german" -> Locale.GERMAN
            "italian" -> Locale.ITALIAN
            "chinese" -> Locale.CHINESE
            "korean" -> Locale.KOREAN
            "english" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
