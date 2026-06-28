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
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }

        androidMain {
            dependencies {
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}
