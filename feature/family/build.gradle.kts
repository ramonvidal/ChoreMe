plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.family"
}

dependencies {
    // "Bob wants to join" alerts for parents.
    implementation(projects.core.push)
    implementation(projects.core.settings)
    // Confirming the Google sign-in before deleting an account.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
