package wtf.zani.llw.core.data.schemas.request

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.lang3.SystemUtils

@Serializable
data class LaunchRequestBody(
    val os: OperatingSystem,
    @SerialName("os_release") val osRelease: String,
    @SerialName("installation_id") val installationId: String,
    val arch: String,
    @SerialName("launcher_version") val launcherVersion: String,
    @SerialName("launch_type") val launchType: LaunchType,
    val branch: String,
    val version: String,
    val module: String,
    @SerialName("canary_preference") val canaryPreference: CanaryPreference
)

enum class LaunchType {
    @SerialName("OFFLINE") Offline;
}

enum class OperatingSystem(val librarySuffix: String = ".so", val libraryPrefix: String = "lib") {
    @SerialName("win32") Windows(".dll", ""),
    @SerialName("darwin") MacOS(".dylib"),
    @SerialName("linux") Linux,
    @SerialName("freebsd") FreeBsd,
    @SerialName("openbsd") OpenBsd;
    
    companion object {
        fun get(): Option<OperatingSystem> = when {
            SystemUtils.IS_OS_WINDOWS -> Some(Windows)
            SystemUtils.IS_OS_LINUX -> Some(Linux)
            SystemUtils.IS_OS_MAC -> Some(MacOS)
            SystemUtils.IS_OS_FREE_BSD -> Some(FreeBsd)
            SystemUtils.IS_OS_OPEN_BSD -> Some(OpenBsd)
            else -> None
        }
    }
}

enum class CanaryPreference {
    @SerialName("OPT_IN") OptIn,
    @SerialName("OPT_OUT") OptOut,
    @SerialName("NEUTRAL") Neutral;
}