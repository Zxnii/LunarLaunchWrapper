package wtf.zani.launchwrapper

import joptsimple.OptionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wtf.zani.launchwrapper.loader.LibraryLoader
import wtf.zani.launchwrapper.loader.LunarLoader
import wtf.zani.launchwrapper.loader.PrebakeHelper
import wtf.zani.launchwrapper.patches.AntiAntiAgent
import wtf.zani.launchwrapper.util.toHexString
import wtf.zani.launchwrapper.version.VersionManifest
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private const val nativeDirKey = "wtf.zani.launchwrapper.nativedir"

private val offlineDir = Path(System.getProperty("user.home"), ".lunarclient", "offline", "multiver")
private val textureDir = Path(System.getProperty("user.home"), ".lunarclient", "textures")

suspend fun main(args: Array<String>) {
    offlineDir.createDirectories()
    textureDir.createDirectories()

    val optionParser = OptionParser()

    val versionSpec =
        optionParser
            .accepts("version")
            .withRequiredArg()
            .required()
            .ofType(String::class.java)
    val moduleSpec =
        optionParser
            .accepts("module")
            .withRequiredArg()
            .required()
            .ofType(String::class.java)
    val disabledPatchesSpec =
        optionParser
            .accepts("disabled-patches")
            .withRequiredArg()
            .ofType(String::class.java)
            .withValuesSeparatedBy(",")

    optionParser.allowsUnrecognizedOptions()

    val options = optionParser.parse(*args)

    val gameVersion = options.valueOf(versionSpec)
    val lunarModule = options.valueOf(moduleSpec)

    val availablePatches = arrayOf(AntiAntiAgent::class.java)

    val disabledPatches = options.valuesOf(disabledPatchesSpec).filter { patch -> availablePatches.find { it.name == patch } != null }
    val enabledPatches = availablePatches.filter { !disabledPatches.contains(it.name) }

    println("Available patches: ${availablePatches.map { it.name }}")
    println("Enabled patches: $enabledPatches")
    println("Disabled patches: $disabledPatches")

    enabledPatches.forEach { it.getConstructor().newInstance() }

    val (version, textures, cache) = VersionManifest.fetch(gameVersion, lunarModule)
        ?: run {
            println("WHOOPS!")
            println("We failed to fetch the version manifest, this is likely because you are offline and had no cached version.")
            println("This must be run at least once while connected to the internet.")

            return
        }

    var hashes: List<String>? = null

    withContext(Dispatchers.IO) {
        launch { hashes = version.download(offlineDir) }
        launch { textures.download(textureDir) }
    }

    cache?.write()

    val natives =
        version
            .artifacts
            .filter { it.type == "NATIVES" }
            .map {
                offlineDir.resolve(it.name.replace(".zip", ""))
            }

    val classpath =
        version
            .artifacts
            .filter { it.type == "CLASS_PATH" }
            .map { offlineDir.resolve(it.name) }

    val externalFiles =
        version
            .artifacts
            .filter { it.type == "EXTERNAL_FILE" }
            .map { it.name }

    if (System.getProperty(nativeDirKey) == null) {
        System.setProperty(nativeDirKey, natives.first().toString())
    }

    System.setProperty("org.lwjgl.librarypath", System.getProperty(nativeDirKey))

    val minecraftArgs = mutableListOf(
        "--launcherVersion", "3.1.0",
        "--classpathDir", offlineDir.toString(),
        "--workingDirectory", offlineDir.toString(),
        "--ichorClassPath", classpath.map { it.fileName }.joinToString(","),
        "--ichorExternalFiles", externalFiles.joinToString(",")
    )

    minecraftArgs += args.toList()

    println("Launching Lunar Client for $gameVersion with module $lunarModule. Game args: ${minecraftArgs.joinToString(", ")}")

    System.setProperty("llw.lunar.module", lunarModule)
    System.setProperty("llw.minecraft.version", gameVersion)

    val loader = LunarLoader(classpath.map { it.toUri().toURL() }.toTypedArray())
    val genesis = loader.loadClass("com.moonsworth.lunar.genesis.Genesis")

    val digest = MessageDigest.getInstance("SHA-256")

    hashes!!.forEach { digest.update(it.toByteArray()) }

    PrebakeHelper.location = offlineDir.resolve("cache/${toHexString(digest.digest(gameVersion.toByteArray()))}")
    PrebakeHelper.location.createDirectories()

    Thread.currentThread().contextClassLoader = loader

    try {
        genesis.getMethod("main", Array<String>::class.java).invoke(null, minecraftArgs.toTypedArray())
    } catch (error: Throwable) {
        error.printStackTrace()
    }
}

fun createLibraryLoader() = LibraryLoader(arrayOf(Path(System.getProperty(nativeDirKey)!!)))
