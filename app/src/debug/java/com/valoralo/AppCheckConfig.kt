package com.valoralo

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds use the App Check debug provider. On first launch it prints a debug token in
 * logcat ("Enter this debug secret into the allow list…"): add it in Firebase console →
 * App Check → Apps → Manage debug tokens.
 */
fun instalarAppCheck() {
    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
}
