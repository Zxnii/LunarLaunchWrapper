package wtf.zani.llw.core.internal

import java.net.URLClassLoader
import java.nio.file.Path

internal class LaunchClassLoader(artifacts: Collection<Path>)
    : URLClassLoader(artifacts.map { a -> a.toUri().toURL() }.toTypedArray())
{
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("wtf.zani.llw.core.internal.fixes"))
            return super.loadClass(name, resolve)

        synchronized(getClassLoadingLock(name)) {
            return findLoadedClass(name) ?: run {
                val data = getResourceAsStream("${name.replace(".", "/")}.class")
                    .use { it?.readBytes() ?: return super.loadClass(name, resolve) }

                val transformed =
                    Transformers.transform(data)
                        ?: return super.loadClass(name, resolve)
                val clazz =
                    defineClass(
                        transformed.first.replace("/", "."),
                        transformed.second,
                        0,
                        transformed.second.size
                    )

                resolveClass(clazz)

                return clazz
            }
        }
    }
}