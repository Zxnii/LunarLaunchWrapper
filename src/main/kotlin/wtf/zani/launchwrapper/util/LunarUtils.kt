package wtf.zani.launchwrapper.util

import java.io.File
import java.io.FileNotFoundException
import kotlin.system.exitProcess

class LunarUtils {
    companion object {
         fun getInstallationId(): String {
             var installationId = ""
             try {
                 installationId = File( "${System.getProperty("user.home")}/.lunarclient/launcher-cache/installation-id").readLines()[0]
             } catch(_: FileNotFoundException) {
                 println("You must run lunar at least once before using this, exiting...")
                 exitProcess(1)
             }
            return installationId
        }
    }

}