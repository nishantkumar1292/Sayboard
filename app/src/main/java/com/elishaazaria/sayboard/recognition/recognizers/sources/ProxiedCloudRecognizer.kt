package com.elishaazaria.sayboard.recognition.recognizers.sources

import com.elishaazaria.sayboard.recognition.audio.WavEncoder
import com.elishaazaria.sayboard.recognition.logging.Logger
import com.elishaazaria.sayboard.recognition.recognizers.Recognizer
import com.elishaazaria.sayboard.utils.DevanagariTransliterator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Recognizer that sends audio to our Firebase Cloud Functions proxy
 * instead of directly to OpenAI/Sarvam. The proxy handles API keys server-side.
 */
class ProxiedCloudRecognizer(
    private val firebaseIdToken: String,
    private val provider: String, // "whisper" or "sarvam"
    override val locale: Locale?,
    private val providerParams: Map<String, String> = emptyMap(),
    private val transliterateToRoman: Boolean = false
) : Recognizer {

    companion object {
        private const val TAG = "ProxiedCloudRecognizer"
        const val PROXY_BASE_URL = "https://asia-south1-speakkeys.cloudfunctions.net"
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 1500L
    }

    override val sampleRate: Float = 16000f

    // Audio buffer - max 30 seconds
    private val maxBufferSamples = (30 * sampleRate).toInt()
    private val audioBuffer = ShortArray(maxBufferSamples)
    private var bufferPosition = 0

    private var lastResult = ""
    private var lastError: IOException? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun reset() {
        bufferPosition = 0
        lastResult = ""
        lastError = null
    }

    override fun acceptWaveForm(buffer: ShortArray?, nread: Int): Boolean {
        if (buffer == null || nread <= 0) return false

        val samplesToAdd = minOf(nread, maxBufferSamples - bufferPosition)
        if (samplesToAdd > 0) {
            System.arraycopy(buffer, 0, audioBuffer, bufferPosition, samplesToAdd)
            bufferPosition += samplesToAdd
        }

        return bufferPosition >= maxBufferSamples
    }

    override fun getResult(): String = ""

    override fun getPartialResult(): String = ""

    override fun getFinalResult(): String {
        if (bufferPosition == 0) return ""

        Logger.d(TAG, "Transcribing ${bufferPosition} samples via proxy ($provider)")
        try {
            transcribe()
            lastError?.let { throw it }
            return lastResult
        } finally {
            bufferPosition = 0
            lastResult = ""
            lastError = null
        }
    }

    private fun transcribe() {
        val wavBytes = createWavBytes()
        Logger.d(TAG, "Created WAV: ${wavBytes.size} bytes")
        lastError = null

        for (attempt in 0..MAX_RETRIES) {
            try {
                val bodyBuilder = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        "audio.wav",
                        wavBytes.toRequestBody("audio/wav".toMediaType())
                    )
                    .addFormDataPart("provider", provider)

                // Add all provider-specific params
                for ((key, value) in providerParams) {
                    if (value.isNotEmpty()) {
                        bodyBuilder.addFormDataPart(key, value)
                    }
                }

                val request = Request.Builder()
                    .url("${PROXY_BASE_URL}/transcribe")
                    .addHeader("Authorization", "Bearer $firebaseIdToken")
                    .post(bodyBuilder.build())
                    .build()

                Logger.d(TAG, "Sending request to proxy (attempt ${attempt + 1})...")
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()

                    when {
                        response.isSuccessful -> {
                            val json = JSONObject(responseBody)
                            var text = when (provider) {
                                "sarvam" -> json.optString("transcript", "").trim()
                                else -> json.optString("text", "").trim()
                            }

                            if (transliterateToRoman) {
                                text = DevanagariTransliterator.transliterate(text)
                            }

                            lastError = null
                            lastResult = removeSpaceForLocale(text)
                            return // Success, no retry needed
                        }
                        response.code == 402 -> {
                            val message = extractErrorMessage(responseBody)
                            Logger.w(TAG, "Access denied: ${response.code} - $message")
                            lastError = IOException("Proxy access denied: $message")
                            return
                        }
                        else -> {
                            val message = extractErrorMessage(responseBody)
                            Logger.e(
                                TAG,
                                "Proxy error: ${response.code} - $message (attempt ${attempt + 1}/${MAX_RETRIES + 1})"
                            )
                            lastError = IOException("Proxy error ${response.code}: $message")
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Transcription via proxy failed (attempt ${attempt + 1}/${MAX_RETRIES + 1})", e)
                lastError = if (e is IOException) e else IOException("Proxy transcription failed", e)
                if (attempt < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS)
                }
            }
        }

        Logger.e(TAG, "All ${MAX_RETRIES + 1} transcription attempts failed", lastError)
    }

    private fun extractErrorMessage(responseBody: String): String {
        if (responseBody.isBlank()) return "Empty response body"
        return try {
            val json = JSONObject(responseBody)
            json.optString("error")
                .ifBlank { json.optString("message") }
                .ifBlank { responseBody.take(200) }
        } catch (_: Exception) {
            responseBody.take(200)
        }
    }

    private fun createWavBytes(): ByteArray =
        WavEncoder.createWavBytes(audioBuffer, bufferPosition, sampleRate.toInt())
}
