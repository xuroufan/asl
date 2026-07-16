pluginManagement {
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

rootProject.name = "ASL"

// Core modules
include(":app")
include(":core:network")
include(":core:database")
include(":core:model")
include(":core:util")

// Feature modules
include(":feature:auth")
include(":feature:market")
include(":feature:trading")
include(":feature:position")
// include(":baselineprofile") # disabled
