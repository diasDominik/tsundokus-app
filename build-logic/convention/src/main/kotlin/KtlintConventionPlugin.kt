import org.gradle.api.Plugin
import org.gradle.api.Project
import uk.tsundokus.convention.configureKtlint

class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKtlint()
        }
    }
}
