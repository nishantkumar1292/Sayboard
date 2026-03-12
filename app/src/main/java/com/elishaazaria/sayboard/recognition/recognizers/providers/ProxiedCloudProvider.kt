package com.elishaazaria.sayboard.recognition.recognizers.providers

import android.content.Context
import android.util.Log
import com.elishaazaria.sayboard.auth.AuthManager
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
        // Only gate on sign-in status (local/reliable). Access is enforced server-side
        // by the proxy backend, avoiding stale-cache lockout issues.
        if (!AuthManager.isSignedIn) {
            Log.d(TAG, "getInstalledModels: not signed in, skipping proxied models")
            return emptyList()
        }
        Log.d(TAG, "getInstalledModels: signed in as ${AuthManager.currentUser?.email}, returning proxied models")

        return listOf(
            InstalledModelReference(
                path = "proxied://sarvam",
                name = "Sarvam Cloud (Proxied)",
                type = ModelType.ProxiedSarvamCloud
            )
        )
    }

    override fun recognizerSourceForModel(localModel: InstalledModelReference): RecognizerSource? {
        if (localModel.type != ModelType.ProxiedSarvamCloud) {
            return null
        }

        if (!AuthManager.isSignedIn) return null

        // ID token is fetched lazily in ProxiedCloud.initialize() on the background thread
        return when (localModel.type) {
            ModelType.ProxiedSarvamCloud -> {
                val locale = Locale("en", "IN")
                val mode = normalizeSarvamMode(prefs.sarvamMode.get())
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

    private fun normalizeSarvamMode(mode: String): String = when (mode) {
        "native" -> "transcribe"
        "transcribe", "translit" -> mode
        else -> "translit"
    }
}
