package com.srinivaskannan.divyaprabhandham.ask

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Inline voice input for Ask, wrapping Android's on-device SpeechRecognizer.
 *
 * Exposes plain observable state — whether it is listening, the latest partial
 * transcript, and a smoothed sound level — so the input bar can show a custom
 * listening animation and drop the recognised text straight into the field.
 * On-device recognition is free (no API cost) and the recognizer streams
 * partial results, which is what makes the live animation and text possible.
 *
 * Tamil-first: the recognition locale is requested as ta-IN, since the audience
 * speaks Tamil; the recognizer falls back gracefully when Tamil is unavailable.
 */
class VoiceRecognizer(private val context: Context) {

    var listening by mutableStateOf(false)
        private set

    /** The best transcript so far — partials while speaking, final on end. */
    var transcript by mutableStateOf("")
        private set

    /** 0f..1f smoothed microphone level, for the animated chakra/bars. */
    var level by mutableFloatStateOf(0f)
        private set

    var error by mutableStateOf<AskError?>(null)
        private set

    private var recognizer: SpeechRecognizer? = null

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(onFinal: (String) -> Unit) {
        if (listening) return
        error = null
        transcript = ""

        if (!isAvailable) {
            error = AskError.SERVER
            return
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true }
                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    // rmsdB is roughly -2..12; map to 0..1 and smooth so the
                    // animation pulses with the voice rather than jittering.
                    val norm = ((rmsdB + 2f) / 14f).coerceIn(0f, 1f)
                    level = level * 0.6f + norm * 0.4f
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false; level = 0f }

                override fun onError(code: Int) {
                    listening = false
                    level = 0f
                    error = when (code) {
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> AskError.OFFLINE
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null // silent: just nothing said
                        else -> AskError.SERVER
                    }
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    transcript = text
                    listening = false
                    level = 0f
                    if (text.isNotBlank()) onFinal(text)
                }

                override fun onPartialResults(partial: Bundle?) {
                    partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.let { if (it.isNotBlank()) transcript = it }
                }

                override fun onEvent(type: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            // Tamil first; the system falls back if unavailable.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.stopListening()
        listening = false
        level = 0f
    }

    fun cancel() {
        recognizer?.cancel()
        listening = false
        level = 0f
        transcript = ""
    }

    fun release() {
        recognizer?.destroy()
        recognizer = null
        listening = false
    }
}
