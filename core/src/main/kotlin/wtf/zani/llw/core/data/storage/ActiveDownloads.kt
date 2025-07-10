package wtf.zani.llw.core.data.storage

import kotlinx.serialization.Serializable
import java.nio.file.Path

typealias ActiveDownloads = MutableMap<String, ActiveDownload>

@Serializable
data class ActiveDownload(
    val hash: String,
    val name: String,
    val path: Path,
    val url: String
)