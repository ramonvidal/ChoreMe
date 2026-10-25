plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "choreme.android.application"
            implementationClass = "com.choreme.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "choreme.android.library"
            implementationClass = "com.choreme.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "choreme.android.compose"
            implementationClass = "com.choreme.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "choreme.android.hilt"
            implementationClass = "com.choreme.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "choreme.android.feature"
            implementationClass = "com.choreme.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidComposeTests") {
            id = "choreme.android.compose.tests"
            implementationClass = "com.choreme.buildlogic.AndroidComposeTestsConventionPlugin"
        }
        register("jvmLibrary") {
            id = "choreme.jvm.library"
            implementationClass = "com.choreme.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
