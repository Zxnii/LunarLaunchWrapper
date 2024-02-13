package wtf.zani.launchwrapper.loader

interface ClassLoaderExtensions {
    fun findClassWithSuper(name: String): Class<*>?
}
