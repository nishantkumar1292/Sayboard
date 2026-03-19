package com.elishaazaria.sayboard.recognition.recognizers

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.concurrent.Executor

interface RecognizerSource {
    fun initialize(executor: Executor, onLoaded: (RecognizerSource?) -> Unit)
    val recognizer: Recognizer
    fun close(freeRAM: Boolean)
    val stateFlow: StateFlow<RecognizerState>

    val addSpaces: Boolean

    /**
     * If true, this recognizer processes audio in batch (like Whisper).
     * Tapping mic to "pause" should actually stop recording to trigger transcription.
     * If false, this recognizer streams results in real-time.
     */
    val isBatchRecognizer: Boolean
        get() = false

    val closed: Boolean

    @get:StringRes
    val errorMessage: Int
    val name: String

    val locale: Locale
}