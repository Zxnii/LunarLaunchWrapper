package wtf.zani.launchwrapper.loader.transformers

import net.weavemc.loader.api.util.asm
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class GenesisTransformer : Transformer("com/moonsworth/lunar/") {
    override fun transform(node: ClassNode): Boolean {
        return when {
            node.superName == "java/net/URLClassLoader" -> {
                node.interfaces.add("wtf/zani/launchwrapper/loader/ClassLoaderExtensions")

                val findClassWithSuper = MethodNode(ACC_PUBLIC, "findClassWithSuper", "(Ljava/lang/String;)Ljava/lang/Class;", null, null)

                findClassWithSuper.instructions = asm {
                    aload(0)
                    aload(1)

                    invokespecial("java/net/URLClassLoader", "findClass", "(Ljava/lang/String;)Ljava/lang/Class;")

                    areturn
                }

                node.methods.add(findClassWithSuper)

                transformClassDefintions(node)
                transformBootstrapClassLoader(node)

                true
            }
            node.name == "com/moonsworth/lunar/genesis/Genesis" -> {
                transformLegacyPrebake(node)

                true
            }
            node.name == "com/moonsworth/lunar/genesis/ClientGameBootstrap" -> {
                transformModernPrebake(node)

                true
            }
            else -> false
        }
    }

    private fun transformLegacyPrebake(node: ClassNode) {
        val main = node.methods.find { it.name == "main" }!!
        val prebakeString = main.instructions.find { it is LdcInsnNode && it.cst is String && it.cst as String == "prebake.cache" } ?: return

        main.instructions.remove(prebakeString.previous)
        main.instructions.insertBefore(
            prebakeString,
            asm {
                getstatic("wtf/zani/launchwrapper/loader/PrebakeHelper", "location", "Ljava/nio/file/Path;")
            })
    }

    private fun transformModernPrebake(node: ClassNode) {
        val apply = node.methods.find { it.name == "apply" }!!

        val bakeString = apply.instructions.find { it is LdcInsnNode && it.cst is String && it.cst as String == "bake.cache" } ?: return
        val variable = if (bakeString.previous is VarInsnNode) { (bakeString.previous as VarInsnNode).`var` } else return

        val store = apply.instructions.find {
            it is VarInsnNode
                    && it.`var` == variable
                    && it.opcode == ASTORE
                    && it.previous is MethodInsnNode
                    && (it.previous as MethodInsnNode).owner == "java/nio/file/Path"
                    && (it.previous as MethodInsnNode).name == "resolve" }

        apply.instructions.insertBefore(
            store,
            asm {
                pop
                getstatic("wtf/zani/launchwrapper/loader/PrebakeHelper", "location", "Ljava/nio/file/Path;")
            })
    }

    private fun transformClassDefintions(node: ClassNode) {
        node.methods.forEach { method ->
            method
                .instructions
                .forEach { insn ->
                    if (insn is MethodInsnNode) {
                        when {
                            insn.owner == node.name && insn.name == "defineClass" -> {
                                insn.owner = "wtf/zani/launchwrapper/loader/DefinitionProxy"
                                insn.desc = "(Ljava/lang/ClassLoader;Ljava/lang/String;[BII)Ljava/lang/Class;"
                                insn.opcode = INVOKESTATIC
                            }
                            insn.owner == node.superName && insn.name == "findClass" && method.name == "findClass" -> {
                                insn.owner = "wtf/zani/launchwrapper/loader/DefinitionProxy"
                                insn.desc = "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;"
                                insn.opcode = INVOKESTATIC
                            }
                        }
                    }
                }
        }
    }

    private fun transformBootstrapClassLoader(node: ClassNode) {
        val ctor = node.methods.find { it.name == "<init>" }!!
        val concatInsn = ctor.instructions.find { it is InvokeDynamicInsnNode && it.bsmArgs[0] is String && it.bsmArgs[0] == "Bootstrap#\u0001" } ?: return

        ctor.instructions.insertBefore(
            concatInsn,
            asm {
                aload(0)
                invokestatic("wtf/zani/launchwrapper/loader/BootstrapProxy", "addUrls", "(Ljava/net/URLClassLoader;)V")
            })
    }
}
