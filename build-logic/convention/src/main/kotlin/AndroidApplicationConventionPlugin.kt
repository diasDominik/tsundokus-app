import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import uk.tsundokus.convention.configureKotlinAndroid
import uk.tsundokus.convention.configureKtlint
import uk.tsundokus.convention.findPluginId
import uk.tsundokus.convention.libs

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply(libs.findPluginId("android-application"))
            }

            extensions.configure<ApplicationExtension> {
                defaultConfig.targetSdk =
                    libs
                        .findVersion("android-sdk-target")
                        .get()
                        .toString()
                        .toInt()

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }

                buildTypes {
                    release {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }

                configureKotlinAndroid(this)
                configureKtlint()
            }
        }
    }
}
