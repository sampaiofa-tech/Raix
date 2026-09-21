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
        val localAppData = System.getenv("LOCALAPPDATA") ?: ""
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
        
        val shouldWipe = if (lastVersion != null) {
            isNewerVersion(currentVersion, lastVersion)
        } else {
            dir.listFiles()?.any { it.name != "version_info.json" } == true
        }

        if (shouldWipe) {
            println("[WIPE] Atualização detectada (last=$lastVersion -> curr=$currentVersion). Executando Wipe Master (Desktop)...")
            
            // Camada 1: Java delete — remove tudo que não está travado pelo próprio processo
            try {
                dir.listFiles()?.forEach { file ->
                    if (file.name != "version_info.json") {
                        val deleted = file.deleteRecursively()
                        println("[WIPE] Java delete: ${file.name} -> ${if (deleted) "OK" else "FALHA (lock?)"}")
                    }
                }
                if (localAppData.isNotEmpty()) {
                    val raixLocal = File(localAppData, "Raix")
                    if (raixLocal.exists()) {
                        val deleted = raixLocal.deleteRecursively()
                        println("[WIPE] Java delete: %LOCALAPPDATA%\\Raix -> ${if (deleted) "OK" else "FALHA (lock?)"}")
                    }
                }
            } catch (e: Exception) {
                println("[WIPE] Java delete parcial: ${e.message}")
            }
            
            // Camada 2: PowerShell — força remoção de ficheiros travados (WebView, etc.)
            try {
                val psCommand = """
                    Get-ChildItem -Path "${'$'}env:APPDATA\Pmsg" -Recurse -Force -ErrorAction SilentlyContinue | Where-Object { ${'$'}_.Name -ne 'version_info.json' } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue;
                    Remove-Item -Recurse -Force "${'$'}env:LOCALAPPDATA\Raix" -ErrorAction SilentlyContinue;
                    Remove-Item -Force "${'$'}env:APPDATA\Microsoft\Windows\Start Menu\Programs\Raix.lnk" -ErrorAction SilentlyContinue;
                    Remove-Item -Recurse -Force "HKCU:\Software\Classes\raix" -ErrorAction SilentlyContinue
                """.trimIndent()

                val process = ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-Command", psCommand)
                    .redirectErrorStream(true)
                    .start()

                process.waitFor()
                println("[WIPE] PowerShell cleanup executado.")
            } catch (e: Exception) {
                println("[WIPE] Falha no PowerShell cleanup: ${e.message}")
            }
            
            // Verificar resultado
            val remaining = dir.listFiles()?.filter { it.name != "version_info.json" }?.map { it.name } ?: emptyList()
            if (remaining.isEmpty()) {
                println("[WIPE] Wipe completo — pasta limpa.")
            } else {
                println("[WIPE] Ficheiros restantes (possivelmente travados): $remaining")
            }
        } else {
            println("[WIPE] Nenhum wipe necessário. last=$lastVersion, curr=$currentVersion")
        }
        
        try {
            // Re-create the dir since it may have been wiped
            if (!dir.exists()) dir.mkdirs()
            val newInfo = json.encodeToString(VersionInfo.serializer(), VersionInfo(currentVersion))
            versionFile.writeText(newInfo)
            println("[WIPE] version_info.json gravado: $currentVersion")
        } catch (_: Exception) {}
    }
    
    private fun isNewerVersion(current: String, last: String): Boolean {
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
}
