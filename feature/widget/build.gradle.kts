// The home-screen widget: what needs doing, and a shortcut to the grocery list.
plugins {
    id("choreme.android.feature")
}

android {
    namespace = "com.choreme.feature.widget"
}

dependencies {
    implementation(projects.core.push)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.lifecycle.process)
}
