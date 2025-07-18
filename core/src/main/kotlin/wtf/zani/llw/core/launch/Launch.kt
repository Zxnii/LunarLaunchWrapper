package wtf.zani.llw.core.launch

import arrow.core.Either
import arrow.core.raise.either
import arrow.fx.coroutines.parMap
import arrow.fx.coroutines.parMapNotNull
import wtf.zani.llw.core.checkVersion
import wtf.zani.llw.core.data.AssetIndex
import wtf.zani.llw.core.data.schemas.response.ArtifactType
import wtf.zani.llw.core.data.storage.Manifest
import wtf.zani.llw.core.data.storage.artifactDirectory
import wtf.zani.llw.core.data.storage.assetsDirectory
import wtf.zani.llw.core.data.storage.uiDirectory
import wtf.zani.llw.core.internal.LaunchClassLoader
import wtf.zani.llw.core.internal.prebakeLocation
import wtf.zani.llw.core.launch.errors.LaunchError
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*

suspend fun prepareTextures(config: LaunchConfiguration, manifest: Manifest) = prepareAssets(
    config.textureDirectory,
    assetsDirectory,
    manifest.assets.textures
)

suspend fun prepareUi(
    config: LaunchConfiguration,
    manifest: Manifest
) {
    unzip(uiDirectory.resolve(manifest.uiData.hash), config.uiDirectory)
    prepareAssets(
        config.uiDirectory.resolve("assets"),
        uiDirectory,
        manifest.assets.uiAssets
    )
}

@OptIn(ExperimentalPathApi::class)
suspend fun prepareArtifactConfiguration(
    config: LaunchConfiguration,
    manifest: Manifest
): ArtifactConfiguration {
    config.multiverDirectory.createDirectories()
    
    val nativesCopyPath = config.multiverDirectory.resolve("natives")
    val artifacts = manifest.launchData.artifacts.parMapNotNull { a ->
        val downloadPath = artifactDirectory.resolve(a.sha1)
        
        when (a.type) {
            ArtifactType.Classpath, ArtifactType.External -> {
                val copyPath = config.multiverDirectory.resolve(a.name)

                if (downloadPath.exists()) {
                    copyPath.parent.createDirectories()
                    downloadPath.copyTo(copyPath, true)
                    Pair(a.type, copyPath)
                } else {
                    println("WARN: Artifact ${a.name} was not found, glhf..?")
                    null
                }
            }
            ArtifactType.Natives -> {
                unpackNatives(downloadPath)
                
                null
            }
        }
    }

    if (nativesCopyPath.exists()) nativesCopyPath.deleteRecursively()
    
    nativesDirectory.copyToRecursively(
        nativesCopyPath,
        { _, _, _ -> OnErrorResult.TERMINATE },
        followLinks = false,
        overwrite = true
    )
    
    return ArtifactConfiguration(
        manifest.launchData.mainClass,
        artifacts.filter { (t) -> t == ArtifactType.Classpath }.map { (_, p) -> p }.toSet(),
        artifacts.filter { (t) -> t == ArtifactType.External }.map { (_, p) -> p }.toSet(),
        setOf(nativesCopyPath)
    )
}

@OptIn(ExperimentalStdlibApi::class)
fun launch(
    config: LaunchConfiguration,
    artifacts: ArtifactConfiguration,
): Either<LaunchError, Unit> = either {
    checkVersion(config.launcherVersion)
        .onLeft { e ->
            raise(LaunchError.UnsupportedVersion(e))
        }
    
    val args = buildArgs(config, artifacts)
    val loader = LaunchClassLoader(artifacts.classPath)
    
    val mainClass =
        try { loader.loadClass(artifacts.mainClass) }
        catch (ex: ClassNotFoundException) { raise(LaunchError.MissingMainClass(artifacts.mainClass)) }
    val mainMethod = 
        try { mainClass.getDeclaredMethod("main", Array<String>::class.java) }
        catch(ex: NoSuchMethodException) { raise(LaunchError.MissingMainMethod) }
    val bootstrapProxy = loader.loadClass("wtf.zani.llw.core.internal.BootstrapProxy")
    val addUrl = bootstrapProxy.getMethod("addUrl", URL::class.java)

    val digest = MessageDigest.getInstance("sha-1")
    
    artifacts.classPath.forEach { a ->
        a.inputStream().use { s ->
            var read = s.readNBytes(1024)

            digest.update(read)
            
            while (read.size == 1024) {
                digest.update(read)
                read = s.readNBytes(1024)
            }
        }
    }
    prebakeLocation =
        config
            .multiverDirectory
            .resolve("cache")
            .resolve(digest.digest().toHexString())
            .createDirectories()
    
    System.setProperty("llw.internal.natives", artifacts.natives.joinToString(File.pathSeparator))
    System.setProperty("llw.internal.classpath", artifacts.classPath.joinToString(File.pathSeparator))
    
    System.setProperty("org.lwjgl.librarypath", System.getProperty("llw.internal.natives"))
    System.setProperty("jna.boot.library.path", System.getProperty("llw.internal.natives"))
    
    Thread.currentThread().contextClassLoader = loader
    
    artifacts.classPath.forEach { p ->
        addUrl(null, p.toUri().toURL())
    }
    
    try { mainMethod.invoke(null, args.toTypedArray()) }
    catch(ex: InvocationTargetException) { raise(LaunchError.LunarException(ex.cause ?: ex)) }
}

private fun buildArgs(
    config: LaunchConfiguration,
    artifacts: ArtifactConfiguration
): List<String> = listOf(
    "--launcherVersion", config.launcherVersion.toString(),
    "--classpathDir", config.multiverDirectory.toString(),
    "--workingDirectory", config.multiverDirectory.toString(),
    "--ichorClassPath",
        artifacts
            .classPath
            .map { p -> config.multiverDirectory.relativize(p) }
            .joinToString(","),
    "--ichorExternalFiles",
        artifacts
            .externalFiles
            .map { p -> config.multiverDirectory.relativize(p) }
            .joinToString(","),
    "--webosrDir",
        artifacts
            .natives
            .first()
            .toString(),
    "--uiDir", config.uiDirectory.toString(),
    "--installationId", config.installId,
    "--version", config.minecraftVersion.toString(),
    *config.additionalArgs.toTypedArray()
)

private suspend fun prepareAssets(output: Path, base: Path, index: AssetIndex) =
    index.entries.parMap { e ->
        val installTo =
            output
                .resolve(e.name)
                .also { p -> p.parent.createDirectories() }
        val input = base.resolve(e.hash)
        
        while (!verifyFile(installTo, e.hash)) input.copyTo(installTo, true)
    }