package com.example.security.identity

/**
 * Enumeration of supported digital signature algorithms (Crypto-Agility).
 * In accordance with NIST FIPS 204 and RFC 8032.
 */
enum class SignatureAlgorithm(val id: String) {
    ED25519("ed25519"),
    ML_DSA_65("ml-dsa-65"),
    HYBRID_ED25519_ML_DSA_65("ed25519+ml-dsa-65")
}

/**
 * Abstract Signature Scheme interface ensuring Crypto-Agility (NIST PQC Migration).
 * Decouples high-level identity routing and handshake logic from underlying primitives.
 */
interface SignatureScheme {
    val algorithm: SignatureAlgorithm
    fun sign(privateKey: ByteArray, message: ByteArray): ByteArray
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}

/**
 * Classical Ed25519 (RFC 8032) Signature Scheme.
 */
class Ed25519SignatureScheme : SignatureScheme {
    override val algorithm: SignatureAlgorithm = SignatureAlgorithm.ED25519

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        return IdentityEd25519.sign(privateKey, message)
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return IdentityEd25519.verify(publicKey, message, signature)
    }
}

/**
 * Post-Quantum ML-DSA-65 (NIST FIPS 204) Signature Scheme.
 */
class MlDsa65SignatureScheme : SignatureScheme {
    override val algorithm: SignatureAlgorithm = SignatureAlgorithm.ML_DSA_65

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        return IdentityMlDsa65.sign(privateKey, message)
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return IdentityMlDsa65.verify(publicKey, message, signature)
    }
}

/**
 * Hybrid Compound Signature Scheme: Ed25519 + ML-DSA-65.
 *
 * Wire format for composite signature:
 * [4-byte big-endian edSigLen][ed25519Signature][mlDsaSignature]
 *
 * Wire format for composite public key:
 * [4-byte big-endian edPubLen][ed25519PublicKey][mlDsaPublicKey]
 *
 * Wire format for composite private key:
 * [4-byte big-endian edPrivLen][ed25519PrivateKey][mlDsaPrivateKey]
 *
 * Verification succeeds if and only if BOTH Ed25519 and ML-DSA-65 signatures are valid.
 */
class HybridEd25519MlDsa65SignatureScheme : SignatureScheme {
    override val algorithm: SignatureAlgorithm = SignatureAlgorithm.HYBRID_ED25519_ML_DSA_65

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        require(privateKey.size >= 4) { "Invalid hybrid private key format" }
        val edPrivLen = ((privateKey[0].toInt() and 0xFF) shl 24) or
                ((privateKey[1].toInt() and 0xFF) shl 16) or
                ((privateKey[2].toInt() and 0xFF) shl 8) or
                (privateKey[3].toInt() and 0xFF)
        require(privateKey.size >= 4 + edPrivLen) { "Corrupt hybrid private key length" }

        val edPriv = privateKey.copyOfRange(4, 4 + edPrivLen)
        val mlDsaPriv = privateKey.copyOfRange(4 + edPrivLen, privateKey.size)

        val edSig = IdentityEd25519.sign(edPriv, message)
        val mlDsaSig = IdentityMlDsa65.sign(mlDsaPriv, message)

        val edSigLen = edSig.size
        val result = ByteArray(4 + edSigLen + mlDsaSig.size)
        result[0] = ((edSigLen ushr 24) and 0xFF).toByte()
        result[1] = ((edSigLen ushr 16) and 0xFF).toByte()
        result[2] = ((edSigLen ushr 8) and 0xFF).toByte()
        result[3] = (edSigLen and 0xFF).toByte()

        edSig.copyInto(result, 4, 0, edSigLen)
        mlDsaSig.copyInto(result, 4 + edSigLen, 0, mlDsaSig.size)
        return result
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size < 4 || signature.size < 4) return false

        // Unpack public key
        val edPubLen = ((publicKey[0].toInt() and 0xFF) shl 24) or
                ((publicKey[1].toInt() and 0xFF) shl 16) or
                ((publicKey[2].toInt() and 0xFF) shl 8) or
                (publicKey[3].toInt() and 0xFF)
        if (publicKey.size < 4 + edPubLen) return false

        val edPub = publicKey.copyOfRange(4, 4 + edPubLen)
        val mlDsaPub = publicKey.copyOfRange(4 + edPubLen, publicKey.size)

        // Unpack signature
        val edSigLen = ((signature[0].toInt() and 0xFF) shl 24) or
                ((signature[1].toInt() and 0xFF) shl 16) or
                ((signature[2].toInt() and 0xFF) shl 8) or
                (signature[3].toInt() and 0xFF)
        if (signature.size < 4 + edSigLen) return false

        val edSig = signature.copyOfRange(4, 4 + edSigLen)
        val mlDsaSig = signature.copyOfRange(4 + edSigLen, signature.size)

        // Both must be valid (AND semantics)
        val edValid = IdentityEd25519.verify(edPub, message, edSig)
        if (!edValid) return false

        return IdentityMlDsa65.verify(mlDsaPub, message, mlDsaSig)
    }

    companion object {
        fun encodeCompositeKey(edKey: ByteArray, mlDsaKey: ByteArray): ByteArray {
            val edLen = edKey.size
            val result = ByteArray(4 + edLen + mlDsaKey.size)
            result[0] = ((edLen ushr 24) and 0xFF).toByte()
            result[1] = ((edLen ushr 16) and 0xFF).toByte()
            result[2] = ((edLen ushr 8) and 0xFF).toByte()
            result[3] = (edLen and 0xFF).toByte()

            edKey.copyInto(result, 4, 0, edLen)
            mlDsaKey.copyInto(result, 4 + edLen, 0, mlDsaKey.size)
            return result
        }
    }
}
