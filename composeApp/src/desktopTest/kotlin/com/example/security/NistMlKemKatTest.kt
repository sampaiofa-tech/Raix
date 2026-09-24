package com.example.security

import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPublicKeyParameters
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * KAT (Known Answer Test) usando vetor oficial do NIST FIPS 203 (ML-KEM-768).
 *
 * Fonte: NIST ACVP-Server (ML-KEM-keyGen-FIPS203)
 * https://github.com/usnistgov/ACVP-Server/tree/master/gen-val/json-files/ML-KEM-keyGen-FIPS203
 * Grupo: tgId=2 (ML-KEM-768), Caso: tcId=26
 *
 * Este teste valida a biblioteca Bouncy Castle diretamente contra o vetor
 * do NIST, contornando a separacao de dominio SHA-256 aplicada pela API
 * RAIX (IdentityMlKem768.generateKeyPair). A separacao de dominio impede
 * alimentar (d, z) diretamente pela API publica -- por isso instanciamos
 * o MLKEMKeyPairGenerator da BC sem a camada RAIX.
 *
 * Verifica: ek (chave publica, 1184 bytes) e dk (chave privada, 2400 bytes)
 * via SHA-256 dos bytes codificados.
 */
class NistMlKemKatTest {

    // ----------------------------------------------------------------
    // Vetor NIST ACVP ML-KEM-768 (tgId=2, tcId=26)
    // ----------------------------------------------------------------
    companion object {
        private const val NIST_D_HEX =
            "E582B7D75E6C80B05AE392A1FC9F7153B12390FD99930368CC67A768BAEBC8A0"
        private const val NIST_Z_HEX =
            "1CDACB8740C0B87C4A379575F187B367CBFA3B300BF591B109F79816E9CBE8F0"

        // SHA-256 dos bytes ek e dk esperados pelo NIST
        private const val EXPECTED_EK_SHA256 =
            "4158f6afb5e516c99f1da07da8c651348422b17c1f4e9a08ad73fb1f91249b3e"
        private const val EXPECTED_DK_SHA256 =
            "7aab35839207f72b310abe36e2daa1cc7ff6f7fa8941e439967cd47d9b437079"
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }

    private fun ByteArray.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(this).joinToString("") { "%02x".format(it) }
    }

    /**
     * SecureRandom deterministico que entrega (d || z) diretamente ao KeyGen.
     * Sem separacao de dominio -- injeta os bytes brutos do vetor NIST.
     */
    private class NistFixedRandom(private val entropy: ByteArray) : SecureRandom() {
        private var offset = 0
        override fun nextBytes(bytes: ByteArray) {
            System.arraycopy(entropy, offset, bytes, 0, bytes.size)
            offset += bytes.size
        }
    }

    @Test
    fun katNistAcvpMlKem768TcId26() {
        // 1. Montar entropia: d (32 bytes) + z (32 bytes)
        val d = hexToBytes(NIST_D_HEX)
        val z = hexToBytes(NIST_Z_HEX)
        assertEquals(32, d.size, "d deve ter 32 bytes")
        assertEquals(32, z.size, "z deve ter 32 bytes")

        // 2. Gerar par de chaves via BC direta
        val generator = MLKEMKeyPairGenerator()
        generator.init(
            MLKEMKeyGenerationParameters(
                NistFixedRandom(d + z),
                MLKEMParameters.ml_kem_768
            )
        )
        val keyPair = generator.generateKeyPair()

        val pub = keyPair.public as MLKEMPublicKeyParameters
        val priv = keyPair.private as MLKEMPrivateKeyParameters

        // 3. Validar tamanhos FIPS 203
        assertEquals(1184, pub.encoded.size, "ek deve ter 1184 bytes (ML-KEM-768)")
        assertEquals(2400, priv.encoded.size, "dk deve ter 2400 bytes (ML-KEM-768)")

        // 4. Validar hashes contra vetor NIST
        val ekSha256 = pub.encoded.sha256Hex()
        val dkSha256 = priv.encoded.sha256Hex()

        assertEquals(
            EXPECTED_EK_SHA256, ekSha256,
            "ek (chave publica) nao corresponde ao vetor NIST ACVP tcId=26"
        )
        assertEquals(
            EXPECTED_DK_SHA256, dkSha256,
            "dk (chave privada) nao corresponde ao vetor NIST ACVP tcId=26"
        )
    }
}
