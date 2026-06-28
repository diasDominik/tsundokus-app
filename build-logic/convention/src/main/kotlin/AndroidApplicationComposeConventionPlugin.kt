import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import uk.tsundokus.convention.configureAndroidCompose
import uk.tsundokus.convention.findPluginId
import uk.tsundokus.convention.libs

class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("tsundoku-convention-android-application"))
                apply(libs.findPluginId("compose-compiler"))
            }

            val extension = extensions.getByType<ApplicationExtension>()
            configureAndroidCompose(extension)
        }
    }
}
