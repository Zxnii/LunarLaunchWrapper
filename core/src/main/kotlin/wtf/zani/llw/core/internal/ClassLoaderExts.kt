package wtf.zani.llw.core.internal

// called from transformed code
@Suppress("unused")
interface ClassLoaderExts {
    fun findClassWithSuper(name: String): Class<*>?
}