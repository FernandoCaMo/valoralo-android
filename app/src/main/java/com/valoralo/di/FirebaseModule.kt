package com.valoralo.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.valoralo.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase instances. With -Pvaloralo.emuladores=true (debug only) they point to the local
 * emulators; 10.0.2.2 is the host machine as seen from the Android emulator.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    private const val HOST_EMULADORES = "10.0.2.2"

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
        if (BuildConfig.USAR_EMULADORES) useEmulator(HOST_EMULADORES, 8080)
    }

    @Provides
    @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance().apply {
        if (BuildConfig.USAR_EMULADORES) useEmulator(HOST_EMULADORES, 9099)
    }

    /** Same region as Firestore and the functions (Madrid). */
    @Provides
    @Singleton
    fun provideFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance("europe-southwest1").apply {
        if (BuildConfig.USAR_EMULADORES) useEmulator(HOST_EMULADORES, 5001)
    }
}
