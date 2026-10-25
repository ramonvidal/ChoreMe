plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.settings"
}

dependencies {
    implementation(projects.core.settings)
    implementation(projects.core.push)
}
