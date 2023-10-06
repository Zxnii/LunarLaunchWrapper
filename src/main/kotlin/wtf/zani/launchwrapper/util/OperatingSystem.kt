package wtf.zani.launchwrapper.util

enum class OperatingSystem(
    val internalName: String,
    val libraryPrefix: String = "",
    val librarySuffix: String
) {
    WINDOWS("win32", librarySuffix = ".dll"),
    LINUX("linux", "lib", ".so"),
    MACOS("darwin", "lib", ".dylib");

    fun version(): String = System.getProperty("os.version")
}
