package wtf.zani.launchwrapper.loader

import wtf.zani.launchwrapper.loader.LibraryLoader

@Suppress("unused")
object LibraryLoaderProxy {
    @JvmStatic
    fun loadLibrary(name: String) {
        LibraryLoader.instance.loadLibrary(name)
    }
}
