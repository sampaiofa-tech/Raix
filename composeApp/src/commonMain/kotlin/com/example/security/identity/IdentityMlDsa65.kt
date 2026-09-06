package com.example.security.identity

data class MlDsaKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MlDsaKeyPair) return false
        return privateKey.contentEquals(other.privateKey) && publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int {
        return 31 * privateKey.contentHashCode() + publicKey.contentHashCode()
    }
}

/**
 * Multiplatform ML-DSA-65 (NIST FIPS 204) Digital Signature Engine.
 * Security Category: NIST Level 3.
 */
expect object IdentityMlDsa65 {
    fun generateKeyPair(seed32: ByteArray): MlDsaKeyPair
    fun sign(privateKey: ByteArray, message: ByteArray): ByteArray
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}
