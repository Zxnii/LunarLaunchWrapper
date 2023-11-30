package wtf.zani.launchwrapper.loader

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

@Suppress("unused")
object DefinitionProxy {
    private val defineClass: MethodHandle

    init {
        val defineClassMethod = ClassLoader::class.java.getDeclaredMethod("defineClass",
            String::class.java, ByteArray::class.java, Int::class.java, Int::class.java)
        val lookup = MethodHandles.lookup()

        defineClassMethod.isAccessible = true
        defineClass = lookup.unreflect(defineClassMethod)
    }

    @JvmStatic
    fun defineClass(instance: ClassLoader, name: String, data: ByteArray, offset: Int, length: Int): Class<*> {
        val transformed = TransformationHandler.transformClass(data)
            ?: return defineClass.invokeExact(instance,
                name, data, offset, length) as Class<*>

        return defineClass
            .invokeExact(
                instance,
                transformed.first.replace("/", "."),
                transformed.second,
                0,
                transformed.second.size) as Class<*>
    }
}
