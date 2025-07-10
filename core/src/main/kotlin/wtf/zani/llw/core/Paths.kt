package wtf.zani.llw.core

import kotlin.io.path.Path
import kotlin.io.path.createDirectories

val lunarRoot =
    Path(System.getProperty("user.home"))
        .resolve(".lunarclient")
        .createDirectories()
internal val llwRoot =
    Path(System.getProperty("user.home"))
        .resolve(".llw")
        .createDirectories()