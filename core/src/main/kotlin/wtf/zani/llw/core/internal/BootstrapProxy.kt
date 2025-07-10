package wtf.zani.llw.core.internal

import java.net.URL
import java.net.URLClassLoader

// called via reflection & transformed code
@Suppress("unused")
object BootstrapProxy {
    private val urls = mutableListOf<URL>()
    
    @JvmStatic
    fun apply(classLoader: URLClassLoader) {
        val addUrl = classLoader::class.java
            .getMethod("addURL", URL::class.java)
            .also { m -> m.isAccessible = true }
        
        urls.forEach { u -> addUrl(classLoader, u) }
    }

    @JvmStatic
    fun addUrl(url: URL) { urls.add(url) }
}