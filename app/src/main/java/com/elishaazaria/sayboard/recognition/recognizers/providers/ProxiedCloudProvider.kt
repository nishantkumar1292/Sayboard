package com.elishaazaria.sayboard.recognition.recognizers.providers

import android.content.Context
import android.util.Log
import com.elishaazaria.sayboard.auth.AuthManager
import com.elishaazaria.sayboard.auth.SubscriptionManager
import com.elishaazaria.sayboard.data.InstalledModelReference
import com.elishaazaria.sayboard.data.ModelType
import com.elishaazaria.sayboard.recognition.recognizers.RecognizerSource
import com.elishaazaria.sayboard.recognition.recognizers.sources.ProxiedCloud
import com.elishaazaria.sayboard.speakKeysPreferenceModel
import java.util.Locale

class ProxiedCloudProvider(private val context: Context) : RecognizerSourceProvider {
    private val prefs by speakKeysPreferenceModel()

    companion object {
        private const val TAG = "ProxiedCloudProvider"
    }

    override fun getInstalledModels(): List<InstalledModelReference> {
        // Only show proxied models if user is signed in and has access
        if (!AuthManager.isSignedIn || !SubscriptionManager.hasAccess) {
            return emptyList()
        }

        return listOf(
            InstalledModelReference(
                path = "proxied://whisper",
                name = "Whisper Cloud (Proxied)",
                type = ModelType.ProxiedWhisperCloud
            ),
            InstalledModelReference(
                path = "proxied://sarvam",
                name = "Sarvam Cloud (Proxied)",
                type = ModelType.ProxiedSarvamCloud
            )
        )
    }

    override fun recognizerSourceForModel(localModel: InstalledModelReference): RecognizerSource? {
        if (localModel.type != ModelType.ProxiedWhisperCloud &&
            localModel.type != ModelType.ProxiedSarvamCloud) {
            return null
        }

        if (!AuthManager.isSignedIn) return null

        // ID token is fetched lazily in ProxiedCloud.initialize() on the background thread
        return when (localModel.type) {
            ModelType.ProxiedWhisperCloud -> {
                val languageCode = prefs.whisperLanguage.get()
                val locale = if (languageCode.isNotEmpty()) Locale(languageCode) else Locale.ROOT
                val prompt = prefs.whisperPrompt.get()
                val transliterateToRoman = prefs.whisperTransliterateToRoman.get()

                val params = mutableMapOf<String, String>()
                params["model"] = "whisper-1"
                if (languageCode.isNotEmpty()) params["language"] = languageCode
                if (prompt.isNotEmpty()) params["prompt"] = prompt

                ProxiedCloud("whisper", locale, params, transliterateToRoman)
            }
            ModelType.ProxiedSarvamCloud -> {
                val locale = Locale("en", "IN")
                val mode = prefs.sarvamMode.get()
                val languageCode = prefs.sarvamLanguage.get()

                val params = mapOf(
                    "model" to "saaras:v3",
                    "mode" to mode,
                    "language_code" to languageCode
                )

                ProxiedCloud("sarvam", locale, params)
            }
            else -> null
        }
    }
}
