package wtf.zani.llw.core.launch

import wtf.zani.llw.core.llwRoot
import java.nio.file.Path
import kotlin.io.path.createDirectories

internal val nativesDirectory = llwRoot.resolve("natives").createDirectories()

fun unpackNatives(artifact: Path) = unzip(artifact, nativesDirectory)