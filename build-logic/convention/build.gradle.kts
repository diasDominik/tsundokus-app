import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

group = "uk.tsundokus.convention.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.androidx.room3.gradle.plugin)
    compileOnly(libs.koin.compiler.gradle.plugin)
    implementation(libs.buildkonfig.gradle.plugin)
    implementation(libs.buildkonfig.compiler)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

ktlint {
    version.set(libs.versions.ktlint.version.get())
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "uk.tsundokus.convention.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidComposeApplication") {
            id = "uk.tsundokus.convention.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("cmpApplication") {
            id = "uk.tsundokus.convention.cmp.application"
            implementationClass = "CmpApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "uk.tsundokus.convention.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("cmpLibrary") {
            id = "uk.tsundokus.convention.cmp.library"
            implementationClass = "CmpLibraryConventionPlugin"
        }
        register("cmpFeature") {
            id = "uk.tsundokus.convention.cmp.feature"
            implementationClass = "CmpFeatureConventionPlugin"
        }
        register("cmpResources") {
            id = "uk.tsundokus.convention.cmp.resources"
            implementationClass = "CmpResourcesConventionPlugin"
        }
        register("buildKonfig") {
            id = "uk.tsundokus.convention.buildkonfig"
            implementationClass = "BuildKonfigConventionPlugin"
        }
        register("room") {
            id = "uk.tsundokus.convention.room"
            implementationClass = "RoomConventionPlugin"
        }
        register("ktlint") {
            id = "uk.tsundokus.convention.ktlint"
            implementationClass = "KtlintConventionPlugin"
        }
        register("koin") {
            id = "uk.tsundokus.convention.koin"
            implementationClass = "KoinConventionPlugin"
        }
    }
}
