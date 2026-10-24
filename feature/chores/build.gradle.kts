plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.chores"
}

dependencies {
    // Morning reminders.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // Asking for notification permission.
    implementation(libs.androidx.activity.compose)
}
