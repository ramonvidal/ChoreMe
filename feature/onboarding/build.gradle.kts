plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
