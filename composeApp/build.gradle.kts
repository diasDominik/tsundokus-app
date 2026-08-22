import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension

plugins {
    alias(libs.plugins.tsundoku.convention.cmp.application)
    alias(libs.plugins.tsundoku.convention.buildkonfig)
    alias(libs.plugins.tsundoku.convention.koin)
}

kotlin {
    extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
        androidResources { enable = true }
        namespace = "uk.tsundokus.composeApp"
        minSdk = libs.versions.android.sdk.min.get().toInt()
        compileSdk {
            version = release(libs.versions.android.sdk.compile.major.get().toInt()) {
                minorApiLevel = libs.versions.android.sdk.compile.minor.get().toInt()
            }
        }
        withHostTest { }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(projects.core.presentation)
            implementation(projects.features.authentication.data)
            implementation(projects.features.authentication.domain)
            implementation(projects.features.authentication.presentation)
            implementation(projects.features.orders.data)
            implementation(projects.features.orders.database)
            implementation(projects.features.orders.domain)
            implementation(projects.features.orders.presentation)
            implementation(projects.features.settings.data)
            implementation(projects.features.settings.domain)
            implementation(projects.features.settings.presentation)

            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.material3.adaptive.navigation.suite)
            implementation(libs.bundles.koin.common)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ksafe)
            implementation(libs.jetbrains.material3.adaptive)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        getByName("androidHostTest").dependencies {
            implementation(libs.robolectric)
            implementation(libs.junit)
        }
    }
}
