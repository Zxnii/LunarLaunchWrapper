package wtf.zani.launchwrapper.loader

import java.net.URL
import java.net.URLClassLoader

@Suppress("unused")
object BootstrapProxy {
    @JvmStatic
    lateinit var urls: Array<URL>

    @JvmStatic
    fun addUrls(classLoader: URLClassLoader) {
        val addUrl = classLoader::class.java.getMethod("addURL", URL::class.java)
        addUrl.isAccessible = true

        urls.forEach { addUrl.invoke(classLoader, it) }
    }
}
