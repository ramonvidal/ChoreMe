// Wires the features together; the only module that may depend on features.
plugins {
    id("choreme.android.application")
    id("choreme.android.compose")
    id("choreme.android.hilt")
}

// Firebase config is per-developer and not committed (see README). Without it the app
// still builds, so CI and first-time contributors aren't blocked; sync just won't work.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "com.choreme"

    defaultConfig {
        applicationId = "com.choreme"
        versionCode = 1
        versionName = "0.1.0"
    }

    // CI and every developer must sign debug builds with the same key, because its
    // SHA-1 is registered in Firebase for Google sign-in. See README.
    val sharedDebugKeystore = file("debug.keystore")
    if (sharedDebugKeystore.exists()) {
        signingConfigs.getByName("debug") {
            storeFile = sharedDebugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.chores)
    implementation(projects.feature.groceries)
    implementation(projects.feature.agenda)
    implementation(projects.feature.family)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
}
