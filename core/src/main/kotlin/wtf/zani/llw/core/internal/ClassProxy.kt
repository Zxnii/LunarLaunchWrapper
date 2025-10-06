package wtf.zani.llw.core.internal

import java.lang.invoke.MethodHandles
import java.security.ProtectionDomain

private val lookup = MethodHandles.lookup()
private val defineClass = run {
    val defineClassMethod = ClassLoader::class.java.getDeclaredMethod("defineClass",
        String::class.java, ByteArray::class.java, Int::class.java, Int::class.java)

    defineClassMethod.isAccessible = true
    lookup.unreflect(defineClassMethod)
}
private val defineClassWithDomain = run {
    val defineClassMethod = ClassLoader::class.java.getDeclaredMethod("defineClass",
        String::class.java, ByteArray::class.java, Int::class.java, Int::class.java, ProtectionDomain::class.java)

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

fun defineClass(instance: ClassLoader, name: String, data: ByteArray, offset: Int, length: Int, protectionDomain: ProtectionDomain): Class<*> {
    val transformed = Transformers.transform(data)
        ?: return defineClassWithDomain.invokeExact(instance,
            name, data, offset, length, protectionDomain) as Class<*>

    return defineClassWithDomain
        .invokeExact(
            instance,
            transformed.first.replace("/", "."),
            transformed.second,
            0,
            transformed.second.size,
            protectionDomain) as Class<*>
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