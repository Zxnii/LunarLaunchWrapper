package wtf.zani.launchwrapper.loader

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import wtf.zani.launchwrapper.loader.transformers.GenesisTransformer
import wtf.zani.launchwrapper.loader.transformers.LibraryTransformer
import wtf.zani.launchwrapper.loader.transformers.Transformer

object TransformationHandler {
    private val transformers = mutableListOf(
        GenesisTransformer(), LibraryTransformer()
    )

    fun transformClass(data: ByteArray): Pair<String, ByteArray>? {
        val node = ClassNode()

        ClassReader(data).accept(node, 0)

        val transformersRan = transformers.filter {
            (it.classNames.find { name -> node.name.startsWith(name) } != null && !it.exact) || it.classNames.find { name -> name == node.name } != null
        }.map { it.transform(node) }.contains(true)

        if (!transformersRan) return null

        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)

        node.accept(writer)

        return Pair(node.name, writer.toByteArray())
    }

    fun addTransformer(transformer: Transformer) {
        transformers.add(transformer)
    }
}
