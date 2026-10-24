plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.map"
}

dependencies {
    implementation(projects.core.location)
    // OpenStreetMap tiles: free, no API key.
    implementation(libs.osmdroid)
    // Background location updates.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // Asking for location permission.
    implementation(libs.androidx.activity.compose)
}
