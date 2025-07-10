package wtf.zani.llw.core.data

import kotlinx.serialization.Serializable

@Serializable
data class IndexEntry(
    val name: String,
    val hash: String,
    val size: ULong,
    val mtime: ULong
)

@Serializable
data class AssetIndex(
    val baseUrl: String,
    val hash: String,
    val entries: List<IndexEntry>
) {
    companion object {
        fun parse(
            baseUrl: String,
            hash: String,
            index: String
        ): AssetIndex =
            AssetIndex(
                baseUrl,
                hash,
                index
                    .split("\n")
                    .map { l -> l.trim() }
                    .filter { l -> l.isNotEmpty() }
                    .map { l ->
                        val (
                            name,
                            hash,
                            size,
                            mtime
                        ) = l.split(" ")

                        IndexEntry(name, hash, size.toULong(), mtime.toULong())
                    }
            )
    }
}
