package com.elishaazaria.sayboard.recognition.recognizers.sources

import android.os.Handler
import android.os.Looper
import com.elishaazaria.sayboard.R
import com.elishaazaria.sayboard.recognition.logging.Logger
import com.elishaazaria.sayboard.recognition.recognizers.Recognizer
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerSource
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.Executor

class SarvamCloud(
    private val apiKey: String,
    private val displayLocale: Locale,
    private val mode: String = "translit",
    private val languageCode: String = "unknown"
) : RecognizerSource {

    companion object {
        private const val TAG = "SarvamCloud"
    }

    private val _stateFlow = MutableStateFlow(RecognizerState.NONE)
    override val stateFlow: StateFlow<RecognizerState> = _stateFlow.asStateFlow()

    private var myRecognizer: SarvamCloudRecognizer? = null

    override val recognizer: Recognizer get() = myRecognizer!!

    override val addSpaces: Boolean
        get() = true  // Hinglish/Roman output needs spaces

    override val isBatchRecognizer: Boolean = true

    override val closed: Boolean get() = myRecognizer == null

    override fun initialize(executor: Executor, onLoaded: (RecognizerSource?) -> Unit) {
        Logger.d(TAG, "initialize() called, current closed state: $closed")
        _stateFlow.value = RecognizerState.LOADING
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            // For cloud, there's no model to load - just validate API key
            val isValidKey = apiKey.isNotEmpty()
            Logger.d(TAG, "isValidKey: $isValidKey")

            handler.post {
                if (isValidKey) {
                    Logger.d(TAG, "Creating SarvamCloudRecognizer")
                    myRecognizer = SarvamCloudRecognizer(apiKey, displayLocale, mode, languageCode)
                    _stateFlow.value = RecognizerState.READY
                    Logger.d(TAG, "Recognizer created, closed state now: $closed")
                } else {
                    Logger.e(TAG, "Invalid API key!")
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

    override val errorMessage: Int get() = R.string.error_sarvam_missing_key
    override val name: String get() = "Sarvam Cloud (Hinglish)"
    override val locale: Locale get() = displayLocale
}
