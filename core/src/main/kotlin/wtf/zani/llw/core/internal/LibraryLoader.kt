package wtf.zani.llw.core.internal

import wtf.zani.llw.core.SystemInfo
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists

private val search =
    System
        .getProperty("llw.internal.natives")
        .split(File.pathSeparator)
        .map { p -> Path(p) }
private val systemInfo = SystemInfo.get().getOrNull()!!

// called from transformed code
@Suppress("unused")
fun loadLibrary(name: String) {
    val completeName = "${systemInfo.os.libraryPrefix}$name${systemInfo.os.librarySuffix}"

    search.forEach { p ->
        val resolved = p.resolve(completeName)

        if (resolved.exists()) {
            System.load(resolved.toString())

            return
        }
    }

    throw UnsatisfiedLinkError("no $name in native search path: ${search.joinToString(File.pathSeparator) { it.toString() }}")
}