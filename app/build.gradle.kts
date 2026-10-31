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

    // Every CI build gets a higher number, so a new APK always installs over the old one.
    val buildNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").map(String::toInt).getOrElse(1)

    defaultConfig {
        applicationId = "com.choreme"
        versionCode = buildNumber
        versionName = "0.$buildNumber"
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
            // Same key as debug builds: it's the one registered in Firebase, and it means a
            // release installs over a test build (and the other way round) without uninstalling.
            signingConfig = signingConfigs.getByName("debug")
            // Shrinking would make the APK smaller, but can break code that's only found at
            // runtime; worth turning on once there are UI tests on a real device.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.core.push)
    implementation(projects.feature.onboarding)
    implementation(projects.feature.chores)
    implementation(projects.feature.groceries)
    implementation(projects.feature.agenda)
    implementation(projects.feature.map)
    implementation(projects.feature.widget)
    implementation(projects.feature.update)
    implementation(projects.feature.family)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
}
