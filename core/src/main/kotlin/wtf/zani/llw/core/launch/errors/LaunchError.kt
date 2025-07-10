package wtf.zani.llw.core.launch.errors

import io.github.z4kn4fein.semver.Version

sealed interface LaunchError {
    data object MissingMainMethod : LaunchError
    data class MissingMainClass internal constructor(val main: String) : LaunchError
    data class LunarException internal constructor(val exception: Throwable) : LaunchError
    @JvmInline
    value class UnsupportedVersion internal constructor(
        private val wrapped: UnsupportedVersionError
    ) : LaunchError {
        val minimum: Version
            get() = wrapped.minimum
        val maximum: Version
            get() = wrapped.maximum
        val version: Version
            get() = wrapped.version
    }
}