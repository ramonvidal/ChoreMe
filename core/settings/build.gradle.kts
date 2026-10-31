// This phone's own choices: which alerts, quiet hours, the app's language.
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
    implementation(libs.androidx.appcompat)
}
