plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.update"
}

dependencies {
    implementation(projects.core.update)
}
