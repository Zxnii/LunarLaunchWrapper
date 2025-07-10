package wtf.zani.llw.core.launch

import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

fun unzip(file: Path, base: Path) {
    ZipInputStream(file.inputStream(StandardOpenOption.READ)).use { s ->
        var entry = s.nextEntry

        while (entry != null) {
            if (entry.isDirectory) {
                base.resolve(entry.name).createDirectories()
                entry = s.nextEntry

                continue
            }

            val output = base.resolve(entry.name)

            output.parent.createDirectories()
            output.writeBytes(
                s.readAllBytes(),
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE
            )

            entry = s.nextEntry
        }
    }
}