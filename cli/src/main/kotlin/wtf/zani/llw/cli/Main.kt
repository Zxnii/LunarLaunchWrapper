package wtf.zani.llw.cli

import io.github.z4kn4fein.semver.Version
import kotlinx.cli.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import wtf.zani.llw.core.MAXIMUM_SUPPORTED_LAUNCHER_VERSION
import wtf.zani.llw.core.data.storage.Manifest
import wtf.zani.llw.core.launch.*
import wtf.zani.llw.core.launch.errors.LaunchError
import wtf.zani.llw.core.lunarRoot
import java.lang.reflect.InvocationTargetException
import kotlin.system.exitProcess

private object VersionArgType : ArgType<Version>(true) {
    override val description: kotlin.String = "{ Version }"

    override fun convert(value: kotlin.String, name: kotlin.String): Version = Version.parse(value, false)
}

fun main(args: Array<String>) {
    val parser = ArgParser("LunarLaunchWrapper")
    
    val version by parser.option(
        VersionArgType,
        "version",
        "v",
        "Minecraft version"
    ).required()
    
    val module by parser.option(
        ArgType.String,
        "module",
        "m",
        "Lunar module (ex: lunar)"
    ).required()

    val branch by parser
        .option(
            ArgType.String,
            "branch",
            "b",
            "Lunar branch (ex: master, dev)"
        )
        .default("master")
    
    val launcherVersion by
        parser
            .option(
                VersionArgType,
                "launcher-version",
                description = "Lunar Launcher Version"
            )
            .default(MAXIMUM_SUPPORTED_LAUNCHER_VERSION)
    
    val skipDownload by
        parser
            .option(
                ArgType.Boolean,
                "skip-download",
                "sd",
                "Skip downloading Lunar"
            )
            .default(false)
    val skipLaunch by
        parser
            .option(
                ArgType.Boolean,
                "skip-launch",
                "sl",
                "Skip launching Lunar"
            )
            .default(false)
    
    val extraArgs by parser
        .argument(ArgType.String, "extra-args", "Extra arguments to be passed on to Lunar")
        .vararg()
        .optional()
    
    parser.parse(args)
    
    if (skipLaunch && skipDownload) {
        println("You cannot skip both launching and downloading.")
        exitProcess(1)
    }
    
    val manifest = runBlocking { 
        fetchLatestManifest(
            version.toString(),
            branch,
            module
        )
    }
    
    if (!skipDownload) runBlocking { download(manifest) }
    else Triple(null, null, null)
    
    if (!skipLaunch) {
        val config = LaunchConfiguration(
            lunarRoot.resolve("offline/multiver"),
            lunarRoot.resolve("textures"),
            lunarRoot.resolve("ui"),
            version,
            extraArgs
        )
        val artifacts = runBlocking { prepareArtifactConfiguration(config, manifest) }

        runBlocking { 
            launch(Dispatchers.IO) { prepareTextures(config, manifest) }
            launch(Dispatchers.IO) { prepareUi(config, manifest) }
        }
        
        try {
            launch(
                config,
                artifacts
            ).onLeft { e ->
                when (e) {
                    is LaunchError.LunarException -> e.exception.printStackTrace()
                    else -> println(e)
                }

                exitProcess(1)
            }
        } catch (ex: Exception) {
            if (ex is InvocationTargetException) { (ex.cause ?: ex).printStackTrace() }
            else ex.printStackTrace()
            
            exitProcess(1)
        }
    }
    
    exitProcess(0)
}

private suspend fun Flow<ProgressReport>.downloadPipeline(): List<DownloadingFile> =
    this
        .onEach { r ->
            when (r) {
                is ProgressReport.Begin -> println("Downloading ${r.file.name}")
                is ProgressReport.Completed -> println("Downloaded ${r.file.name}")
                is ProgressReport.Failed -> println("Failed to download ${r.file.name} (${r.cause})")
                else -> {}
            }
        }
        .filterIsInstance<ProgressReport.Completed>()
        .map { r -> r.file }
        .toList()

private suspend fun download(manifest: Manifest): Triple<
    List<DownloadingFile>,
    List<DownloadingFile>,
    List<DownloadingFile>?
> = coroutineScope {
    val downloader = Downloader(DownloadConfiguration(
        artifacts = manifest.launchData.artifacts,
        assets = manifest.assets.textures,
        ui = manifest.uiData,
        uiAssets = manifest.assets.uiAssets
    ))
    
    val artifactsJob = async { downloader.downloadArtifacts().downloadPipeline() }
    val texturesJob = async { downloader.downloadAssets().downloadPipeline() }
    val uiJob = async { downloader.downloadUiAssets()?.downloadPipeline() }

    launch { downloader.downloadUi()?.downloadPipeline() }
    
    Triple(
        artifactsJob.await(),
        texturesJob.await(),
        uiJob.await()
    )
}