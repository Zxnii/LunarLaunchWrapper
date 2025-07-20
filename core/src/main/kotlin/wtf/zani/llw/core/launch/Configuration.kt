package wtf.zani.llw.core.launch

import io.github.z4kn4fein.semver.Version
import wtf.zani.llw.core.MAXIMUM_SUPPORTED_LAUNCHER_VERSION
import wtf.zani.llw.core.data.AssetIndex
import wtf.zani.llw.core.data.schemas.response.Artifact
import wtf.zani.llw.core.data.storage.Ui
import java.nio.file.Path
import kotlin.math.max

data class DownloadConfiguration(
    val maxArtifactConcurrency: Int = Runtime.getRuntime().availableProcessors(),
    val maxAssetConcurrency: Int = max(Runtime.getRuntime().availableProcessors() * 4, 64),
    val artifacts: List<Artifact>,
    val assets: AssetIndex,
    val ui: Ui?,
    val uiAssets: AssetIndex?
)

data class LaunchConfiguration(
    val multiverDirectory: Path,
    val textureDirectory: Path,
    val uiDirectory: Path?,
    val minecraftVersion: Version,
    val additionalArgs: Collection<String> = listOf(),
    val launcherVersion: Version = MAXIMUM_SUPPORTED_LAUNCHER_VERSION,
    val installId: String = installId().toString()
)

data class ArtifactConfiguration(
    val mainClass: String,
    val classPath: Set<Path>,
    val externalFiles: Set<Path>,
    val natives: Set<Path>
)