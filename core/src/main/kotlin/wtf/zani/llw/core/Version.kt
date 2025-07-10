package wtf.zani.llw.core

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.z4kn4fein.semver.Version
import wtf.zani.llw.core.launch.errors.UnsupportedVersionError

val MAXIMUM_SUPPORTED_LAUNCHER_VERSION = Version.parse("3.3.7")
val MINIMUM_SUPPORTED_LAUNCHER_VERSION = Version.parse("3.3.7")

internal fun checkVersion(version: Version): Either<UnsupportedVersionError, Unit> = either {
    ensure(!(version < MINIMUM_SUPPORTED_LAUNCHER_VERSION || version > MAXIMUM_SUPPORTED_LAUNCHER_VERSION)) {
        UnsupportedVersionError(
            MINIMUM_SUPPORTED_LAUNCHER_VERSION,
            MAXIMUM_SUPPORTED_LAUNCHER_VERSION,
            version
        )
    }
}