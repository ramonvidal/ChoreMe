plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.rooms"
}

dependencies {
    // Picking a picture of the room.
    implementation(libs.androidx.activity.compose)
}
