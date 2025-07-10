package wtf.zani.llw.core.launch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.io.path.exists
import kotlin.io.path.inputStream

private const val CHUNK_SIZE = 1024 * 1024

@OptIn(ExperimentalStdlibApi::class)
suspend fun verifyFile(file: Path, hash: String): Boolean = coroutineScope {
    if (!file.exists()) return@coroutineScope false
    
    async(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("sha-1")

        file
            .inputStream(StandardOpenOption.READ)
            .use { s ->
                var read = s.readNBytes(CHUNK_SIZE)
                
                digest.update(read)
                
                while (read.size == CHUNK_SIZE) {
                    read = s.readNBytes(CHUNK_SIZE)
                    digest.update(read)
                }
            }
        
        digest.digest().toHexString().lowercase() == hash.lowercase()
    }.await()
}