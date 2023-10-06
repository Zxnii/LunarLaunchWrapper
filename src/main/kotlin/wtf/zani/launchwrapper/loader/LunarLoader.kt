package wtf.zani.launchwrapper.loader

import java.net.URL
import java.net.URLClassLoader

class LunarLoader(urls: Array<URL>) : URLClassLoader(urls) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            return findLoadedClass(name) ?: run {
                val data = getResourceAsStream("${name.replace(".", "/")}.class")
                    .use { it?.readBytes() ?: return super.loadClass(name, resolve) }

                val transformed = TransformationHandler.transformClass(data) ?: return super.loadClass(name, resolve)
                val clazz =
                    defineClass(transformed.first.replace("/", "."), transformed.second, 0, transformed.second.size)

                resolveClass(clazz)

                return clazz
            }
        }
    }
}
