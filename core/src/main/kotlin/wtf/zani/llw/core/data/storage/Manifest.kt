package wtf.zani.llw.core.data.storage

import arrow.core.None
import arrow.core.Option
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import wtf.zani.llw.core.data.AssetIndex
import wtf.zani.llw.core.data.schemas.response.LaunchTypeData

@Serializable
data class ManifestIndexes(
    val uiAssets: AssetIndex?,
    val textures: AssetIndex
)

@Serializable
data class ManifestStoredIndexes(
    val uiAssets: String?,
    val textures: String
)

@Serializable
data class Ui(
    val url: String,
    val hash: String
)

@Serializable
data class Manifest(
    val module: String,
    val version: String,
    val branch: String,
    val launchData: LaunchTypeData,
    val uiData: Ui?,
    val downloadedAt: Instant,
    private val assetHashes: ManifestStoredIndexes,
    @Transient private val _assets: Option<ManifestIndexes> = None
) {
    val assets
        get() = _assets.getOrNull()!!
}
