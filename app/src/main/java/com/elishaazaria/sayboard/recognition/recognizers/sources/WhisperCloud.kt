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

class WhisperCloud(
    private val apiKey: String,
    private val displayLocale: Locale,
    private val prompt: String = "",
    private val transliterateToRoman: Boolean = false
) : RecognizerSource {

    companion object {
        private const val TAG = "WhisperCloud"
    }

    private val _stateFlow = MutableStateFlow(RecognizerState.NONE)
    override val stateFlow: StateFlow<RecognizerState> = _stateFlow.asStateFlow()

    private var myRecognizer: WhisperCloudRecognizer? = null

    override val recognizer: Recognizer get() = myRecognizer!!

    override val addSpaces: Boolean
        get() = !listOf("ja", "zh").contains(displayLocale.language)

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
                    Logger.d(TAG, "Creating WhisperCloudRecognizer with prompt: $prompt, transliterate: $transliterateToRoman")
                    myRecognizer = WhisperCloudRecognizer(apiKey, displayLocale, prompt, transliterateToRoman)
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

    override val errorMessage: Int get() = R.string.error_invalid_api_key
    override val name: String get() = "Whisper Cloud (OpenAI)"
    override val locale: Locale get() = displayLocale
}
