package wtf.zani.llw.core.internal

import java.lang.invoke.MethodHandles

private val defineClass = run {
    val defineClassMethod = ClassLoader::class.java.getDeclaredMethod("defineClass",
        String::class.java, ByteArray::class.java, Int::class.java, Int::class.java)
    val lookup = MethodHandles.lookup()

    defineClassMethod.isAccessible = true
    lookup.unreflect(defineClassMethod)
}

fun defineClass(instance: ClassLoader, name: String, data: ByteArray, offset: Int, length: Int): Class<*> {
    val transformed = Transformers.transform(data)
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

// called from transformed code
@Suppress("unused")
fun findClass(instance: ClassLoader, name: String): Class<*>? {
    val data = instance.getResourceAsStream("${name.replace(".", "/")}.class")?.readAllBytes() ?: return null

    if (Transformers.getTransformers(name.replace(".", "/")).isEmpty()) {
        return (instance as ClassLoaderExts).findClassWithSuper(name)
    }

    return defineClass(instance, name, data, 0, data.size)
}