package net.ddns.mindustry.database.bot

import java.io.File
import java.io.FileInputStream
import java.util.Properties

val properties = Properties()

fun loadConfiguration() {
    val osName = System.getProperty("os.name").lowercase()

    if (osName != "linux") {
        throw RuntimeException("Unsupported OS.")
    }

    val configFile = File("/etc/net/ddns/mindustry/database/bot/settings.conf")
    if (!configFile.exists()) {
        configFile.parentFile.mkdirs()
        configFile.createNewFile()
    }

    val fileStream = FileInputStream(configFile)
    properties.load(fileStream)
}
