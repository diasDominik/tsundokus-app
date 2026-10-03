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

                // Platform passkey ceremonies (Credential Manager / ASAuthorization / WebAuthn).
                implementation(libs.passkeys)
                implementation(libs.passkeys.compose)
                implementation(projects.features.settings.data)
                implementation(projects.features.settings.domain)
                implementation(libs.jetbrains.material3.adaptive)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.aboutlibraries.core)
                implementation(libs.aboutlibraries.compose.m3)
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
