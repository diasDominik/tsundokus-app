import uk.tsundokus.convention.appVersion
import uk.tsundokus.convention.versionCodeOf

plugins {
    alias(libs.plugins.tsundoku.convention.android.application.compose)
}

android {
    namespace = "uk.tsundokus.androidapp"

    // From the git tag only (see appVersion in build-logic).
    val appVersion = appVersion()

    // Release builds in CI are signed with the keystore they name: the upload keystore for Play, the
    // distribution keystore for the APK published elsewhere. Local builds fall back to debug.
    val runsCIReleaseBuild = System.getenv("SIGNING_STORE_PASSWORD") != null

    defaultConfig {
        applicationId = "uk.tsundokus"
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("SIGNING_STORE_FILE") ?: "keystore/upload_keystore.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName(if (runsCIReleaseBuild) "release" else "debug")
        }
    }

    lint {
        if (System.getenv("CI") != null) {
            disable +=
                setOf(
                    "GradleDependency",
                    "GradlePluginVersion",
                    "AndroidGradlePluginVersion",
                )
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true

        // Robolectric emulates SDK 37's ApplicationSharedMemory, which pokes at raw
        // FileDescriptor internals through jdk.internal.access — a package the JDK keeps
        // closed to unnamed modules unless it is exported explicitly.
        unitTests.all { test ->
            test.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
        }
    }
}

dependencies {
    implementation(projects.composeApp)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
}
