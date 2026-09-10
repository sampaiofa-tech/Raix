package com.example.security.forensic

/**
 * Armazenamento persistente multiplataforma para o Trilho de Auditoria e Cadeia de Custódia Forense.
 */
expect object CustodyStorage {
    fun loadChain(): List<CustodyEntry>
    fun appendEntry(entry: CustodyEntry)
    fun clearChain()
}
