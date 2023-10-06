package wtf.zani.launchwrapper.util

data class SystemInfo(
    val os: OperatingSystem,
    val arch: String
) {
    companion object {
        fun get(): SystemInfo {
            val osName = System.getProperty("os.name").lowercase()

            val os = when {
                osName.startsWith("windows") -> OperatingSystem.WINDOWS
                osName.contains("macos")
                        || osName.contains("darwin") -> OperatingSystem.MACOS
                osName.contains("linux") -> OperatingSystem.LINUX
                else -> throw Exception("Unsupported operating system")
            }

            return SystemInfo(
                os,
                when (val arch = System.getProperty("os.arch")) {
                    "x86" -> "ia32"
                    "amd64" -> "x64"
                    "aarch64" -> "arm64"
                    else -> arch
                }
            )
        }
    }
}
