plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.agenda"
}

dependencies {
    implementation(projects.core.calendar)
    // Asking for calendar permission.
    implementation(libs.androidx.activity.compose)
}
