package wtf.zani.llw.core.launch

import arrow.fx.coroutines.parMap
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.flow.*
import wtf.zani.llw.core.data.AssetIndex
import wtf.zani.llw.core.data.schemas.response.Artifact
import wtf.zani.llw.core.data.storage.artifactDirectory
import wtf.zani.llw.core.data.storage.assetsDirectory
import wtf.zani.llw.core.data.storage.uiDirectory
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.outputStream

data class DownloadingFile(
    val name: String,
    val path: Path,
    val hash: String
)

sealed interface ProgressReport {
    data class Chunk(
        val progress: Double,
        val downloaded: ULong,
        val remaining: ULong,
        val total: ULong,
        val file: DownloadingFile
    ) : ProgressReport
    
    data class Failed(
        val failNumber: UInt,
        val cause: FailCause,
        val file: DownloadingFile
    ) : ProgressReport

    data class Begin(
        val file: DownloadingFile
    ) : ProgressReport
    
    data class Completed(
        val file: DownloadingFile
    ) : ProgressReport
    
    data class Verified(
        val file: DownloadingFile
    ) : ProgressReport
}

sealed interface FailCause {
    data object VerifyFailed : FailCause
    data class Exception(val ex: Throwable) : FailCause
}

class Downloader(private val config: DownloadConfiguration) {
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }
    }
    
    fun downloadUi(): Flow<ProgressReport> =
        downloadFile(
            config.ui.url,
            uiDirectory.resolve(config.ui.hash),
            config.ui.hash,
            0uL,
            "ui.zip"
        )
        
    
    fun downloadUiAssets(): Flow<ProgressReport> =
        downloadIndex(
            config.uiAssets,
            uiDirectory,
            config.maxAssetConcurrency
        )
    
    fun downloadAssets(): Flow<ProgressReport> =
        downloadIndex(
            config.assets,
            assetsDirectory,
            config.maxAssetConcurrency
        )
    
    fun downloadArtifacts(): Flow<ProgressReport> = channelFlow { 
        config.artifacts
            .parMap (concurrency = config.maxArtifactConcurrency) { a ->
                downloadArtifact(a)
                    .onEach { u -> send(u) }
                    .collect()
            }
    }
    
    fun downloadArtifact(idx: Int): Flow<ProgressReport> = downloadArtifact(config.artifacts[idx])
    
    private fun downloadArtifact(artifact: Artifact): Flow<ProgressReport> = 
        downloadFile(
            artifact.url,
            artifactDirectory.resolve(artifact.sha1),
            artifact.sha1,
            artifact.size,
            artifact.name
        )
    
    private fun downloadIndex(index: AssetIndex, output: Path, maxConcurrency: Int): Flow<ProgressReport> = channelFlow {
        index.entries
            .parMap(concurrency = maxConcurrency) { e ->
                downloadFile(
                    "${index.baseUrl}${e.hash}",
                    output.resolve(e.hash),
                    e.hash,
                    e.size,
                    e.name
                ).onEach { u -> send(u) }.collect()
            }
    }
    
    private fun downloadFile(
        url: String,
        path: Path,
        hash: String,
        size: ULong,
        name: String
    ): Flow<ProgressReport> = flow {
        val file = DownloadingFile(
            name,
            path,
            hash
        )
        
        var isValid = verifyFile(path, hash)
        var fails = 0u
        
        val started = !isValid
        
        while (!isValid) {
            emit(ProgressReport.Begin(file))
            
            try {
                path.outputStream(
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE
                ).use { s ->
                    val body = client.get(url).bodyAsChannel()
                    var downloaded = 0uL

                    while (!body.isClosedForRead) {
                        body.awaitContent()

                        val packet = body.readRemaining(body.availableForRead.toLong())
                        val packetSize = packet.remaining

                        s.writePacket(packet)
                        downloaded += packetSize.toULong()

                        if (size != 0uL) {
                            emit(
                                ProgressReport.Chunk(
                                    downloaded.toDouble() / size.toDouble(),
                                    downloaded,
                                    size - downloaded,
                                    size,
                                    file
                                )
                            )
                        }
                    }
                }
            } catch (ex: Throwable) {
                fails++
                emit(ProgressReport.Failed(
                    fails,
                    FailCause.Exception(ex),
                    file
                ))
                
                continue
            }

            isValid = verifyFile(path, hash)
            
            if (!isValid) {
                fails++
                
                emit(ProgressReport.Failed(
                    fails,
                    FailCause.VerifyFailed,
                    file
                ))
            }
        }

        if (started) emit(ProgressReport.Completed(file))
        else emit(ProgressReport.Verified(file))
    }
}