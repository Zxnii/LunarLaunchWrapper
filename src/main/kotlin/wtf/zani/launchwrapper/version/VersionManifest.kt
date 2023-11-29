package wtf.zani.launchwrapper.version

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wtf.zani.launchwrapper.util.LunarUtils
import wtf.zani.launchwrapper.util.SystemInfo
import wtf.zani.launchwrapper.util.toHexString
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.io.path.*

@Serializable
private data class ManifestResponse(
    val success: Boolean,
    val launchTypeData: VersionManifest? = null
)

@Serializable
data class LauncherInfo(
    @SerialName("installation_id")
    val installationId: String,
    val os: String,
    @SerialName("os_release")
    val osRelease: String = "0",
    val arch: String,
    val module: String,
    val version: String,
    val branch: String = "master",
    @SerialName("launcher_version")
    val launcherVersion: String = "3.1.3",
    val hwid: String = ""
) {
    @Transient
    val cacheName = "${arch}_${os}_${module}${version.replace(".", "_")}.json"
}

@Serializable
data class Artifact(
    val name: String,
    val sha1: String,
    val type: String,
    val url: String
)

@Serializable
data class VersionManifest(
    val artifacts: List<Artifact>
) {

    suspend fun download(directory: Path) {
        artifacts
            .forEach { artifact ->
                val filePath = directory.resolve(artifact.name)

                if (filePath.exists() && filePath.isRegularFile()) {
                    val digest = MessageDigest.getInstance("SHA-1")

                    if (artifact.sha1 == toHexString(digest.digest(filePath.readBytes()))) return@forEach
                }

                println("Downloading ${artifact.name}")

                filePath
                    .outputStream()
                    .use { out ->
                        httpClient
                            .get(artifact.url)
                            .body<InputStream>()
                            .use {
                                it.transferTo(out)
                            }
                    }

                if (artifact.type == "NATIVES") {
                    val nativesDirectory =
                        directory
                            .resolve(artifact.name.replace(".zip", ""))
                            .createDirectories()

                    ZipFile(filePath.toFile())
                        .use { zip ->
                            zip
                                .entries()
                                .asIterator()
                                .forEach { entry ->
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
        private val httpClient = HttpClient()

        suspend fun fetch(version: String, module: String): VersionManifest? {
            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

            val systemInfo = SystemInfo.get()

            val launcherInfo = LauncherInfo(
                installationId = LunarUtils.getInstallationId(),
                os = systemInfo.os.internalName,
                arch = systemInfo.arch,
                osRelease = systemInfo.os.version(),
                version = version,
                module = module
            )

            val cacheFile = File(launcherInfo.cacheName)
            println(json.encodeToString(launcherInfo))
            return try {
                val rawResponse = httpClient.post("https://api.lunarclientprod.com/launcher/launch") {
                    header("Content-Type", "application/json")
                    setBody(
                        json.encodeToString(launcherInfo)
                    )
                }.bodyAsText()
                println(rawResponse)

                val response = json.decodeFromString<ManifestResponse>(rawResponse)

                if (!response.success) {
                    throw Exception("Failed to fetch manifest : ")
                }

                cacheFile.writeText(json.encodeToString(response.launchTypeData!!))

                response.launchTypeData
            } catch (ex: Throwable) {
                ex.printStackTrace()

                println("Failed to fetch the latest version manifest for $module on $version (are you offline?). Trying the cache.")

                if (cacheFile.exists()) {
                    return try {
                        json.decodeFromString<VersionManifest>(cacheFile.readText())
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
