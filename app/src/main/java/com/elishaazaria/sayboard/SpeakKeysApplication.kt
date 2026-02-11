package com.elishaazaria.sayboard

import android.app.Application
import com.elishaazaria.sayboard.AppCtx.setAppCtx
import dev.patrickgold.jetpref.datastore.JetPref

class SpeakKeysApplication : Application() {
    private val prefs by speakKeysPreferenceModel()
    override fun onCreate() {
        super.onCreate()

        // Optionally initialize global JetPref configs. This must be done before
        // any preference datastore is initialized!
        JetPref.configure(
            saveIntervalMs = 500,
            encodeDefaultValues = true,
        )

        // Initialize your datastore here (required)
        prefs.initializeBlocking(this)

        setAppCtx(this)
    }
}
