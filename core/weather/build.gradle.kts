// The weather forecast for the places and times of the family's routine (Open-Meteo, free, no key).
plugins {
    id("choreme.android.library")
    id("choreme.android.hilt")
}

android {
    namespace = "com.choreme.core.weather"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    // The parser reads JSON with Android's org.json, which Robolectric supplies for the tests.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
}
