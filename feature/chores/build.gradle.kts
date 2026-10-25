plugins {
    id("choreme.android.feature")
    id("choreme.android.compose.tests")
}

android {
    namespace = "com.choreme.feature.chores"
}

dependencies {
    // Telling the family's other phones when a chore is added.
    implementation(projects.core.push)
    implementation(projects.core.settings)
    // Putting a chore on the family calendar.
    implementation(projects.core.calendar)
    // Morning reminders.
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // Asking for notification permission.
    implementation(libs.androidx.activity.compose)
}
