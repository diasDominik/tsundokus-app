plugins {
    alias(libs.plugins.tsundoku.convention.kmp.library)
    alias(libs.plugins.tsundoku.convention.koin)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.data)
                implementation(projects.core.domain)
                implementation(projects.features.settings.domain)

                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        webMain {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}
