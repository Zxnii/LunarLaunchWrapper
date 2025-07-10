package wtf.zani.llw.core.internal.transformers

import net.weavemc.loader.api.util.asm
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import wtf.zani.llw.core.asm.Transformer

internal object GenesisTransformer : Transformer {
    override val classNames = listOf("com/moonsworth/lunar/")
    override val exact = false

    override fun transform(node: ClassNode): Boolean {
        return when {
            node.superName == "java/net/URLClassLoader" -> {
                node.interfaces.add("wtf/zani/llw/core/internal/ClassLoaderExts")

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
                transformClassPathReferences(node)
                
                true
            }
            node.name == "com/moonsworth/lunar/genesis/ClientGameBootstrap" -> {
                transformModernPrebake(node)

                true
            }
            else -> transformClassPathReferences(node)
        }
    }
    
    private fun transformClassDefintions(node: ClassNode) {
        node.methods.forEach { method ->
            method
                .instructions
                .forEach { insn ->
                    if (insn is MethodInsnNode) {
                        when {
                            insn.owner == node.name && insn.name == "defineClass" -> {
                                insn.owner = "wtf/zani/llw/core/internal/ClassProxyKt"
                                insn.desc = "(Ljava/lang/ClassLoader;Ljava/lang/String;[BII)Ljava/lang/Class;"
                                insn.opcode = INVOKESTATIC
                            }
                            insn.owner == node.superName && insn.name == "findClass" && method.name == "findClass" -> {
                                insn.owner = "wtf/zani/llw/core/internal/ClassProxyKt"
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
        val concatInsn = ctor.instructions.find { i -> i is InvokeDynamicInsnNode && i.bsmArgs[0] is String && i.bsmArgs[0] == "Bootstrap#\u0001" } ?: return

        ctor.instructions.insertBefore(
            concatInsn,
            asm {
                aload(0)
                invokestatic("wtf/zani/llw/core/internal/BootstrapProxy", "apply", "(Ljava/net/URLClassLoader;)V")
            })
    }

    private fun transformLegacyPrebake(node: ClassNode) {
        val main = node.methods.find { it.name == "main" }!!
        val prebakeString = main.instructions.find { i -> i is LdcInsnNode && i.cst is String && i.cst as String == "prebake.cache" } ?: return

        main.instructions.remove(prebakeString.previous)
        main.instructions.insertBefore(
            prebakeString,
            asm {
                getstatic("wtf/zani/llw/core/internal/PrebakeKt", "prebakeLocation", "Ljava/nio/file/Path;")
            })
    }

    private fun transformModernPrebake(node: ClassNode) {
        val apply = node.methods.find { m -> m.name == "apply" }!!

        val bakeString = apply.instructions.find { i -> i is LdcInsnNode && i.cst is String && i.cst as String == "bake.cache" } ?: return
        val variable = if (bakeString.previous is VarInsnNode) { (bakeString.previous as VarInsnNode).`var` } else return

        val store = apply.instructions.find { i ->
            i is VarInsnNode
                    && i.`var` == variable
                    && i.opcode == ASTORE
                    && i.previous is MethodInsnNode
                    && (i.previous as MethodInsnNode).owner == "java/nio/file/Path"
                    && (i.previous as MethodInsnNode).name == "resolve" }

        apply.instructions.insertBefore(
            store,
            asm {
                pop
                getstatic("wtf/zani/llw/core/internal/PrebakeKt", "prebakeLocation", "Ljava/nio/file/Path;")
            })
    }
    
    private fun transformClassPathReferences(node: ClassNode): Boolean =
        node
            .methods
            .flatMap { m -> m.instructions }
            .asSequence()
            .filterIsInstance<LdcInsnNode>()
            .filter { i -> i.cst == "java.class.path" }
            .onEach { i -> i.cst = "llw.internal.classpath" }
            .count() > 0
}