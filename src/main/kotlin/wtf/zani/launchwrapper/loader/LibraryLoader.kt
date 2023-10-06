package wtf.zani.launchwrapper.loader

import wtf.zani.launchwrapper.createLibraryLoader
import wtf.zani.launchwrapper.util.SystemInfo
import java.nio.file.Path
import kotlin.io.path.exists

class LibraryLoader(private val search: Array<Path>) {
    private val systemInfo = SystemInfo.get()

    fun loadLibrary(name: String) {
        val completeName = "${systemInfo.os.libraryPrefix}$name${systemInfo.os.librarySuffix}"

        search.forEach {
            val resolved = it.resolve(completeName)

            if (resolved.exists()) {
                System.load(resolved.toString())

                return
            }
        }

        throw UnsatisfiedLinkError("no $name in native search path: ${search.joinToString(";") { it.toString() }}")
    }

    companion object {
        var instance = createLibraryLoader()
    }
}
