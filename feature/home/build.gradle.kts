plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.home"
}

dependencies {
    // The day's calendar events.
    implementation(projects.core.calendar)
    // The weather outside and for the family's trips.
    implementation(projects.core.weather)
    implementation(projects.core.location)
    implementation(projects.core.settings)
    // The morning weather notification.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
