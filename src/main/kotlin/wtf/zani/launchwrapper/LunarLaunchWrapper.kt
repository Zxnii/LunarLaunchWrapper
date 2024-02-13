package wtf.zani.launchwrapper

import joptsimple.OptionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import wtf.zani.launchwrapper.loader.LibraryLoader
import wtf.zani.launchwrapper.loader.LunarLoader
import wtf.zani.launchwrapper.loader.PrebakeHelper
import wtf.zani.launchwrapper.patches.AntiAntiAgent
import wtf.zani.launchwrapper.patches.ipc.IpcPatch
import wtf.zani.launchwrapper.util.toHexString
import wtf.zani.launchwrapper.version.VersionManifest
import java.io.File
import java.net.URL
import java.security.MessageDigest
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private const val nativeDirKey = "wtf.zani.launchwrapper.nativedir"

private val offlineDir = Path(System.getProperty("user.home"), ".lunarclient", "offline", "multiver")
private val textureDir = Path(System.getProperty("user.home"), ".lunarclient", "textures")

val llwDir = Path(System.getProperty("user.home"), ".llw")

suspend fun main(args: Array<String>) {
    llwDir.createDirectories()

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
    val disableUpdatesSpec =
        optionParser
            .accepts("disable-updates")

    optionParser.allowsUnrecognizedOptions()

    val options = optionParser.parse(*args)

    val gameVersion = options.valueOf(versionSpec)
    val lunarModule = options.valueOf(moduleSpec)

    val availablePatches = arrayOf(
        AntiAntiAgent::class.java,
        IpcPatch::class.java
    )

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

    if (!options.has(disableUpdatesSpec)) {
        withContext(Dispatchers.IO) {
            launch { hashes = version.download(offlineDir) }
            launch { textures.download(textureDir) }
        }

        cache?.write()
    } else {
        println("Updates are disabled")

        hashes = cache?.version?.artifacts?.map { it.sha1 }
    }

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
        "--launcherVersion", "3.2.3",
        "--classpathDir", offlineDir.toString(),
        "--workingDirectory", offlineDir.toString(),
        "--ichorClassPath", classpath.map { it.fileName }.joinToString(","),
        "--ichorExternalFiles", externalFiles.joinToString(","),
        "--webosrPath", natives.first().toString()
    )

    minecraftArgs += args.toList()

    println("Launching Lunar Client for $gameVersion with module $lunarModule. Game args: ${minecraftArgs.joinToString(", ")}")

    System.setProperty("llw.lunar.module", lunarModule)
    System.setProperty("llw.minecraft.version", gameVersion)
    System.setProperty("llw.java.classpath", classpath.map { it.fileName }.joinToString(File.separator))

    val urls = classpath.map { it.toUri().toURL() }.toTypedArray()
    val loader = LunarLoader(urls)

    val genesis = loader.loadClass("com.moonsworth.lunar.genesis.Genesis")
    val bootstrapProxy = loader.loadClass("wtf.zani.launchwrapper.loader.BootstrapProxy")

    val digest = MessageDigest.getInstance("SHA-256")

    hashes!!.forEach { digest.update(it.toByteArray()) }

    PrebakeHelper.location = offlineDir.resolve("cache/${toHexString(digest.digest(gameVersion.toByteArray()))}")
    PrebakeHelper.location.createDirectories()

    Thread.currentThread().contextClassLoader = loader

    try {
        bootstrapProxy.getMethod("setUrls", Array<URL>::class.java).invoke(null, urls)
        genesis.getMethod("main", Array<String>::class.java).invoke(null, minecraftArgs.toTypedArray())
    } catch (error: Throwable) {
        error.printStackTrace()
    }
}

fun createLibraryLoader() = LibraryLoader(arrayOf(Path(System.getProperty(nativeDirKey)!!)))
