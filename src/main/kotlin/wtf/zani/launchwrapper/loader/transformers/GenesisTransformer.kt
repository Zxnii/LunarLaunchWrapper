package wtf.zani.launchwrapper.loader.transformers

import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

class GenesisTransformer : Transformer("com/moonsworth/lunar/") {
    override fun transform(node: ClassNode) {
        if (node.superName == "java/net/URLClassLoader") {
            node.methods.forEach { method ->
                method
                    .instructions
                    .forEach { insn ->
                        if (insn is MethodInsnNode && insn.owner == node.name && insn.name == "defineClass") {
                            insn.owner = "wtf/zani/launchwrapper/loader/DefinitionProxy"
                            insn.desc = "(Ljava/lang/ClassLoader;Ljava/lang/String;[BII)Ljava/lang/Class;"
                            insn.opcode = INVOKESTATIC
                        }
                    }
            }
        }
    }
}
