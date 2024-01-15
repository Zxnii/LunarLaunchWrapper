package wtf.zani.launchwrapper.loader.transformers

import net.weavemc.loader.api.util.asm
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode

class GenesisTransformer : Transformer("com/moonsworth/lunar/") {
    override fun transform(node: ClassNode): Boolean {
        if (node.superName == "java/net/URLClassLoader") { transformClassLoader(node); return true }
        // if lunar ever changes the main class name llw is DONE FOR
        if (node.name == "com/moonsworth/lunar/genesis/Genesis") { transformMain(node); return true }

        return false
    }

    private fun transformMain(node: ClassNode) {
        val main = node.methods.find { it.name == "main" }!!
        val prebakeString = main.instructions.find { it is LdcInsnNode && it.cst is String && it.cst as String == "prebake.cache" }!!

        main.instructions.remove(prebakeString.previous)
        main.instructions.insertBefore(
            prebakeString,
            asm {
                getstatic("wtf/zani/launchwrapper/loader/PrebakeHelper", "location", "Ljava/nio/file/Path;")
            })
    }

    private fun transformClassLoader(node: ClassNode) {
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
