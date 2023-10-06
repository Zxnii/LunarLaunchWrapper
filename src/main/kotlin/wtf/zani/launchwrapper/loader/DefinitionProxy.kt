package wtf.zani.launchwrapper.loader

import java.net.URLClassLoader

@Suppress("unused")
object DefinitionProxy {
    private val defineClass = ClassLoader::class.java.getDeclaredMethod("defineClass",
        String::class.java, ByteArray::class.java, Int::class.java, Int::class.java)

    init {
        defineClass.isAccessible = true
    }

    @JvmStatic
    fun defineClass(instance: URLClassLoader, name: String, data: ByteArray, offset: Int, length: Int): Class<*> {
        val transformed = TransformationHandler.transformClass(data)
            ?: return defineClass.invoke(instance,
                name, data, offset, length) as Class<*>

        return defineClass
            .invoke(
                instance,
                transformed.first.replace("/", "."),
                transformed.second,
                0,
                transformed.second.size) as Class<*>
    }
}
