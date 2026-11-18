plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// google-services.json is not committed (see .gitignore). Only apply the plugin
// when the file is present so the project builds without a Firebase config.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    // Crashlytics needs the Firebase config; in release it also uploads the R8 mapping file.
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

/**
 * Upload key, kept outside the repository: C:\Users\Fernando\.secrets\valoralo-upload.jks, with its
 * path and passwords in the user's ~/.gradle/gradle.properties (VALORALO_UPLOAD_*). Never in this repo.
 */
val claveSubida = listOf(
    "VALORALO_UPLOAD_STORE_FILE", "VALORALO_UPLOAD_STORE_PASSWORD", "VALORALO_UPLOAD_KEY_ALIAS", "VALORALO_UPLOAD_KEY_PASSWORD",
).associateWith { project.findProperty(it) as String? }

android {
    namespace = "com.valoralo"
    compileSdk = 36

    defaultConfig {
        // Play identifier (com.valoralo was taken). The namespace and the Kotlin packages stay com.valoralo.
        applicationId = "io.github.fernandocamo.valoralo"
        minSdk = 26
        targetSdk = 36
        // Increase versionCode on every upload to Play.
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (claveSubida.values.all { !it.isNullOrBlank() }) {
            create("subida") {
                storeFile = file(claveSubida.getValue("VALORALO_UPLOAD_STORE_FILE")!!)
                storePassword = claveSubida.getValue("VALORALO_UPLOAD_STORE_PASSWORD")
                keyAlias = claveSubida.getValue("VALORALO_UPLOAD_KEY_ALIAS")
                keyPassword = claveSubida.getValue("VALORALO_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // ./gradlew assembleDebug -Pvaloralo.emuladores=true → use the local Firebase emulators.
            val emuladores = (project.findProperty("valoralo.emuladores") as String?) == "true"
            buildConfigField("boolean", "USAR_EMULADORES", emuladores.toString())
            manifestPlaceholders["crashlyticsActivado"] = "false"
        }
        release {
            buildConfigField("boolean", "USAR_EMULADORES", "false")
            manifestPlaceholders["crashlyticsActivado"] = "true"
            // Without the upload key the release build is unsigned (fine for CI checks, not for Play).
            signingConfigs.findByName("subida")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// An AAB for Play must be signed with the upload key: fail early with a clear message.
tasks.matching { it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(android.signingConfigs.findByName("subida") != null) {
            "Falta la clave de subida: añade VALORALO_UPLOAD_STORE_FILE, VALORALO_UPLOAD_STORE_PASSWORD, " +
                "VALORALO_UPLOAD_KEY_ALIAS y VALORALO_UPLOAD_KEY_PASSWORD a ~/.gradle/gradle.properties."
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.crashlytics)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
