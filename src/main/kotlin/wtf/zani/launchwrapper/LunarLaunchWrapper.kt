package wtf.zani.launchwrapper

import joptsimple.OptionParser
import wtf.zani.launchwrapper.loader.LibraryLoader
import wtf.zani.launchwrapper.loader.LunarLoader
import wtf.zani.launchwrapper.version.VersionManifest
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

private const val nativeDirKey = "wtf.zani.launchwrapper.nativedir"
private val offlineDir = Path(System.getProperty("user.home"), ".lunarclient", "offline", "multiver")

suspend fun main(args: Array<String>) {
    offlineDir.createDirectories()

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

    optionParser.allowsUnrecognizedOptions()

    val options = optionParser.parse(*args)

    val gameVersion = options.valueOf(versionSpec)
    val lunarModule = options.valueOf(moduleSpec)

    val manifest = VersionManifest.fetch(gameVersion, lunarModule)
        ?: run {
            println("WHOOPS!")
            println("We failed to fetch the version manifest, this is likely because you are offline and had no cached version.")
            println("This must be run at least once while connected to the internet.")

            return
        }

    manifest.download(offlineDir)

    val natives =
        manifest
            .artifacts
            .filter { it.type == "NATIVES" }
            .map {
                offlineDir.resolve(it.name.replace(".zip", ""))
            }

    val classpath =
        manifest
            .artifacts
            .filter { it.type == "CLASS_PATH" }
            .map { offlineDir.resolve(it.name) }

    val externalFiles =
        manifest
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
        "--ichorExternalFiles", externalFiles.joinToString(",")
    )

    minecraftArgs += args.toList()

    println("Launching Lunar Client for $gameVersion with module $lunarModule. Game args: ${minecraftArgs.joinToString(", ")}")

    val loader = LunarLoader(classpath.map { it.toUri().toURL() }.toTypedArray())
    val genesis = loader.loadClass("com.moonsworth.lunar.genesis.Genesis")

    Thread.currentThread().contextClassLoader = loader

    try {
        genesis.getMethod("main", Array<String>::class.java).invoke(null, minecraftArgs.toTypedArray())
    } catch (error: Throwable) {
        error.printStackTrace()
    }
}

fun createLibraryLoader() = LibraryLoader(arrayOf(Path(System.getProperty(nativeDirKey)!!)))
