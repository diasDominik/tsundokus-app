plugins {
    alias(libs.plugins.tsundoku.convention.cmp.feature)
}

// See androidApp: Robolectric's SDK 37 emulation needs jdk.internal.access exported.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
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
                // The platform save dialog for exporting orders; on the web, a download.
                implementation(libs.filekit.dialogs.compose)
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
                // Home-screen widgets.
                implementation(libs.glance.appwidget)
                implementation(libs.glance.material3)
            }
        }

        // Robolectric and WorkManager's test helpers run the reminder scheduler against a real
        // WorkManager on the JVM.
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.work.testing)
                implementation(libs.glance.appwidget.testing)
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
