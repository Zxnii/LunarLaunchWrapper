package wtf.zani.llw.core.asm

import org.objectweb.asm.tree.ClassNode

interface Transformer {
    val classNames: List<String>
    val exact: Boolean
    
    fun transform(node: ClassNode): Boolean
}