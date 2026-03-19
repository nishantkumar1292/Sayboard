package com.elishaazaria.sayboard.recognition.recognizers.sources

import android.os.Handler
import android.os.Looper
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.auth.AuthManager
import com.elishaazaria.sayboard.recognition.logging.Logger
import com.elishaazaria.sayboard.recognition.recognizers.Recognizer
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerSource
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.concurrent.Executor

/**
 * RecognizerSource that uses the proxied cloud endpoint.
 * Supports both Whisper and Sarvam via the proxy, selected by [provider].
 */
class ProxiedCloud(
    private val provider: String,
    private val displayLocale: Locale,
    private val providerParams: Map<String, String> = emptyMap(),
    private val transliterateToRoman: Boolean = false
) : RecognizerSource {

    companion object {
        private const val TAG = "ProxiedCloud"
    }

    private val _stateFlow = MutableStateFlow(RecognizerState.NONE)
    override val stateFlow: StateFlow<RecognizerState> = _stateFlow.asStateFlow()

    private var myRecognizer: ProxiedCloudRecognizer? = null

    override val recognizer: Recognizer get() = myRecognizer!!

    override val addSpaces: Boolean
        get() = !listOf("ja", "zh").contains(displayLocale.language)

    override val isBatchRecognizer: Boolean = true

    override val closed: Boolean get() = myRecognizer == null

    override fun initialize(executor: Executor, onLoaded: (RecognizerSource?) -> Unit) {
        Logger.d(TAG, "initialize() for provider=$provider")
        _stateFlow.value = RecognizerState.LOADING
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            // Fetch a fresh ID token on the background thread (safe to block here)
            val idToken = runBlocking { AuthManager.getIdToken() }
            Logger.d(TAG, "idToken valid: ${!idToken.isNullOrEmpty()}")

            handler.post {
                if (!idToken.isNullOrEmpty()) {
                    myRecognizer = ProxiedCloudRecognizer(
                        idToken, provider, displayLocale, providerParams, transliterateToRoman
                    )
                    _stateFlow.value = RecognizerState.READY
                } else {
                    Logger.e(TAG, "No valid ID token!")
                    _stateFlow.value = RecognizerState.ERROR
                }
                onLoaded(this)
            }
        }
    }

    override fun close(freeRAM: Boolean) {
        if (freeRAM) {
            myRecognizer = null
        }
    }

    override val errorMessage: Int get() = R.string.error_proxy_auth

    override val name: String
        get() = when (provider) {
            "sarvam" -> "Sarvam Cloud (Proxied)"
            else -> "Whisper Cloud (Proxied)"
        }

    override val locale: Locale get() = displayLocale
}
