package wtf.zani.launchwrapper.util

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode

fun getStrings(node: ClassNode) =
    node.methods.flatMap {
        it.instructions
            .filter { insn -> insn is LdcInsnNode && insn.cst is String }
            .map { insn -> (insn as LdcInsnNode).cst as String }
    }
