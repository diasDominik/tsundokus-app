plugins {
    alias(libs.plugins.tsundoku.convention.cmp.feature)
    alias(libs.plugins.aboutlibraries)
}

// Collect OSS dependency metadata into this module's compose resources so the licenses screen can
// render it. CMP targets don't auto-generate, so run :exportLibraryDefinitions to (re)create the file.
aboutLibraries {
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.designsystem)
                implementation(projects.core.domain)
                implementation(projects.core.presentation)
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
