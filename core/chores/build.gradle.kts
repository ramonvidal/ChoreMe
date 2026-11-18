// Doing things to chores the same way on every screen: the rules from :core:model, saved through
// :core:data, the family pinged through :core:push. Used by Chores, Rooms and Casa.
plugins {
    id("choreme.android.library")
    id("choreme.android.hilt")
}

android {
    namespace = "com.choreme.core.chores"
}

dependencies {
    // Callers pass and get chores and members.
    api(projects.core.model)
    implementation(projects.core.data)
    implementation(projects.core.push)
    implementation(libs.kotlinx.coroutines.android)
}
