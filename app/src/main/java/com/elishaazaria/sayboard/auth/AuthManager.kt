package com.elishaazaria.sayboard.auth

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.elishaazaria.sayboard.AppCtx
import com.elishaazaria.sayboard.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object AuthManager {
    private const val TAG = "AuthManager"

    private fun webClientId(): String =
        AppCtx.getStringRes(R.string.default_web_client_id)

    const val RC_SIGN_IN = 9001

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    val isSignedIn: Boolean get() = currentUser != null

    val displayName: String? get() = currentUser?.displayName

    val email: String? get() = currentUser?.email

    val uid: String? get() = currentUser?.uid

    /**
     * Get a fresh Firebase ID token for authenticating with our proxy.
     * Firebase SDK handles caching and auto-refresh.
     */
    suspend fun getIdToken(): String? {
        return try {
            val result = currentUser?.getIdToken(false)?.await()
            result?.token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get ID token", e)
            null
        }
    }

    /**
     * Build the Google Sign-In intent. Call this from an Activity
     * and use startActivityForResult with RC_SIGN_IN.
     */
    fun getSignInIntent(activity: Activity): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId())
            .requestEmail()
            .build()

        val client: GoogleSignInClient = GoogleSignIn.getClient(activity, gso)
        return client.signInIntent
    }

    /**
     * Handle the result from Google Sign-In intent.
     * Call this from onActivityResult when requestCode == RC_SIGN_IN.
     * Returns true if sign-in succeeded.
     */
    suspend fun handleSignInResult(data: Intent?): Boolean {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
            auth.signInWithCredential(credential).await()
            Log.d(TAG, "Sign-in successful: ${currentUser?.email}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in failed", e)
            false
        }
    }

    fun signOut(activity: Activity) {
        auth.signOut()

        // Also sign out from Google to allow account picker next time
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId())
            .requestEmail()
            .build()
        GoogleSignIn.getClient(activity, gso).signOut()

        Log.d(TAG, "Signed out")
    }
}
