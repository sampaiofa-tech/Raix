package com.example.security.identity

import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner
import java.security.SecureRandom

private class FixedSecureRandom(private val seed: ByteArray) : SecureRandom() {
    private var offset = 0
    override fun nextBytes(bytes: ByteArray) {
        for (i in bytes.indices) {
            bytes[i] = seed[offset % seed.size]
            offset++
        }
    }
}

actual object IdentityMlDsa65 {

    actual fun generateKeyPair(seed32: ByteArray): MlDsaKeyPair {
        require(seed32.size == 32) { "ML-DSA-65 seed must be exactly 32 bytes" }
        val generator = MLDSAKeyPairGenerator()
        generator.init(MLDSAKeyGenerationParameters(FixedSecureRandom(seed32), MLDSAParameters.ml_dsa_65))
        val keyPair = generator.generateKeyPair()
        val pubParams = keyPair.public as MLDSAPublicKeyParameters
        return MlDsaKeyPair(
            privateKey = seed32.copyOf(),
            publicKey = pubParams.encoded
        )
    }

    actual fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        val privParams = MLDSAPrivateKeyParameters(MLDSAParameters.ml_dsa_65, privateKey)
        val signer = MLDSASigner()
        signer.init(true, privParams)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    actual fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return try {
            val pubParams = MLDSAPublicKeyParameters(MLDSAParameters.ml_dsa_65, publicKey)
            val signer = MLDSASigner()
            signer.init(false, pubParams)
            signer.update(message, 0, message.size)
            signer.verifySignature(signature)
        } catch (e: Exception) {
            false
        }
    }
}
