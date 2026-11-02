package uk.tsundokus.convention

import org.gradle.api.Project

private const val RELEASE_TAG = "v[0-9]*.[0-9]*.[0-9]*"

/**
 * The app version, read from git and from nowhere else: a commit tagged vX.Y.Z builds as X.Y.Z, any
 * other commit as a development build "<latest tag>-dev" ("0.0.0-dev" before the first tag). So a
 * release can only carry the version of the tag it was built from.
 */
fun Project.appVersion(): String {
    fun git(vararg args: String): String? =
        runCatching {
            val result =
                providers.exec {
                    commandLine("git", *args)
                    workingDir = rootDir
                    isIgnoreExitValue = true
                }
            result.standardOutput.asText
                .get()
                .trim()
                .takeIf { result.result.get().exitValue == 0 && it.isNotEmpty() }
        }.getOrNull()

    git("describe", "--tags", "--exact-match", "--match", RELEASE_TAG)?.let { tag ->
        return tag.removePrefix("v").also(::versionCodeOf)
    }
    val latest = git("describe", "--tags", "--abbrev=0", "--match", RELEASE_TAG)?.removePrefix("v") ?: "0.0.0"
    return "$latest-dev"
}

/**
 * A monotonically increasing versionCode from the version ("1.2.3" -> 10203; a "-dev" suffix is
 * ignored), so every Play upload gets a higher code without manual bumping. Each part has two
 * digits, so a part above 99 would collide with the next one and is rejected. Android needs a
 * positive code, so the dev build before the first tag (0.0.0-dev) gets 1.
 */
fun versionCodeOf(version: String): Int {
    val parts = version.substringBefore('-').split(".").map { it.toIntOrNull() }
    require(parts.size == 3 && parts.all { it != null && it in 0..99 }) {
        "Version \"$version\" must be x.y.z with each part 0-99; it comes from the release tag vx.y.z"
    }
    val (major, minor, patch) = parts.map { it!! }
    return (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
}
