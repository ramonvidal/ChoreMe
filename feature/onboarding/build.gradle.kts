plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.onboarding"
}

dependencies {
    implementation(projects.core.push)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
}
