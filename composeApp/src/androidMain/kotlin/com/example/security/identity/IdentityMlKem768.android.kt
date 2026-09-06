package com.example.security.identity

import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import java.security.SecureRandom

actual object IdentityMlKem768 {

    private val secureRandom = SecureRandom()

    actual fun generateKeyPair(seed32: ByteArray): MlKemKeyPair {
        require(seed32.isNotEmpty()) { "ML-KEM-768 seed cannot be empty" }
        val d = Sha256Digest.digest(seed32 + "raix-mlkem-d".encodeToByteArray())
        val z = Sha256Digest.digest(seed32 + "raix-mlkem-z".encodeToByteArray())
        val generator = MLKEMKeyPairGenerator()
        generator.init(MLKEMKeyGenerationParameters(secureRandom, MLKEMParameters.ml_kem_768))
        val keyPair = generator.internalGenerateKeyPair(d, z)
        val privParams = keyPair.private as MLKEMPrivateKeyParameters
        val pubParams = keyPair.public as MLKEMPublicKeyParameters
        return MlKemKeyPair(
            privateKey = privParams.encoded,
            publicKey = pubParams.encoded
        )
    }

    actual fun encapsulate(recipientPublicKey: ByteArray, customRandom: ByteArray?): KemEncapsulation {
        val pubParams = MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, recipientPublicKey)
        val generator = MLKEMGenerator(secureRandom)
        val secretWithEncapsulation = generator.generateEncapsulated(pubParams)
        return KemEncapsulation(
            ciphertext = secretWithEncapsulation.encapsulation,
            sharedSecret = secretWithEncapsulation.secret
        )
    }

    actual fun decapsulate(recipientPrivateKey: ByteArray, ciphertext: ByteArray): ByteArray {
        val privParams = MLKEMPrivateKeyParameters(MLKEMParameters.ml_kem_768, recipientPrivateKey)
        val extractor = MLKEMExtractor(privParams)
        return extractor.extractSecret(ciphertext)
    }
}
