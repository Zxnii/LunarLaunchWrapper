package wtf.zani.launchwrapper.util

fun toHexString(buffer: ByteArray): String = buffer.joinToString("") { String.format("%02x", it) }
