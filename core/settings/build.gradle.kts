// This phone's own choices: which alerts, quiet hours.
plugins {
    id("choreme.android.library")
    id("choreme.android.hilt")
}

android {
    namespace = "com.choreme.core.settings"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
}
