package com.example.security.integrity

/**
 * Diagnostic report of client runtime integrity (P0.4 - Supply Chain & Runtime Tampering).
 */
data class RuntimeIntegrityReport(
    val isSecure: Boolean,
    val threatsDetected: List<String>,
    val details: String,
    val platformLabel: String
)

/**
 * Cross-platform runtime tamper and environment integrity auditor.
 *
 * Core Security Invariant:
 * Without client integrity verification, the assertion "total server compromise delivers only
 * ciphertext" cannot hold, because an injected or hooked client can leak the decrypted DEK
 * or key material directly from memory.
 */
expect object RuntimeIntegrityVerifier {
    fun verifyIntegrity(): RuntimeIntegrityReport
}
