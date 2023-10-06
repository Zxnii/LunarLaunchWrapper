package wtf.zani.launchwrapper.loader.transformers

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

class LibraryTransformer : Transformer(
    "com/luciad/imageio/webp/NativeLibraryUtils",
    exact = true
) {
    override fun transform(node: ClassNode) {
        println("transforming ${node.name}")

        node.methods.forEach { method ->
            method
                .instructions
                .forEach {
                    if (it is MethodInsnNode && it.owner == "java/lang/System" && it.name == "loadLibrary") {
                        it.owner = "wtf/zani/launchwrapper/loader/LibraryLoaderProxy"
                    }
                }
        }
    }
}
