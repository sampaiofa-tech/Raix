package com.example.security.forensic

import com.example.util.AndroidContextHolder
import java.io.File

/**
 * Implementação Android do armazenamento da Cadeia de Custódia Forense.
 *
 * Persiste evidências criptograficamente assinadas no diretório privado isolado do app.
 */
actual object CustodyStorage {

    private const val FILE_NAME = "custody_trail.log"

    private val inMemoryEntries = mutableListOf<CustodyEntry>()
    private var isLoaded = false

    private fun getStorageFile(): File? {
        val ctx = AndroidContextHolder.appContext ?: return null
        return File(ctx.filesDir, FILE_NAME)
    }

    actual fun loadChain(): List<CustodyEntry> = synchronized(this) {
        if (!isLoaded) {
            inMemoryEntries.clear()
            val file = getStorageFile()
            if (file != null && file.exists()) {
                try {
                    file.forEachLine { line ->
                        if (line.isNotBlank()) {
                            CustodyEntry.fromLogLine(line.trim())?.let { inMemoryEntries.add(it) }
                        }
                    }
                } catch (_: Exception) {
                    // Fallback para memória volátil
                }
            }
            isLoaded = true
        }
        return inMemoryEntries.toList()
    }

    actual fun appendEntry(entry: CustodyEntry) = synchronized(this) {
        if (!isLoaded) loadChain()
        inMemoryEntries.add(entry)
        val file = getStorageFile()
        if (file != null) {
            try {
                file.appendText(entry.toLogLine() + "\n")
            } catch (_: Exception) {
                // Preservado em memória
            }
        }
    }

    actual fun clearChain() = synchronized(this) {
        inMemoryEntries.clear()
        isLoaded = true
        val file = getStorageFile()
        if (file != null && file.exists()) {
            try {
                file.delete()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }
}
