package com.elishaazaria.sayboard.recognition.recognizers.sources

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.auth.AuthManager
import com.elishaazaria.sayboard.recognition.recognizers.Recognizer
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerSource
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerState
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

    private val stateMLD = MutableLiveData(RecognizerState.NONE)
    override val stateLD: LiveData<RecognizerState> get() = stateMLD

    private var myRecognizer: ProxiedCloudRecognizer? = null

    override val recognizer: Recognizer get() = myRecognizer!!

    override val addSpaces: Boolean
        get() = !listOf("ja", "zh").contains(displayLocale.language)

    override val isBatchRecognizer: Boolean = true

    override val closed: Boolean get() = myRecognizer == null

    override fun initialize(executor: Executor, onLoaded: Observer<RecognizerSource?>) {
        Log.d(TAG, "initialize() for provider=$provider")
        stateMLD.postValue(RecognizerState.LOADING)
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            // Fetch a fresh ID token on the background thread (safe to block here)
            val idToken = runBlocking { AuthManager.getIdToken() }
            Log.d(TAG, "idToken valid: ${!idToken.isNullOrEmpty()}")

            handler.post {
                if (!idToken.isNullOrEmpty()) {
                    myRecognizer = ProxiedCloudRecognizer(
                        idToken, provider, displayLocale, providerParams, transliterateToRoman
                    )
                    stateMLD.postValue(RecognizerState.READY)
                } else {
                    Log.e(TAG, "No valid ID token!")
                    stateMLD.postValue(RecognizerState.ERROR)
                }
                onLoaded.onChanged(this)
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
