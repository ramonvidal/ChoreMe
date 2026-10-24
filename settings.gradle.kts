pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ChoreMe"

// See CLAUDE.md, "Module structure", for what may depend on what.
include(":app")
include(":core:model")
include(":core:data")
include(":core:ui")
include(":core:calendar")
include(":core:location")
include(":feature:onboarding")
include(":feature:chores")
include(":feature:groceries")
include(":feature:agenda")
include(":feature:map")
include(":feature:family")
