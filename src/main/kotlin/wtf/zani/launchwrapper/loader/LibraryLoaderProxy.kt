package wtf.zani.launchwrapper.loader

@Suppress("unused")
object LibraryLoaderProxy {
    @JvmStatic
    fun loadLibrary(name: String) {
        LibraryLoader.instance.loadLibrary(name)
    }
}
