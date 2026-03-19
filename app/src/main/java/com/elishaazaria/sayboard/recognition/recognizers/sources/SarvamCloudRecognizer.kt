package com.elishaazaria.sayboard.recognition.recognizers.sources

import com.elishaazaria.sayboard.recognition.audio.WavEncoder
import com.elishaazaria.sayboard.recognition.logging.Logger
import com.elishaazaria.sayboard.recognition.recognizers.Recognizer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

class SarvamCloudRecognizer(
    private val apiKey: String,
    override val locale: Locale?,
    private val mode: String = "translit",
    private val languageCode: String = "unknown"
) : Recognizer {

    companion object {
        private const val TAG = "SarvamCloudRecognizer"
        private const val SARVAM_API_URL = "https://api.sarvam.ai/speech-to-text"

        private const val MODEL = "saaras:v3"  // Advanced model with flexible output modes
    }

    override val sampleRate: Float = 16000f

    // Audio buffer - max 30 seconds
    private val maxBufferSamples = (30 * sampleRate).toInt()
    private val audioBuffer = ShortArray(maxBufferSamples)
    private var bufferPosition = 0

    private var lastResult = ""

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override fun reset() {
        bufferPosition = 0
        lastResult = ""
    }

    override fun acceptWaveForm(buffer: ShortArray?, nread: Int): Boolean {
        if (buffer == null || nread <= 0) return false

        val samplesToAdd = minOf(nread, maxBufferSamples - bufferPosition)
        if (samplesToAdd > 0) {
            System.arraycopy(buffer, 0, audioBuffer, bufferPosition, samplesToAdd)
            bufferPosition += samplesToAdd
        }

        // Return true if buffer is full (triggers auto-stop)
        return bufferPosition >= maxBufferSamples
    }

    override fun getResult(): String {
        // Sarvam doesn't have intermediate results - return empty
        return ""
    }

    override fun getPartialResult(): String {
        return ""
    }

    override fun getFinalResult(): String {
        if (bufferPosition == 0) return ""

        Logger.d(TAG, "Transcribing ${bufferPosition} samples (${bufferPosition / sampleRate} seconds)")
        transcribe()
        val result = lastResult
        lastResult = ""
        return result
    }

    private fun transcribe() {
        if (apiKey.isEmpty()) {
            Logger.e(TAG, "No API key configured")
            return
        }

        val wavBytes = createWavBytes()
        Logger.d(TAG, "Created WAV: ${wavBytes.size} bytes")

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "audio.wav",
                    wavBytes.toRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", MODEL)
                .addFormDataPart("mode", mode)
                .addFormDataPart("language_code", languageCode)
                .addFormDataPart("with_timestamps", "false")
                .build()

            val request = Request.Builder()
                .url(SARVAM_API_URL)
                .addHeader("api-subscription-key", apiKey)
                .post(requestBody)
                .build()

            Logger.d(TAG, "Sending request to Sarvam API (model=$MODEL)...")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val text = json.optString("transcript", "").trim()

                    lastResult = removeSpaceForLocale(text)
                }
            } else {
                Logger.e(TAG, "API error: ${response.code}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Transcription failed", e)
        }

        bufferPosition = 0
    }

    private fun createWavBytes(): ByteArray =
        WavEncoder.createWavBytes(audioBuffer, bufferPosition, sampleRate.toInt())
}
