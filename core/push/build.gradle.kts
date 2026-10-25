// Free instant pings between family phones, through UnifiedPush.
plugins {
    id("choreme.android.library")
    id("choreme.android.hilt")
}

android {
    namespace = "com.choreme.core.push"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // The connector depends on desktop Tink, whose full protobuf clashes with Firebase's
    // protobuf-lite; Tink's Android build is the same API on protobuf-lite.
    implementation(libs.unifiedpush.connector) {
        exclude(group = "com.google.crypto.tink", module = "tink")
    }
    implementation(libs.tink.android)
}
