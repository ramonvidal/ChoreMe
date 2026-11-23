plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.agenda"
}

dependencies {
    // The phone's calendars: reading the family's events and adding new ones.
    implementation(projects.core.calendar)
    // Asking for calendar permission.
    implementation(libs.androidx.activity.compose)
}
