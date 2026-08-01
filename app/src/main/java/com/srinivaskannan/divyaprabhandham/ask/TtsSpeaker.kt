package com.srinivaskannan.divyaprabhandham.ask

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Speaks Ask answers aloud for voice mode, wrapping Android's built-in
 * TextToSpeech. On-device, free, no backend.
 *
 * Two honest constraints from the feasibility study shape this:
 *  - Tamil voice availability is device-dependent, so [speak] picks ta-IN only
 *    when the device actually has it and otherwise falls back to English rather
 *    than failing silently or reading Tamil with a wrong-language voice.
 *  - The native engine cannot stream, so it speaks a whole answer at once —
 *    fine for a Q&A, but it means a "walkie-talkie" cadence, not real-time.
 */
class TtsSpeaker(context: Context) {

    var speaking by mutableStateOf(false)
        private set

    var ready by mutableStateOf(false)
        private set

    private var tamilAvailable = false

    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tamilAvailable = runCatching {
                val r = engineLanguageAvailable(Locale("ta", "IN"))
                r
            }.getOrDefault(false)
            ready = true
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { speaking = true }
            override fun onDone(utteranceId: String?) { speaking = false }
            @Deprecated("deprecated in API 21")
            override fun onError(utteranceId: String?) { speaking = false }
            override fun onError(utteranceId: String?, errorCode: Int) { speaking = false }
        })
    }

    private fun engineLanguageAvailable(locale: Locale): Boolean {
        val res = tts.isLanguageAvailable(locale)
        return res == TextToSpeech.LANG_AVAILABLE ||
            res == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            res == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    /**
     * Speaks [text]. If it looks Tamil and a Tamil voice exists, speaks in
     * Tamil; otherwise English. Returns false if nothing could be spoken (no
     * usable voice), so the caller can decide whether to hint the user.
     */
    fun speak(text: String): Boolean {
        if (!ready || text.isBlank()) return false
        val wantsTamil = looksTamil(text)
        val locale = if (wantsTamil && tamilAvailable) Locale("ta", "IN") else Locale.ENGLISH
        if (tts.setLanguage(locale) == TextToSpeech.LANG_MISSING_DATA) return false
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ask-answer")
        return true
    }

    fun stop() {
        tts.stop()
        speaking = false
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    /** True if the text contains Tamil script, so we prefer a Tamil voice. */
    private fun looksTamil(text: String): Boolean =
        text.any { it.code in 0x0B80..0x0BFF }
}
