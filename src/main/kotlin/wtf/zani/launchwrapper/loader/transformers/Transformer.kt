package wtf.zani.launchwrapper.loader.transformers

import org.objectweb.asm.tree.ClassNode

abstract class Transformer(vararg val classNames: String, val exact: Boolean = false) {
    abstract fun transform(node: ClassNode)
}
