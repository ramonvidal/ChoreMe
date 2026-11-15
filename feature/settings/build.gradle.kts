plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.settings"
}

dependencies {
    // Alerts, quiet hours, language, look and units of this phone.
    implementation(projects.core.settings)
    // Instant alerts (ntfy / UnifiedPush) and "Check for updates".
    implementation(projects.core.push)
    implementation(projects.core.update)
    // The phone's own visit log, for the habits the app suggests in the trips plan.
    implementation(projects.core.location)
}
