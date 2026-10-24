// Firebase: auth and Firestore repositories.
plugins {
    id("choreme.android.library")
    id("choreme.android.hilt")
}

android {
    namespace = "com.choreme.core.data"
}

dependencies {
    // Repositories take and return model types, so callers get them too.
    api(projects.core.model)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
}
