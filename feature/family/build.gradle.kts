plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.family"
}

dependencies {
    // "Bob wants to join" alerts for parents.
    implementation(projects.core.push)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
