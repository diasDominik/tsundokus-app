import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import uk.tsundokus.convention.findPluginId
import uk.tsundokus.convention.libs

class KoinConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("koin-compiler"))
            }

            dependencies {
                "commonMainImplementation"(libs.findLibrary("koin-annotations").get())
            }
        }
    }
}
