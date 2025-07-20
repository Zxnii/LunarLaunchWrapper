package wtf.zani.llw.core.launch

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import io.github.z4kn4fein.semver.Version
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import wtf.zani.llw.core.MAXIMUM_SUPPORTED_LAUNCHER_VERSION
import wtf.zani.llw.core.SystemInfo
import wtf.zani.llw.core.data.AssetIndex
import wtf.zani.llw.core.data.schemas.request.CanaryPreference
import wtf.zani.llw.core.data.schemas.request.LaunchRequestBody
import wtf.zani.llw.core.data.schemas.request.LaunchType
import wtf.zani.llw.core.data.schemas.response.LaunchResponse
import wtf.zani.llw.core.data.storage.*
import wtf.zani.llw.core.data.storage.indexes
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}
private val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(json)
    }
    
    engine {
        config {
            followRedirects(true)
        }
    }
}

suspend fun fetchLatestManifest(
    version: String,
    branch: String,
    module: String = "lunar",
    canaryPreference: CanaryPreference = CanaryPreference.Neutral,
    launcherVersion: Version = MAXIMUM_SUPPORTED_LAUNCHER_VERSION,
    installId: UUID = installId()
): Manifest = coroutineScope {
    val system = SystemInfo.get().getOrNull()!!
    val launchResponse = client.post("https://api.lunarclientprod.com/launcher/launch") {
        contentType(ContentType.Application.Json)
        setBody(LaunchRequestBody(
            os = system.os,
            osRelease = system.release,
            arch = system.architecture,
            installationId = installId.toString(),
            launcherVersion = launcherVersion.toString(),
            launchType = LaunchType.Offline,
            branch = branch,
            version = version,
            module = module,
            canaryPreference = canaryPreference,
        ))
    }.body<LaunchResponse>()
    
    val assetIndexJob = async {
        when (val result = openIndex(launchResponse.textures.indexSha1)) {
            is Some -> result.value
            is None -> {
                client.get(launchResponse.textures.indexUrl).bodyAsText().let { c ->
                    saveIndex(AssetIndex.parse(
                        launchResponse.textures.baseUrl,
                        launchResponse.textures.indexSha1,
                        c
                    ))
                }
            }
        }
    }
    
    val uiAssetIndexJob = 
        launchResponse.ui?.let { u ->
            async {
                when (val result = openIndex(u.assets.indexSha1)) {
                    is Some -> result.value
                    is None -> {
                        client.get(u.assets.indexUrl).bodyAsText().let { c ->
                            saveIndex(
                                AssetIndex.parse(
                                    u.assets.baseUrl,
                                    u.assets.indexSha1,
                                    c
                                )
                            )
                        }
                    }
                }
            }
        }
    
    val assetIndex = assetIndexJob.await()
    val uiAssetIndex = uiAssetIndexJob?.await()
    
    Manifest(
        module,
        version,
        branch,
        launchResponse.launchTypeData,
        launchResponse.ui?.let { u ->
            Ui(
                u.sourceUrl,
                u.sourceSha1
            )   
        },
        Clock.System.now(),
        ManifestStoredIndexes(
            uiAssetIndex?.hash,
            assetIndex.hash
        ),
        Some(ManifestIndexes(
            uiAssetIndex,
            assetIndex
        ))
    )
}

private suspend fun openIndex(hash: String): Option<AssetIndex> = coroutineScope { 
    async(Dispatchers.IO) {
        val storageLocation = indexes.resolve(hash)
        
        if (storageLocation.exists())
            try {
                Some(
                    storageLocation
                        .readText()
                        .let { c -> json.decodeFromString<AssetIndex>(c) }
                )
            } catch (_: Exception) { None }
        else None
    }.await()
}

private suspend fun saveIndex(index: AssetIndex): AssetIndex = coroutineScope { 
    async(Dispatchers.IO) {
        indexes
            .resolve(index.hash)
            .writeText(json.encodeToString(index))
    }.await()

    index
}