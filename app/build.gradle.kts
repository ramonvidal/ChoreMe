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

    // Two builds of the same app. github: the APK on the project's release page, which updates
    // itself. play: the bundle for Google Play, which Google updates (so no self-updater, and
    // not the permission to install apps). Everything else is shared.
    flavorDimensions += "distribution"
    productFlavors {
        create("github") { dimension = "distribution" }
        create("play") { dimension = "distribution" }
    }

    // The Play bundle is signed with the upload key (Google then signs what people install with
    // its own key). Built with -PplayUpload, with the key in these environment variables (CI
    // keeps them as secrets); every other build uses the shared debug key below.
    val signForPlay = providers.gradleProperty("playUpload").isPresent
    if (signForPlay) {
        fun upload(name: String) = providers.environmentVariable(name).orNull
            ?: error("Signing for Google Play needs the environment variable $name (see README, \"Google Play\")")
        signingConfigs.create("upload") {
            storeFile = file(upload("UPLOAD_KEYSTORE_FILE"))
            storePassword = upload("UPLOAD_KEYSTORE_PASSWORD")
            keyAlias = upload("UPLOAD_KEY_ALIAS")
            keyPassword = upload("UPLOAD_KEY_PASSWORD")
        }
    }

    // The language picked in the app (English or Baianês) must work whatever the phone's own
    // language is, so a bundle can't hand each phone only its own language's texts.
    bundle { language { enableSplit = false } }

    buildTypes {
        release {
            // Same key as debug builds: it's the one registered in Firebase, and it means a
            // release installs over a test build (and the other way round) without uninstalling.
            signingConfig = if (signForPlay) signingConfigs.getByName("upload") else signingConfigs.getByName("debug")
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
    implementation(projects.core.update)
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
