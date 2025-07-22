package wtf.zani.llw.core.internal.transformers

import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import wtf.zani.llw.core.asm.Transformer

internal object AuthenticatorTransformer : Transformer {
    override val classNames = listOf("com/moonsworth/lunar/client/")
    override val exact: Boolean = false

    override fun transform(node: ClassNode): Boolean {
        node.methods.forEach { m ->
            m.instructions.find { i -> i is LdcInsnNode && i.cst == "application/x-protobuf" } ?: return@forEach
            val mapInstruction = m.instructions.find { i ->
                i is MethodInsnNode
                        && i.owner == "java/util/Map"
                        && i.name == "of"
            } as MethodInsnNode? ?: return@forEach
            
            mapInstruction.owner = "wtf/zani/llw/core/internal/AuthenticatorHeaderProxyKt"
            mapInstruction.name = "create"
            mapInstruction.opcode = INVOKESTATIC
            mapInstruction.itf = false
            
            return true
        }
        
        return false
    }
}