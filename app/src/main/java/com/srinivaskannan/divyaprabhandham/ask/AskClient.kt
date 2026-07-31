package com.srinivaskannan.divyaprabhandham.ask

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to the Ask proxy (the Cloud Function that fronts Gemini). Mirrors the
 * HttpURLConnection style used by DriveAppData, with one deliberate difference:
 * sync swallows every failure because it is best-effort, but Ask must *show*
 * failures — a question that silently does nothing is worse than an error — so
 * this returns a sealed [AskResult] rather than null.
 *
 * The app never calls Gemini directly; the API key and the guardrail live only
 * behind this endpoint.
 */
object AskClient {

    // The deployed proxy. The Cloud Run URL is the equivalent target if the
    // Cloud Functions host is ever retired.
    private const val ENDPOINT =
        "https://asia-south1-spry-guru-504101-r5.cloudfunctions.net/ndp-ask"

    /**
     * Sends a question (and optional retrieved corpus [context]) to the proxy.
     * [appCheckToken] is attached when available so the proxy can enforce App
     * Check in Phase 3; null is fine until then.
     */
    suspend fun ask(
        question: String,
        context: String?,
        appCheckToken: String? = null,
    ): AskResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val body = JSONObject().apply {
                put("question", question)
                if (!context.isNullOrBlank()) put("context", context)
            }.toString()

            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                appCheckToken?.let { setRequestProperty("X-Firebase-AppCheck", it) }
                connectTimeout = 15_000
                // Generous read timeout: a grounded generation can take a few
                // seconds, and cutting it off early would read as a failure.
                readTimeout = 45_000
                doOutput = true
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body) }
            }

            val code = connection.responseCode
            if (code in 200..299) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val answer = JSONObject(text).optString("answer").trim()
                if (answer.isEmpty()) AskResult.Error(AskError.EMPTY)
                else AskResult.Answer(answer)
            } else {
                // Map the proxy's status codes to typed errors the UI can phrase
                // in the person's language, rather than showing a raw number.
                val error = when (code) {
                    401 -> AskError.UNAUTHORIZED
                    429 -> AskError.RATE_LIMITED
                    in 500..599 -> AskError.SERVER
                    else -> AskError.SERVER
                }
                AskResult.Error(error)
            }
        } catch (_: java.net.UnknownHostException) {
            AskResult.Error(AskError.OFFLINE)
        } catch (_: java.net.SocketTimeoutException) {
            AskResult.Error(AskError.TIMEOUT)
        } catch (_: Exception) {
            AskResult.Error(AskError.SERVER)
        } finally {
            connection?.disconnect()
        }
    }
}

sealed interface AskResult {
    data class Answer(val text: String) : AskResult
    data class Error(val kind: AskError) : AskResult
}

/** Failure kinds, each mapped to a localised message in the UI. */
enum class AskError {
    OFFLINE,
    TIMEOUT,
    RATE_LIMITED,
    UNAUTHORIZED,
    SERVER,
    EMPTY,
}
