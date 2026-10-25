plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.settings"
}

dependencies {
    implementation(projects.core.settings)
    implementation(projects.core.push)
    // "Check for updates"; the banner at the top offers what it finds.
    implementation(projects.core.update)
}
