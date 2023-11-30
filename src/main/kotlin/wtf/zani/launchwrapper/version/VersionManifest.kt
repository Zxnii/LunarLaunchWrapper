package wtf.zani.launchwrapper.version

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wtf.zani.launchwrapper.util.SystemInfo
import wtf.zani.launchwrapper.util.toHexString
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipFile
import kotlin.io.path.*

private val httpClient = HttpClient()

@Serializable
private data class ManifestResponse(
    val success: Boolean,
    val launchTypeData: VersionManifest? = null,
    val textures: TextureManifest? = null,
    val ui: String? = null
)

@Serializable
data class Cache(
    @Transient private val file: File = File("cache.json"),
    val hash: String,
    val version: VersionManifest,
    val textures: TextureManifest
) {
    fun write() {
        file.writeText(Json.encodeToString(this))
    }
}

@Serializable
data class LauncherInfo(
    val os: String,
    @SerialName("os_release") val osRelease: String = "0",
    @SerialName("installation_id") val installationId: String = UUID.randomUUID().toString(),
    val arch: String,
    val module: String,
    @SerialName("launch_type") val launchType: String = "OFFLINE",
    val version: String,
    val branch: String = "master",
    @SerialName("launcher_version") val launcherVersion: String = "3.1.0",
    val hwid: String = ""
) {
    @Transient
    val cacheName = "${arch}_${os}_${module}${version.replace(".", "_")}.json"
}

@Serializable
data class Artifact(
    val name: String, val sha1: String, val type: String, val url: String
)

@Serializable
data class TextureManifest(
    val indexUrl: String, val indexSha1: String, val baseUrl: String
) {
    @Transient
    var upToDate = false

    suspend fun download(directory: Path) {
        if (upToDate) return

        println("Downloading Lunar's assets")

        httpClient.get(indexUrl)
            .bodyAsText()
            .lines()
            .map { it.split(" ") }
            .filter { it.size == 4 }
            .chunked(Runtime.getRuntime().availableProcessors() * 4)
            .forEach { chunk ->
                withContext(Dispatchers.IO) {
                    chunk.forEach chunk@{ texture ->
                        val filePath = directory.resolve(texture[0])

                        filePath.parent.createDirectories()

                        if (filePath.exists() && filePath.isRegularFile()) {
                            val digest = MessageDigest.getInstance("SHA-1")

                            if (texture[1] == toHexString(digest.digest(filePath.readBytes()))) return@chunk
                        }

                        println("Downloading ${texture[0]}")

                        launch {
                            filePath.outputStream().use { out ->
                                httpClient.get("$baseUrl${texture[1]}")
                                    .body<InputStream>()
                                    .use {
                                        it.transferTo(out)
                                    }
                            }
                        }
                    }
                }
        }
    }
}

@Serializable
data class VersionManifest(
    val artifacts: List<Artifact>,
) {
    @Transient
    var upToDate = false

    suspend fun download(directory: Path) {
        if (upToDate) return

        println("Downloading Lunar")

        artifacts.forEach { artifact ->
            val filePath = directory.resolve(artifact.name)

            if (filePath.exists() && filePath.isRegularFile()) {
                val digest = MessageDigest.getInstance("SHA-1")

                if (artifact.sha1 == toHexString(digest.digest(filePath.readBytes()))) return@forEach
            }

            println("Downloading ${artifact.name}")

            filePath.outputStream().use { out ->
                httpClient.get(artifact.url).body<InputStream>().use {
                    it.transferTo(out)
                }
            }

            if (artifact.type == "NATIVES") {
                val nativesDirectory = directory.resolve(artifact.name.replace(".zip", "")).createDirectories()

                ZipFile(filePath.toFile()).use { zip ->
                    zip.entries().asIterator().forEach { entry ->
                        if (!entry.isDirectory) {
                            val file = nativesDirectory.resolve(entry.name)

                            file.parent.createDirectories()
                            file.outputStream().use { output ->
                                zip.getInputStream(entry).use { input ->
                                    input.transferTo(output)
                                }
                            }
                        }
                    }
                }
            }
        }


    }

    companion object {
        suspend fun fetch(version: String, module: String): Triple<VersionManifest, TextureManifest, Cache?>? {
            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

            val systemInfo = SystemInfo.get()

            val launcherInfo = LauncherInfo(
                os = systemInfo.os.internalName,
                arch = systemInfo.arch,
                osRelease = systemInfo.os.version(),
                version = version,
                module = module
            )

            val cacheFile = File(launcherInfo.cacheName)

            return try {
                val rawResponse = httpClient.post("https://api.lunarclientprod.com/launcher/launch") {
                    header("Content-Type", "application/json")
                    setBody(
                        json.encodeToString(launcherInfo)
                    )
                }.bodyAsText()

                val response = json.decodeFromString<ManifestResponse>(rawResponse)

                if (!response.success) {
                    throw Exception("Failed to fetch manifest. Response: $rawResponse")
                }

                val responseHash = toHexString(MessageDigest.getInstance("SHA-1").digest(rawResponse.toByteArray()))

                if (cacheFile.exists()) {
                    try {
                        val cache = json.decodeFromString<Cache>(cacheFile.readText())

                        if (cache.hash == responseHash) {
                            response.launchTypeData!!.upToDate = true
                            response.textures!!.upToDate = true

                            println("Lunar is up to date")
                        }
                    } catch (_: Throwable) {}
                }

                Triple(response.launchTypeData!!, response.textures!!, Cache(cacheFile, responseHash, response.launchTypeData, response.textures))
            } catch (ex: Throwable) {
                ex.printStackTrace()

                println("Failed to fetch the latest version manifest for $module on $version (are you offline?). Trying the cache.")

                if (cacheFile.exists()) {
                    return try {
                        val cache = json.decodeFromString<Cache>(cacheFile.readText())

                        cache.version.upToDate = true
                        cache.textures.upToDate = true

                        Triple(cache.version, cache.textures, null)
                    } catch (ex: Throwable) {
                        ex.printStackTrace()

                        null
                    }
                } else {
                    println("Failed to find the version manifest for $module on $version in the cache.")
                }

                null
            }
        }
    }
}
