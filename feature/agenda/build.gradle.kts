plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.agenda"
}

dependencies {
    implementation(projects.core.calendar)
    // Asking for calendar permission.
    implementation(libs.androidx.activity.compose)
}
