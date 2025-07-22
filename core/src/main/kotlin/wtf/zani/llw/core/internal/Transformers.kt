package wtf.zani.llw.core.internal

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import wtf.zani.llw.core.asm.Transformer
import wtf.zani.llw.core.internal.transformers.AuthenticatorTransformer
import wtf.zani.llw.core.internal.transformers.GenesisTransformer
import wtf.zani.llw.core.internal.transformers.NativesTransformer
import wtf.zani.llw.core.llwRoot
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

internal object Transformers {
    private val transformers = mutableListOf(GenesisTransformer, NativesTransformer, AuthenticatorTransformer)
    
    fun transform(data: ByteArray): Pair<String, ByteArray>? {
        val node = ClassNode()
        ClassReader(data).accept(node, 0)

        val transformersRan =
            getTransformers(node.name)
                .map { it.transform(node) }
                .contains(true)
        if (!transformersRan) return null

        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)

        node.accept(writer)

        val bytecode = writer.toByteArray()
        if (System.getProperty("llw.debug.dumpBytecode") == "true") dumpClass(node.name, bytecode)

        return Pair(node.name, bytecode)
    }

    fun getTransformers(target: String): List<Transformer> =
        transformers.filter { t ->
            (t.classNames.find { name -> target.startsWith(name) } != null && !t.exact)
            || t.classNames.find { name -> name == target } != null
        }

    private fun dumpClass(name: String, bytecode: ByteArray) {
        val dumpPath = llwRoot.resolve("dump/classes/$name.class")

        dumpPath.parent.createDirectories()
        dumpPath.writeBytes(bytecode)
    }
}