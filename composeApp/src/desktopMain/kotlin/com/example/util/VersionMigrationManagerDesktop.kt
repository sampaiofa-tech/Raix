package com.example.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class VersionInfo(val lastVersion: String)

object VersionMigrationManagerDesktop {
    private val json = Json { ignoreUnknownKeys = true }
    
    fun checkAndWipeOnUpdate(currentVersion: String) {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg")
        if (!dir.exists()) dir.mkdirs()
        
        val versionFile = File(dir, "version_info.json")
        var lastVersion: String? = null
        
        if (versionFile.exists()) {
            try {
                val info = json.decodeFromString(VersionInfo.serializer(), versionFile.readText())
                lastVersion = info.lastVersion
            } catch (_: Exception) {}
        }
        
        if (lastVersion != null && isNewerVersion(currentVersion, lastVersion)) {
            println("Atualização detectada de $lastVersion para $currentVersion. Executando Wipe Master (Desktop)...")
            wipeDirectory(dir, exclude = versionFile.name)
        }
        
        try {
            val newInfo = json.encodeToString(VersionInfo.serializer(), VersionInfo(currentVersion))
            versionFile.writeText(newInfo)
        } catch (_: Exception) {}
    }
    
    private fun isNewerVersion(current: String, last: String): Boolean {
        // Simple semver check
        val currParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val lastParts = last.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(currParts.size, lastParts.size)
        for (i in 0 until length) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lastParts.getOrElse(i) { 0 }
            if (c > l) return true
            if (c < l) return false
        }
        return false
    }
    
    private fun wipeDirectory(dir: File, exclude: String) {
        if (!dir.exists() || !dir.isDirectory) return
        dir.listFiles()?.forEach { file ->
            if (file.name != exclude) {
                file.deleteRecursively()
            }
        }
    }
}
