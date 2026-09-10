package com.example.security.forensic

import java.io.File

/**
 * Implementação Desktop (JVM) do armazenamento da Cadeia de Custódia Forense.
 */
actual object CustodyStorage {

    private val storageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "custody_trail.log")
    }

    private val inMemoryEntries = mutableListOf<CustodyEntry>()
    private var isLoaded = false

    actual fun loadChain(): List<CustodyEntry> = synchronized(this) {
        if (!isLoaded) {
            inMemoryEntries.clear()
            if (storageFile.exists()) {
                try {
                    storageFile.forEachLine { line ->
                        if (line.isNotBlank()) {
                            CustodyEntry.fromLogLine(line.trim())?.let { inMemoryEntries.add(it) }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback com registros em memória
                }
            }
            isLoaded = true
        }
        return inMemoryEntries.toList()
    }

    actual fun appendEntry(entry: CustodyEntry) = synchronized(this) {
        if (!isLoaded) loadChain()
        inMemoryEntries.add(entry)
        try {
            storageFile.appendText(entry.toLogLine() + "\n")
        } catch (_: Exception) {
            // Em caso de I/O bloqueado, permanece garantido em memória volátil
        }
    }

    actual fun clearChain() = synchronized(this) {
        inMemoryEntries.clear()
        isLoaded = true
        if (storageFile.exists()) {
            try {
                storageFile.delete()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}
