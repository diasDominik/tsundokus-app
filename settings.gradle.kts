rootProject.name = "TsundokuApp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

includeBuild("build-logic")
include(":androidApp")
include(":composeApp")
include(":core:data")
include(":core:designsystem")
include(":core:domain")
include(":core:presentation")
include(":features:authentication:data")
include(":features:authentication:domain")
include(":features:authentication:presentation")
include(":features:authentication:testing")
include(":features:orders:data")
include(":features:orders:database")
include(":features:orders:domain")
include(":features:orders:presentation")
include(":features:orders:sqliteWasmWorker")
include(":features:settings:data")
include(":features:settings:domain")
include(":features:settings:presentation")
