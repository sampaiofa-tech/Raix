package com.example.security.identity

import kotlin.random.Random

data class HybridKemResult(
    val ephemeralX25519PubKey: ByteArray,
    val mlKemCiphertext: ByteArray,
    val combinedSharedSecret: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HybridKemResult) return false
        return ephemeralX25519PubKey.contentEquals(other.ephemeralX25519PubKey) &&
                mlKemCiphertext.contentEquals(other.mlKemCiphertext) &&
                combinedSharedSecret.contentEquals(other.combinedSharedSecret)
    }

    override fun hashCode(): Int {
        var result = ephemeralX25519PubKey.contentHashCode()
        result = 31 * result + mlKemCiphertext.contentHashCode()
        result = 31 * result + combinedSharedSecret.contentHashCode()
        return result
    }
}

/**
 * Hybrid Key Encapsulation Mechanism (KEM) Combiner
 * in accordance with NIST SP 800-227 and RFC 9180 (HPKE).
 *
 * Construction: DHKEM(X25519, HKDF-SHA256) || ML-KEM-768
 * Ensures Forward Secrecy and Resistance to Harvest-Now-Decrypt-Later threats.
 */
object HybridKem {

    private val INFO_LABEL = "raix-hybrid-hpke-v1".encodeToByteArray()

    fun encapsulate(
        recipientX25519PubKey: ByteArray,
        recipientMlKemPubKey: ByteArray,
        customEphemeralX25519Priv: ByteArray? = null
    ): HybridKemResult {
        require(recipientX25519PubKey.size == 32) { "Recipient X25519 public key must be 32 bytes" }
        require(recipientMlKemPubKey.isNotEmpty()) { "Recipient ML-KEM-768 public key cannot be empty" }

        // 1. Classical Ephemeral X25519
        val ephemPriv = customEphemeralX25519Priv ?: ByteArray(32).also { Random.nextBytes(it) }
        val clampedPriv = Curve25519Engine.clampPrivateKey(ephemPriv)
        val ephemPub = IdentityCurve25519.generatePublicKey(clampedPriv)
        val ssClassical = IdentityCurve25519.computeSharedSecret(clampedPriv, recipientX25519PubKey)

        // 2. Post-Quantum ML-KEM-768
        val pqEncapsulation = IdentityMlKem768.encapsulate(recipientMlKemPubKey)
        val ctPQ = pqEncapsulation.ciphertext
        val ssPQ = pqEncapsulation.sharedSecret

        // 3. NIST SP 800-227 Combiner:
        // salt = ephemPub || ctPQ
        // ikm = ssClassical || ssPQ
        val salt = ephemPub + ctPQ
        val ikm = ssClassical + ssPQ
        val prk = HkdfSha256.extract(salt = salt, ikm = ikm)
        val combinedKey = HkdfSha256.expand(prk = prk, info = INFO_LABEL, length = 32)

        return HybridKemResult(
            ephemeralX25519PubKey = ephemPub,
            mlKemCiphertext = ctPQ,
            combinedSharedSecret = combinedKey
        )
    }

    fun decapsulate(
        recipientX25519PrivKey: ByteArray,
        recipientMlKemPrivKey: ByteArray,
        ephemeralX25519PubKey: ByteArray,
        mlKemCiphertext: ByteArray
    ): ByteArray {
        require(recipientX25519PrivKey.size == 32) { "Recipient X25519 private key must be 32 bytes" }
        require(recipientMlKemPrivKey.isNotEmpty()) { "Recipient ML-KEM-768 private key cannot be empty" }
        require(ephemeralX25519PubKey.size == 32) { "Ephemeral X25519 public key must be 32 bytes" }
        require(mlKemCiphertext.isNotEmpty()) { "ML-KEM-768 ciphertext cannot be empty" }

        // 1. Classical Shared Secret
        val ssClassical = IdentityCurve25519.computeSharedSecret(recipientX25519PrivKey, ephemeralX25519PubKey)

        // 2. Post-Quantum Shared Secret
        val ssPQ = IdentityMlKem768.decapsulate(recipientMlKemPrivKey, mlKemCiphertext)

        // 3. Same NIST SP 800-227 Combiner
        val salt = ephemeralX25519PubKey + mlKemCiphertext
        val ikm = ssClassical + ssPQ
        val prk = HkdfSha256.extract(salt = salt, ikm = ikm)
        return HkdfSha256.expand(prk = prk, info = INFO_LABEL, length = 32)
    }
}
