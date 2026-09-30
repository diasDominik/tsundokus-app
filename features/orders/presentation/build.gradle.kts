plugins {
    alias(libs.plugins.tsundoku.convention.cmp.feature)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.designsystem)
                implementation(projects.core.domain)
                implementation(projects.core.presentation)
                implementation(projects.features.orders.data)
                implementation(projects.features.orders.domain)
                implementation(libs.jetbrains.material3.adaptive)
                implementation(libs.jetbrains.navigationevent.compose)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.coil.compose)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }

        // KScan gives camera scanning on Android, iOS and web. Not desktop: its desktop artifact pulls
        // in OpenCV natives for every OS, so desktop types the ISBN instead (IsbnScanner.desktop.kt).
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.work.runtime)
                implementation(libs.kscan)
            }
        }

        wasmJsMain {
            dependencies {
                implementation(libs.kscan)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        iosMain {
            dependencies {
                implementation(libs.kscan)
            }
        }
    }
}
