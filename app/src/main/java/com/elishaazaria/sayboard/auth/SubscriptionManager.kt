package com.elishaazaria.sayboard.auth

import android.util.Log
import com.elishaazaria.sayboard.speakKeysPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SubscriptionManager {
    private const val TAG = "SubscriptionManager"

    // TODO: Replace with your deployed Cloud Functions URL
    const val PROXY_BASE_URL = "https://asia-south1-speakkeys.cloudfunctions.net"

    private val prefs by speakKeysPreferenceModel()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Whether the user has access to proxied cloud recognition.
     * Checks cached local state (subscription active, trial active, or has credits).
     */
    val hasAccess: Boolean
        get() {
            val status = prefs.cachedSubscriptionStatus.get()
            if (status == "active") return true

            val credits = prefs.cachedCredits.get()
            if (credits > 0) return true

            val trialExpires = prefs.cachedTrialExpiresAt.get()
            if (trialExpires > 0 && System.currentTimeMillis() < trialExpires) return true

            // No cached data yet = new user whose trial hasn't started. Give access.
            if (trialExpires == 0L && status == "none") return true

            return false
        }

    val isTrialActive: Boolean
        get() {
            val trialExpires = prefs.cachedTrialExpiresAt.get()
            return trialExpires > 0 && System.currentTimeMillis() < trialExpires
        }

    val trialDaysRemaining: Int
        get() {
            val trialExpires = prefs.cachedTrialExpiresAt.get()
            if (trialExpires <= 0) return 0
            val remaining = trialExpires - System.currentTimeMillis()
            if (remaining <= 0) return 0
            return (remaining / (24 * 60 * 60 * 1000)).toInt() + 1
        }

    val subscriptionStatus: String
        get() = prefs.cachedSubscriptionStatus.get()

    val credits: Int
        get() = prefs.cachedCredits.get()

    /**
     * Fetch latest status from the backend and update local cache.
     * Call this on app launch and after sign-in.
     */
    suspend fun refreshStatus() {
        if (!AuthManager.isSignedIn) {
            clearCache()
            return
        }

        val token = AuthManager.getIdToken() ?: return

        try {
            val response = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("$PROXY_BASE_URL/userStatus")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                client.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return
                val json = JSONObject(body)

                val trialExpiresAt = json.optString("trialExpiresAt", "")
                val subStatus = json.optString("subscriptionStatus", "none")
                val userCredits = json.optInt("credits", 0)

                prefs.cachedSubscriptionStatus.set(subStatus)
                prefs.cachedCredits.set(userCredits)

                if (trialExpiresAt.isNotEmpty() && trialExpiresAt != "null") {
                    try {
                        val sdf = java.text.SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            java.util.Locale.US
                        ).also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
                        val date = sdf.parse(trialExpiresAt)
                            ?: java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                                java.util.Locale.US
                            ).also { it.timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                .parse(trialExpiresAt)
                        prefs.cachedTrialExpiresAt.set(date?.time ?: 0L)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse trialExpiresAt: $trialExpiresAt", e)
                    }
                } else {
                    prefs.cachedTrialExpiresAt.set(0L)
                }

                Log.d(TAG, "Status refreshed: sub=$subStatus, credits=$userCredits, trialExpires=$trialExpiresAt")
            } else {
                Log.w(TAG, "Status fetch failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh status", e)
        }
    }

    fun clearCache() {
        prefs.cachedSubscriptionStatus.set("none")
        prefs.cachedTrialExpiresAt.set(0L)
        prefs.cachedCredits.set(0)
    }
}
