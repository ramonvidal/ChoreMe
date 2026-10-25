// Theme, shared composables and strings used by more than one feature.
plugins {
    id("choreme.android.library")
    id("choreme.android.compose")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.core.ui"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
