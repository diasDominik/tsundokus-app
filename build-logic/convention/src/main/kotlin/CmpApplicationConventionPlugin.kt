import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import uk.tsundokus.convention.applyHierarchyTemplate
import uk.tsundokus.convention.configureCommonCompose
import uk.tsundokus.convention.configureDesktopTarget
import uk.tsundokus.convention.configureIosTargets
import uk.tsundokus.convention.findPluginId
import uk.tsundokus.convention.libs

class CmpApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("android-kmp-library"))
                apply(libs.findPluginId("kotlin-multiplatform"))
                apply(libs.findPluginId("tsundoku-convention-cmp-resources"))
                apply(libs.findPluginId("kotlin-serialization"))
            }

            configureIosTargets()
            configureDesktopTarget()
            configureCommonCompose()

            extensions.configure<KotlinMultiplatformExtension> {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs {
                    browser()
                    binaries.executable()
                }
                applyHierarchyTemplate()

                compilerOptions {
                    freeCompilerArgs.addAll(
                        "-Xexpect-actual-classes",
                    )
                }
            }

            dependencies {
                // Single-variant model: use androidMainImplementation instead of debugImplementation
                "androidMainImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
            }
        }
    }
}
