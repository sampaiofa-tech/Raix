package com.example.security.identity

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * Encapsulated envelope containing the opaque cryptographic payload for the recipient.
 * In accordance with NIST SP 800-227 and RFC 9180 (HPKE).
 *
 * Supports both:
 * 1. Hybrid PQC: DHKEM(X25519, HKDF-SHA256) || ML-KEM-768 ("hybrid-v1")
 * 2. Classical fallback: X25519 ("classical-v1")
 */
@Serializable
data class SealedBoxEnvelope(
    val ephemeralPubKeyHex: String,
    val wrappedDekBase64: String,
    val mlKemCiphertextBase64: String? = null,
    val securitySuite: String = SUITE_HYBRID
) {
    companion object {
        const val SUITE_HYBRID = "hybrid-v1"
        const val SUITE_CLASSICAL = "classical-v1"
    }
}

class DowngradeAttackException(message: String) : Exception(message)

/**
 * Multiplatform Sealed-Box primitive for E2E DEK encryption.
 * Implements Hybrid Post-Quantum Key Encapsulation (X25519 + ML-KEM-768)
 * and strict Anti-Downgrade protection (Guru Amendment A.2 & A.3).
 */
object SealedBox {

    private const val INFO_LABEL = "pmsg-dek-wrap-v1"
    const val NONCE_SIZE = 12 // 96 bits

    /**
     * Seals a Data Encryption Key (DEK) using Hybrid PQC: X25519 + ML-KEM-768.
     * Preserves Forward Secrecy by using fresh ephemeral keys for both algorithms.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun sealHybrid(
        dek: ByteArray,
        recipientX25519PubKey: ByteArray,
        recipientMlKemPubKey: ByteArray,
        customNonce: ByteArray? = null
    ): SealedBoxEnvelope {
        require(recipientX25519PubKey.size == 32) { "Recipient X25519 public key must be 32 bytes" }
        require(recipientMlKemPubKey.isNotEmpty()) { "Recipient ML-KEM-768 public key cannot be empty" }

        // 1. Execute Hybrid KEM (NIST SP 800-227 combiner)
        val kemResult = HybridKem.encapsulate(recipientX25519PubKey, recipientMlKemPubKey)

        // 2. Encrypt DEK with derived KEK via AES-256-GCM
        val nonce = customNonce ?: ByteArray(NONCE_SIZE).also { Random.nextBytes(it) }
        require(nonce.size == NONCE_SIZE) { "Nonce must be 12 bytes" }
        val encryptedPayload = AesGcm.encrypt(plaintext = dek, key = kemResult.combinedSharedSecret, iv = nonce)

        // 3. Pack wrappedDek = nonce (12 bytes) + encryptedPayload (ciphertext + 16-byte tag)
        val wrappedBytes = ByteArray(NONCE_SIZE + encryptedPayload.size)
        nonce.copyInto(wrappedBytes, 0, 0, NONCE_SIZE)
        encryptedPayload.copyInto(wrappedBytes, NONCE_SIZE, 0, encryptedPayload.size)

        return SealedBoxEnvelope(
            ephemeralPubKeyHex = bytesToHex(kemResult.ephemeralX25519PubKey),
            wrappedDekBase64 = Base64.encode(wrappedBytes),
            mlKemCiphertextBase64 = Base64.encode(kemResult.mlKemCiphertext),
            securitySuite = SealedBoxEnvelope.SUITE_HYBRID
        )
    }

    /**
     * Unseals a Data Encryption Key (DEK) from a Hybrid PQC envelope.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun unsealHybrid(
        envelope: SealedBoxEnvelope,
        recipientX25519PrivKey: ByteArray,
        recipientMlKemPrivKey: ByteArray
    ): ByteArray {
        require(recipientX25519PrivKey.size == 32) { "Recipient X25519 private key must be 32 bytes" }
        require(recipientMlKemPrivKey.isNotEmpty()) { "Recipient ML-KEM-768 private key cannot be empty" }
        val mlKemCtBase64 = envelope.mlKemCiphertextBase64
            ?: throw DowngradeAttackException("Tentativa de downgrade detectada: envelope não contém ciphertext ML-KEM-768.")

        val ephemeralPub = hexToBytes(envelope.ephemeralPubKeyHex)
        val mlKemCiphertext = Base64.decode(mlKemCtBase64)

        // 1. Recover combined KEK using Hybrid KEM combiner
        val kek = HybridKem.decapsulate(
            recipientX25519PrivKey = recipientX25519PrivKey,
            recipientMlKemPrivKey = recipientMlKemPrivKey,
            ephemeralX25519PubKey = ephemeralPub,
            mlKemCiphertext = mlKemCiphertext
        )

        // 2. Unpack wrapped DEK
        val wrappedBytes = Base64.decode(envelope.wrappedDekBase64)
        require(wrappedBytes.size >= NONCE_SIZE + 16) { "Wrapped DEK payload is too short" }

        val nonce = ByteArray(NONCE_SIZE)
        wrappedBytes.copyInto(nonce, 0, 0, NONCE_SIZE)

        val cipherWithTag = ByteArray(wrappedBytes.size - NONCE_SIZE)
        wrappedBytes.copyInto(cipherWithTag, 0, NONCE_SIZE, wrappedBytes.size)

        // 3. Decrypt DEK with AES-256-GCM
        return AesGcm.decrypt(ciphertext = cipherWithTag, key = kek, iv = nonce)
    }

    /**
     * Unified seal entry point with automatic mode selection:
     * Prefers Hybrid PQC if recipientMlKemPubKey is provided; falls back to classical X25519 if absent.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun seal(
        dek: ByteArray,
        recipientPubKey: ByteArray,
        recipientMlKemPubKey: ByteArray? = null,
        customEphemeralPriv: ByteArray? = null,
        customNonce: ByteArray? = null
    ): SealedBoxEnvelope {
        if (recipientMlKemPubKey != null && recipientMlKemPubKey.isNotEmpty()) {
            return sealHybrid(
                dek = dek,
                recipientX25519PubKey = recipientPubKey,
                recipientMlKemPubKey = recipientMlKemPubKey,
                customNonce = customNonce
            )
        }

        // Classical X25519 Fallback
        require(recipientPubKey.size == 32) { "Recipient public key must be 32 bytes" }

        val ephemeralPriv = customEphemeralPriv ?: ByteArray(32).also { Random.nextBytes(it) }
        val clampedPriv = Curve25519Engine.clampPrivateKey(ephemeralPriv)
        val ephemeralPub = IdentityCurve25519.generatePublicKey(clampedPriv)
        val sharedSecret = IdentityCurve25519.computeSharedSecret(clampedPriv, recipientPubKey)

        val salt = Sha256Digest.digest(ephemeralPub)
        val info = INFO_LABEL.encodeToByteArray()
        val kek = HkdfSha256.deriveKey(ikm = sharedSecret, salt = salt, info = info, length = 32)

        val nonce = customNonce ?: ByteArray(NONCE_SIZE).also { Random.nextBytes(it) }
        require(nonce.size == NONCE_SIZE) { "Nonce must be 12 bytes" }
        val encryptedPayload = AesGcm.encrypt(plaintext = dek, key = kek, iv = nonce)

        val wrappedBytes = ByteArray(NONCE_SIZE + encryptedPayload.size)
        nonce.copyInto(wrappedBytes, 0, 0, NONCE_SIZE)
        encryptedPayload.copyInto(wrappedBytes, NONCE_SIZE, 0, encryptedPayload.size)

        return SealedBoxEnvelope(
            ephemeralPubKeyHex = bytesToHex(ephemeralPub),
            wrappedDekBase64 = Base64.encode(wrappedBytes),
            mlKemCiphertextBase64 = null,
            securitySuite = SealedBoxEnvelope.SUITE_CLASSICAL
        )
    }

    /**
     * Unified unseal entry point with Anti-Downgrade enforcement:
     * If enforceHybrid is true, any classical envelope without ML-KEM-768 is rejected.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun unseal(
        envelope: SealedBoxEnvelope,
        recipientPrivKey: ByteArray,
        recipientMlKemPrivKey: ByteArray? = null,
        enforceHybrid: Boolean = false
    ): ByteArray {
        if (envelope.mlKemCiphertextBase64 != null) {
            requireNotNull(recipientMlKemPrivKey) { "Chave privada ML-KEM-768 obrigatória para desencapsular envelope híbrido" }
            return unsealHybrid(envelope, recipientPrivKey, recipientMlKemPrivKey)
        }

        if (enforceHybrid) {
            throw DowngradeAttackException(
                "Alerta de Segurança (Anti-Downgrade): Conexão requer nível mínimo pós-quântico (ML-KEM-768). Envelope clássico rejeitado."
            )
        }

        // Classical unseal
        require(recipientPrivKey.size == 32) { "Recipient private key must be 32 bytes" }

        val ephemeralPub = hexToBytes(envelope.ephemeralPubKeyHex)
        require(ephemeralPub.size == 32) { "Ephemeral public key must be 32 bytes" }

        val wrappedBytes = Base64.decode(envelope.wrappedDekBase64)
        require(wrappedBytes.size >= NONCE_SIZE + 16) { "Wrapped DEK payload is too short" }

        val nonce = ByteArray(NONCE_SIZE)
        wrappedBytes.copyInto(nonce, 0, 0, NONCE_SIZE)

        val cipherWithTag = ByteArray(wrappedBytes.size - NONCE_SIZE)
        wrappedBytes.copyInto(cipherWithTag, 0, NONCE_SIZE, wrappedBytes.size)

        val sharedSecret = IdentityCurve25519.computeSharedSecret(recipientPrivKey, ephemeralPub)
        val salt = Sha256Digest.digest(ephemeralPub)
        val info = INFO_LABEL.encodeToByteArray()
        val kek = HkdfSha256.deriveKey(ikm = sharedSecret, salt = salt, info = info, length = 32)

        return AesGcm.decrypt(ciphertext = cipherWithTag, key = kek, iv = nonce)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().lowercase()
        require(clean.length % 2 == 0) { "Hex string must have even length" }
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val high = clean[i * 2].digitToInt(16)
            val low = clean[i * 2 + 1].digitToInt(16)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
