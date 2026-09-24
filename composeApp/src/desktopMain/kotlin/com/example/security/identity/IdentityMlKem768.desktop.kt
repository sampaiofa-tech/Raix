package com.example.security.identity

import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import java.security.SecureRandom

/**
 * SecureRandom determinisico que reproduz bytes a partir de uma semente fixa.
 *
 * USO EXCLUSIVO: derivacao deterministica de identidade (restauracao a partir
 * do mnemonico BIP-39). Fornece os 64 bytes (d[32] + z[32]) exigidos pelo
 * FIPS 203 KeyGen de forma reprodutivel.
 *
 * NUNCA usar para:
 * - Geracao de chaves efemeras ou de sessao
 * - Operacoes de encapsulamento (encaps) -- usar SecureRandom() do sistema
 * - Qualquer contexto que exija aleatoriedade real (IND-CCA2)
 *
 * @param seed Bytes determinisicos (tipicamente d + z, 64 bytes no total)
 */
private class MlKemFixedRandom(private val seed: ByteArray) : SecureRandom() {
    private var offset = 0
    override fun nextBytes(bytes: ByteArray) {
        for (i in bytes.indices) {
            bytes[i] = seed[offset % seed.size]
            offset++
        }
    }
}

actual object IdentityMlKem768 {

    actual fun generateKeyPair(seed32: ByteArray): MlKemKeyPair {
        require(seed32.isNotEmpty()) { "ML-KEM-768 seed cannot be empty" }
        val d = Sha256Digest.digest(seed32 + "raix-mlkem-d".encodeToByteArray())
        val z = Sha256Digest.digest(seed32 + "raix-mlkem-z".encodeToByteArray())
        val generator = MLKEMKeyPairGenerator()
        generator.init(MLKEMKeyGenerationParameters(MlKemFixedRandom(d + z), MLKEMParameters.ml_kem_768))
        val keyPair = generator.generateKeyPair()
        val privParams = keyPair.private as MLKEMPrivateKeyParameters
        val pubParams = keyPair.public as MLKEMPublicKeyParameters
        return MlKemKeyPair(
            privateKey = privParams.encoded,
            publicKey = pubParams.encoded
        )
    }

    actual fun encapsulate(recipientPublicKey: ByteArray, customRandom: ByteArray?): KemEncapsulation {
        val pubParams = MLKEMPublicKeyParameters(MLKEMParameters.ml_kem_768, recipientPublicKey)
        val generator = MLKEMGenerator(SecureRandom())
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
