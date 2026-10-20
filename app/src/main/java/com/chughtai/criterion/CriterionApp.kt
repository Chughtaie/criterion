package com.chughtai.criterion

import android.app.Application
import android.os.StrictMode
import dagger.hilt.android.HiltAndroidApp

/** Hilt's root. onCreate does no eager DB, network or WorkManager work (doc 04 §13); StrictMode is debug-only (§4.4). */
@HiltAndroidApp
class CriterionApp : Application() {
    override fun onCreate() {
        // Before super.onCreate(), so StrictMode also covers what Hilt injects into the application.
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy
                    .Builder()
                    .detectAll()
                    .penaltyLog()
                    .build(),
            )
        }
        super.onCreate()
    }
}
