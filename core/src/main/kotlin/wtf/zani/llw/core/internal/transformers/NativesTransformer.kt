package wtf.zani.llw.core.internal.transformers

import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import wtf.zani.llw.core.asm.Transformer

object NativesTransformer : Transformer {
    override val classNames = listOf("")
    override val exact = false

    override fun transform(node: ClassNode): Boolean {
        var any = false
        
        node.methods.forEach { m ->
            m.instructions.forEach { i ->
                if (
                    i is MethodInsnNode
                    && i.opcode == INVOKESTATIC
                    && i.owner == "java/lang/System"
                    && i.name == "loadLibrary"
                ) {
                    i.owner = "wtf/zani/llw/core/internal/LibraryLoaderKt"
                    any = true
                }
            }
        }
        
        return any
    }
}