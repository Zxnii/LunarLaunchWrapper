package wtf.zani.llw.core

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import wtf.zani.llw.core.data.schemas.request.OperatingSystem

data class SystemInfo(
    val os: OperatingSystem,
    val release: String,
    val architecture: String
) {
    companion object {
        fun get(): Option<SystemInfo> {
            return Some(
                SystemInfo(
                    OperatingSystem.get().getOrElse { return None },
                    System.getProperty("os.version"),
                    when (val arch = System.getProperty("os.arch")) {
                        "x86" -> "ia32"
                        "amd64" -> "x64"
                        "aarch64" -> "arm64"
                        else -> arch
                    }
                )
            )
        }
    }
}