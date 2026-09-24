package com.example.security

import com.example.security.identity.IdentityMlKem768
import com.example.security.identity.Sha256Digest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Teste PERMANENTE de determinismo e conformidade ML-KEM-768 (FIPS 203).
 *
 * Objetivo: garantir que a derivacao deterministica de chaves a partir de semente
 * fixa (usada na restauracao de identidade via mnemonico BIP-39) produz SEMPRE
 * os mesmos bytes de chave publica e privada, independentemente da versao da
 * biblioteca criptografica (Bouncy Castle).
 *
 * NOTA IMPORTANTE SOBRE MlKemFixedRandom:
 * A classe MlKemFixedRandom (definida em IdentityMlKem768.android.kt e
 * IdentityMlKem768.desktop.kt) e EXCLUSIVAMENTE destinada a derivacao
 * deterministica de identidade (restauracao a partir do mnemonico).
 * Ela NUNCA deve ser usada para geracao de chaves efemeras, de sessao,
 * ou para qualquer operacao de encapsulamento (encaps). A encapsulacao
 * (IdentityMlKem768.encapsulate) usa SecureRandom() do sistema, como
 * exigido pelo FIPS 203 para seguranca IND-CCA2.
 *
 * Hashes de referencia capturados e verificados em BC 1.79 e BC 1.85
 * (equivalencia byte-a-byte comprovada durante a migracao T4).
 *
 * Referencia: NIST FIPS 203 (Module-Lattice-Based Key-Encapsulation Mechanism Standard)
 * https://csrc.nist.gov/pubs/fips/203/final
 */
class MlKemDeterminismTest {

    // -----------------------------------------------------------------------
    // Utilidades
    // -----------------------------------------------------------------------

    private fun ByteArray.toHex(): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(this.size * 2)
        for (b in this) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    /**
     * Gera uma semente deterministicamente a partir de um indice.
     * Cada indice produz 32 bytes unicos via SHA-256(indice).
     */
    private fun seedFromIndex(index: Int): ByteArray {
        val input = ByteArray(4)
        input[0] = ((index ushr 24) and 0xFF).toByte()
        input[1] = ((index ushr 16) and 0xFF).toByte()
        input[2] = ((index ushr 8) and 0xFF).toByte()
        input[3] = (index and 0xFF).toByte()
        return Sha256Digest.digest(input)
    }

    // -----------------------------------------------------------------------
    // Semente de referencia e hashes fixos (KAT -- Known Answer Test)
    // -----------------------------------------------------------------------

    /**
     * Semente de referencia (32 bytes) usada na prova de equivalencia T4.
     * Padrao: byte[i] = 0x42 XOR (i and 0xFF), para i em [0, 31].
     */
    private val REFERENCE_SEED_32 = ByteArray(32) { (0x42 xor (it and 0xFF)).toByte() }

    // Hashes SHA-256 da chave publica e privada geradas a partir de REFERENCE_SEED_32.
    // Capturados em BC 1.79 (internalGenerateKeyPair) e verificados identicos em BC 1.85
    // (MlKemFixedRandom + generateKeyPair). Qualquer alteracao nestes valores indica
    // regressao na derivacao deterministica.
    private val EXPECTED_PUB_SHA256 = "86f5bc6d85b36233a0f46c70e189f5458352d829e5fb58eaa7680e7b4c32dbf2"
    private val EXPECTED_PRIV_SHA256 = "39ce3fec9da515080a91909231128189d41a9e6283fe790e1329dfade3cdef64"

    // Tamanhos FIPS 203, Nivel 3 (ML-KEM-768)
    private val FIPS203_PUB_KEY_LEN = 1184
    private val FIPS203_PRIV_KEY_LEN = 2400
    private val FIPS203_SHARED_SECRET_LEN = 32

    // -----------------------------------------------------------------------
    // Teste 1: KAT -- Known Answer Test com hashes de referencia
    // -----------------------------------------------------------------------

    /**
     * Verifica que a semente de referencia produz EXATAMENTE os mesmos hashes
     * de chave publica e privada documentados (capturados em BC 1.79/1.85).
     *
     * Referencia de conformidade: NIST FIPS 203, Secao 7.1 (KeyGen).
     * O keygen ML-KEM consome 64 bytes de aleatoriedade: d[32] + z[32].
     * A derivacao RAIX aplica SHA-256(seed32 + "raix-mlkem-d") para d
     * e SHA-256(seed32 + "raix-mlkem-z") para z, garantindo determinismo
     * e separacao de dominio.
     */
    @Test
    fun katDeterministicKeyGen() {
        val keyPair = IdentityMlKem768.generateKeyPair(REFERENCE_SEED_32)

        // Tamanhos FIPS 203 Nivel 3
        assertEquals(FIPS203_PUB_KEY_LEN, keyPair.publicKey.size,
            "Tamanho da chave publica ML-KEM-768 deve ser $FIPS203_PUB_KEY_LEN bytes")
        assertEquals(FIPS203_PRIV_KEY_LEN, keyPair.privateKey.size,
            "Tamanho da chave privada ML-KEM-768 deve ser $FIPS203_PRIV_KEY_LEN bytes")

        // Hashes de referencia (KAT)
        val pubHash = Sha256Digest.digestHex(keyPair.publicKey)
        val privHash = Sha256Digest.digestHex(keyPair.privateKey)

        assertEquals(EXPECTED_PUB_SHA256, pubHash,
            "Hash da chave publica diverge do KAT de referencia (regressao na derivacao)")
        assertEquals(EXPECTED_PRIV_SHA256, privHash,
            "Hash da chave privada diverge do KAT de referencia (regressao na derivacao)")
    }

    // -----------------------------------------------------------------------
    // Teste 2: Determinismo -- mesma seed -> mesma chave (idempotencia)
    // -----------------------------------------------------------------------

    /**
     * Executa a geracao de chaves 3 vezes com a mesma semente e verifica
     * igualdade byte-a-byte em todas as execucoes.
     */
    @Test
    fun samesSeedProducesSameKeys() {
        val kp1 = IdentityMlKem768.generateKeyPair(REFERENCE_SEED_32)
        val kp2 = IdentityMlKem768.generateKeyPair(REFERENCE_SEED_32)
        val kp3 = IdentityMlKem768.generateKeyPair(REFERENCE_SEED_32)

        assertTrue(kp1.publicKey.contentEquals(kp2.publicKey),
            "Chave publica nao e idempotente (execucao 1 vs 2)")
        assertTrue(kp1.privateKey.contentEquals(kp2.privateKey),
            "Chave privada nao e idempotente (execucao 1 vs 2)")
        assertTrue(kp2.publicKey.contentEquals(kp3.publicKey),
            "Chave publica nao e idempotente (execucao 2 vs 3)")
        assertTrue(kp2.privateKey.contentEquals(kp3.privateKey),
            "Chave privada nao e idempotente (execucao 2 vs 3)")
    }

    // -----------------------------------------------------------------------
    // Teste 3: Multiplas seeds -- seeds distintas -> chaves distintas
    // -----------------------------------------------------------------------

    /**
     * Gera chaves para 120 sementes distintas e verifica:
     * (a) cada semente produz a mesma chave ao ser reutilizada (determinismo);
     * (b) chaves de sementes distintas sao diferentes (ausencia de colisao).
     */
    @Test
    fun multipleDistinctSeedsProduceDistinctKeys() {
        val seedCount = 120
        val pubHashes = mutableSetOf<String>()
        val privHashes = mutableSetOf<String>()

        for (i in 0 until seedCount) {
            val seed = seedFromIndex(i)
            val kp1 = IdentityMlKem768.generateKeyPair(seed)
            val kp2 = IdentityMlKem768.generateKeyPair(seed)

            // (a) Determinismo: mesma seed -> mesma chave
            assertTrue(kp1.publicKey.contentEquals(kp2.publicKey),
                "Determinismo falhou para seed index=$i (chave publica)")
            assertTrue(kp1.privateKey.contentEquals(kp2.privateKey),
                "Determinismo falhou para seed index=$i (chave privada)")

            // (b) Tamanhos FIPS 203
            assertEquals(FIPS203_PUB_KEY_LEN, kp1.publicKey.size,
                "Tamanho da chave publica incorreto para seed index=$i")
            assertEquals(FIPS203_PRIV_KEY_LEN, kp1.privateKey.size,
                "Tamanho da chave privada incorreto para seed index=$i")

            // Coleta hashes para verificar unicidade
            pubHashes.add(Sha256Digest.digestHex(kp1.publicKey))
            privHashes.add(Sha256Digest.digestHex(kp1.privateKey))
        }

        // (b) Unicidade: todas as chaves devem ser distintas
        assertEquals(seedCount, pubHashes.size,
            "Colisao detectada em chaves publicas para $seedCount seeds distintas")
        assertEquals(seedCount, privHashes.size,
            "Colisao detectada em chaves privadas para $seedCount seeds distintas")
    }

    // -----------------------------------------------------------------------
    // Teste 4: Ciclo completo encaps/decaps com chave de referencia
    // -----------------------------------------------------------------------

    /**
     * Verifica que as chaves geradas deterministicamente a partir da semente
     * de referencia funcionam corretamente no ciclo completo:
     * encapsulate(pubKey) -> ciphertext + sharedSecret
     * decapsulate(privKey, ciphertext) -> sharedSecret'
     * Assercao: sharedSecret == sharedSecret'
     *
     * Este teste garante que as chaves nao sao apenas byte-strings validos,
     * mas chaves ML-KEM-768 funcionais para KEM.
     */
    @Test
    fun encapsDecapsRoundtripWithDeterministicKeys() {
        val keyPair = IdentityMlKem768.generateKeyPair(REFERENCE_SEED_32)

        val encapsResult = IdentityMlKem768.encapsulate(keyPair.publicKey)

        assertEquals(FIPS203_SHARED_SECRET_LEN, encapsResult.sharedSecret.size,
            "Shared secret do encaps deve ter $FIPS203_SHARED_SECRET_LEN bytes (FIPS 203)")

        val decapsSecret = IdentityMlKem768.decapsulate(keyPair.privateKey, encapsResult.ciphertext)

        assertEquals(FIPS203_SHARED_SECRET_LEN, decapsSecret.size,
            "Shared secret do decaps deve ter $FIPS203_SHARED_SECRET_LEN bytes (FIPS 203)")

        assertTrue(encapsResult.sharedSecret.contentEquals(decapsSecret),
            "Roundtrip encaps/decaps falhou: shared secrets divergem")
    }

    // -----------------------------------------------------------------------
    // Teste 5: Ciclo encaps/decaps com multiplas seeds
    // -----------------------------------------------------------------------

    /**
     * Executa o ciclo encaps/decaps para 10 sementes distintas, garantindo
     * que todas as chaves geradas deterministicamente sao funcionais.
     */
    @Test
    fun encapsDecapsWorksForMultipleSeeds() {
        for (i in 0 until 10) {
            val seed = seedFromIndex(i + 1000) // offsets distintos do teste anterior
            val keyPair = IdentityMlKem768.generateKeyPair(seed)

            val encapsResult = IdentityMlKem768.encapsulate(keyPair.publicKey)
            val decapsSecret = IdentityMlKem768.decapsulate(keyPair.privateKey, encapsResult.ciphertext)

            assertTrue(encapsResult.sharedSecret.contentEquals(decapsSecret),
                "Roundtrip encaps/decaps falhou para seed index=${i + 1000}")
        }
    }

    // -----------------------------------------------------------------------
    // Teste 6: Chaves de seeds distintas NAO decapsulam corretamente
    // -----------------------------------------------------------------------

    /**
     * Verifica que a decapsulacao com a chave privada ERRADA NAO produz
     * o mesmo shared secret (propriedade de seguranca IND-CCA2).
     */
    @Test
    fun wrongPrivateKeyDoesNotDecapsulateCorrectly() {
        val seedA = seedFromIndex(2000)
        val seedB = seedFromIndex(2001)
        val kpA = IdentityMlKem768.generateKeyPair(seedA)
        val kpB = IdentityMlKem768.generateKeyPair(seedB)

        // Encapsula com a chave publica de A
        val encapsResult = IdentityMlKem768.encapsulate(kpA.publicKey)

        // Tenta decapsular com a chave privada de B (errada)
        val wrongSecret = IdentityMlKem768.decapsulate(kpB.privateKey, encapsResult.ciphertext)

        // ML-KEM nao lanca excecao com chave errada (por design FIPS 203),
        // mas o shared secret sera diferente
        assertFalse(encapsResult.sharedSecret.contentEquals(wrongSecret),
            "Decapsulacao com chave errada NAO deveria produzir o mesmo shared secret")
    }
}
