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
                implementation(projects.features.orders.database)
                implementation(projects.features.orders.domain)

                implementation(libs.androidx.room3.runtime)
                implementation(libs.bundles.ktor.common)
                implementation(libs.koin.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.koin.android)
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }

        wasmJsMain {
            dependencies {
                implementation(projects.features.orders.sqliteWasmWorker)

                implementation(libs.androidx.sqlite.web)
                implementation(libs.kotlinx.browser)
                implementation(libs.ktor.client.js)
            }
        }

        nativeMain {
            dependencies {
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.ktor.client.darwin)
            }
        }

        desktopMain {
            dependencies {
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}
