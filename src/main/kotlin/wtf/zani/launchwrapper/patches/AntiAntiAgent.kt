package wtf.zani.launchwrapper.patches

import net.weavemc.loader.api.util.asm
import org.objectweb.asm.tree.ClassNode
import wtf.zani.launchwrapper.loader.TransformationHandler
import wtf.zani.launchwrapper.loader.transformers.Transformer
import wtf.zani.launchwrapper.util.getStrings

class AntiAntiAgent {
    init {
        TransformationHandler.addTransformer(AntiAntiAgentTransformer())
    }
}

class AntiAntiAgentTransformer : Transformer("com/moonsworth/lunar/client/") {
    override fun transform(node: ClassNode): Boolean {
        if (!getStrings(node).contains("-javaagent")) return false

        val getAgents = node.methods
            .find { it.desc == "()Ljava/util/List;" } ?: return false

        getAgents.instructions = asm {
            new("java/util/ArrayList")
            dup
            invokespecial("java/util/ArrayList", "<init>", "()V")
            areturn
        }

        return true
    }
}
