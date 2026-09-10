package com.example.security.forensic

/**
 * Implementação Web (WasmJs) do armazenamento da Cadeia de Custódia Forense.
 */
actual object CustodyStorage {

    private val inMemoryEntries = mutableListOf<CustodyEntry>()

    actual fun loadChain(): List<CustodyEntry> {
        return inMemoryEntries.toList()
    }

    actual fun appendEntry(entry: CustodyEntry) {
        inMemoryEntries.add(entry)
    }

    actual fun clearChain() {
        inMemoryEntries.clear()
    }
}
