package com.example.security.identity

data class MlKemKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MlKemKeyPair) return false
        return privateKey.contentEquals(other.privateKey) && publicKey.contentEquals(other.publicKey)
    }

    override fun hashCode(): Int {
        return 31 * privateKey.contentHashCode() + publicKey.contentHashCode()
    }
}

data class KemEncapsulation(
    val ciphertext: ByteArray,
    val sharedSecret: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KemEncapsulation) return false
        return ciphertext.contentEquals(other.ciphertext) && sharedSecret.contentEquals(other.sharedSecret)
    }

    override fun hashCode(): Int {
        return 31 * ciphertext.contentHashCode() + sharedSecret.contentHashCode()
    }
}

/**
 * Multiplatform ML-KEM-768 (NIST FIPS 203) Key Encapsulation Mechanism.
 * Security Category: NIST Level 3.
 */
expect object IdentityMlKem768 {
    fun generateKeyPair(seed32: ByteArray): MlKemKeyPair
    fun encapsulate(recipientPublicKey: ByteArray, customRandom: ByteArray? = null): KemEncapsulation
    fun decapsulate(recipientPrivateKey: ByteArray, ciphertext: ByteArray): ByteArray
}
