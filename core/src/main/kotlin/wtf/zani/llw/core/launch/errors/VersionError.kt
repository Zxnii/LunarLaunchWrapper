package wtf.zani.llw.core.launch.errors

import io.github.z4kn4fein.semver.Version

data class UnsupportedVersionError internal constructor(
    val minimum: Version,
    val maximum: Version,
    val version: Version
)