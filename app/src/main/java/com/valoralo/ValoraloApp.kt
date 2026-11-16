package com.valoralo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ValoraloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // App Check: debug provider in debug builds, Play Integrity in release (see AppCheckConfig).
        instalarAppCheck()
    }
}
