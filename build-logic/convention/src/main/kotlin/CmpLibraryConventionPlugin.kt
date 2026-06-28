import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import uk.tsundokus.convention.findPluginId
import uk.tsundokus.convention.libs

class CmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("tsundoku-convention-kmp-library"))
                apply(libs.findPluginId("tsundoku-convention-cmp-resources"))
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-foundation").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-material3").get())
                "commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui-tooling-preview").get())

                "androidMainImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                "androidRuntimeClasspath"(libs.findLibrary("jetbrains-compose-ui-tooling-preview").get())
            }
        }
    }
}
