plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.groceries"
}

dependencies {
    // "Mom is at the shop" on the other phones.
    implementation(projects.core.push)
    implementation(projects.core.settings)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
}
