package wtf.zani.llw.core.data.storage

import wtf.zani.llw.core.llwRoot
import kotlin.io.path.createDirectories

internal val downloadCache =
    llwRoot
        .resolve("downloads")
        .createDirectories()
internal val indexes =
    llwRoot
        .resolve("indexes")
        .createDirectories()

internal val artifactDirectory = downloadCache.resolve("artifacts").createDirectories()
internal val assetsDirectory = downloadCache.resolve("assets").createDirectories()
internal val uiDirectory = downloadCache.resolve("ui").createDirectories()