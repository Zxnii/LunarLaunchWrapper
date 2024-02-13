package wtf.zani.launchwrapper.loader

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import wtf.zani.launchwrapper.llwDir
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

        val transformersRan = getTransformers(node.name).map { it.transform(node) }.contains(true)

        if (!transformersRan) return null

        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)

        node.accept(writer)

        val bytecode = writer.toByteArray()

        if (System.getProperty("llw.dumpBytecode") == "true") dumpClass(node.name, bytecode)

        return Pair(node.name, bytecode)
    }

    fun getTransformers(target: String): List<Transformer> =
        transformers.filter {
            (it.classNames.find { name -> target.startsWith(name) } != null && !it.exact) || it.classNames.find { name -> name == target } != null
        }

    fun addTransformer(transformer: Transformer) {
        transformers.add(transformer)
    }

    private fun dumpClass(name: String, bytecode: ByteArray) {
        val dumpPath = llwDir.resolve("class_dump/$name.class")

        dumpPath.parent.toFile().mkdirs()
        dumpPath.toFile().writeBytes(bytecode)
    }
}
