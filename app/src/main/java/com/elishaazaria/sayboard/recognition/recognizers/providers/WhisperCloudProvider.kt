package com.elishaazaria.sayboard.recognition.recognizers.providers

import com.elishaazaria.sayboard.data.InstalledModelReference
import com.elishaazaria.sayboard.data.ModelType
import com.elishaazaria.sayboard.recognition.preferences.PreferencesRepository
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerSource
import com.elishaazaria.sayboard.recognition.recognizers.sources.WhisperCloud
import java.util.Locale

class WhisperCloudProvider(private val prefs: PreferencesRepository) : RecognizerSourceProvider {

    override fun getInstalledModels(): List<InstalledModelReference> {
        // Only show cloud option if API key is configured
        val apiKey = prefs.getOpenaiApiKey()
        if (apiKey.isEmpty()) {
            return emptyList()
        }

        // Return a single "model" representing OpenAI cloud
        return listOf(
            InstalledModelReference(
                path = "whisper://cloud",  // Virtual path
                name = "Whisper Cloud (OpenAI)",
                type = ModelType.WhisperCloud
            )
        )
    }

    override fun recognizerSourceForModel(localModel: InstalledModelReference): RecognizerSource? {
        if (localModel.type != ModelType.WhisperCloud) return null

        val apiKey = prefs.getOpenaiApiKey()
        if (apiKey.isEmpty()) return null

        // Get preferred language from settings
        val languageCode = prefs.getWhisperLanguage()
        val locale = if (languageCode.isNotEmpty()) {
            Locale(languageCode)
        } else {
            Locale.ROOT  // Auto-detect
        }

        // Get prompt for guiding transcription style (e.g., Hinglish)
        val prompt = prefs.getWhisperPrompt()

        // Get transliteration preference
        val transliterateToRoman = prefs.getWhisperTransliterateToRoman()

        return WhisperCloud(apiKey, locale, prompt, transliterateToRoman)
    }
}
