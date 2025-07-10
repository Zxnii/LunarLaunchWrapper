package wtf.zani.llw.core.launch

import wtf.zani.llw.core.llwRoot
import java.util.UUID
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private lateinit var cachedId: UUID

fun installId(): UUID {
    if (::cachedId.isInitialized) return cachedId
    
    val file = llwRoot.resolve(".installid")
    
    cachedId = if (file.exists()) {
        try {
            UUID.fromString(file.readText(Charsets.UTF_8))
        } catch (_: Exception) {
            val id = UUID.randomUUID()
            
            file.writeText(id.toString(), Charsets.UTF_8)
            id
        }
    } else {
        val id = UUID.randomUUID()

        file.writeText(id.toString(), Charsets.UTF_8)
        id
    }
    return cachedId
}