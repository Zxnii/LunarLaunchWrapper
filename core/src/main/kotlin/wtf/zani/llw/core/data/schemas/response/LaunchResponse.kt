package wtf.zani.llw.core.data.schemas.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LaunchResponse(
    val success: Boolean,
    val updateAssets: Boolean,
    val launchTypeData: LaunchTypeData,
    val licenses: List<License>,
    val textures: Textures,
    val jre: Jre,
    val ui: Ui
)

@Serializable
data class LaunchTypeData(
    val artifacts: List<Artifact>,
    val mainClass: String
)

@Serializable
data class Artifact(
    val name: String,
    val sha1: String,
    val url: String,
    val differentialUrl: String?,
    val type: ArtifactType,
    val size: ULong,
    val mtime: ULong
)

enum class ArtifactType {
    @SerialName("CLASS_PATH") Classpath,
    @SerialName("EXTERNAL_FILE") External,
    @SerialName("NATIVES") Natives;
}

@Serializable
data class License(
    val file: String,
    val url: String,
    val sha1: String,
    val size: ULong,
    val mtime: ULong
)

@Serializable
data class Textures(
    val indexUrl: String,
    val indexSha1: String,
    val jitIndexUrl: String,
    val jitIndexSha1: String,
    val baseUrl: String
)

@Serializable
data class Jre(
    val download: JreDownload,
    val folderChecksum: String,
    val executablePathInArchive: List<String>,
    val extraArguments: List<String>,
    val javawDownload: String? = null,
    val javawExeChecksum: String? = null
)

@Serializable
data class JreDownload(
    val url: String,
    val fallbackUrl: String,
    val extension: String
)

@Serializable
data class Ui(
    val sourceUrl: String,
    val sourceSha1: String,
    val assets: UiAssets
)

@Serializable
data class UiAssets(
    val baseUrl: String,
    val indexUrl: String,
    val indexSha1: String
)