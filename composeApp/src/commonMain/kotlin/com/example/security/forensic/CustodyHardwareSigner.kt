package com.example.security.forensic

/**
 * Assinador digital multiplataforma ancorado em hardware para a Cadeia de Custódia Forense (RFI).
 *
 * Provê não-repúdio probatório local para sustentação judicial de evidências de integridade.
 * - Android: AndroidKeyStore (StrongBox Keymaster / TEE) com ECDSA P-256 / SHA-256.
 * - iOS: Keychain Services + Apple Secure Enclave (curva NIST P-256).
 * - Desktop: JVM JCA ECDSA / Ed25519 protegido localmente (DPAPI).
 * - Web (WasmJs): WebCrypto API / Provedor isolado de menor garantia.
 */
expect object CustodyHardwareSigner {
    fun isHardwareBacked(): Boolean
    fun getPublicKeyHex(): String
    fun sign(data: ByteArray): ByteArray
    fun verify(publicKeyHex: String, data: ByteArray, signature: ByteArray): Boolean
}
