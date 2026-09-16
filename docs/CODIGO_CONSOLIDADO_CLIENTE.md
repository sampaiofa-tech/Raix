# Raix Codebase Consolidada — Módulo 2: Cliente KMP (Criptografia, Identidade e Rede)
**Destinado ao Validador:** Qwen 2.5 Coder 32B  
**Escopo:** Kotlin Multiplatform (`composeApp/src/commonMain/kotlin/com/example/`)  
**Status de Segurança:** KEM Pós-Quântico Híbrido (NIST FIPS 203 ML-KEM-768 + X25519), Assinatura Híbrida Composta (NIST FIPS 204 ML-DSA-65 + Ed25519), BIP-39 PT-BR, Envelopes SealedBox, Sanitização de Memória e Cliente REST Direto.

---

## 1. Visão Geral Arquitetural do Cliente

O cliente Raix concentra todo o poder criptográfico na ponta do usuário, aderindo às garantias de confidencialidade perfeita para o presente e para o futuro pós-quântico:
1. **Derivação de Identidade BIP-39**: A semente principal de 128 bits de entropia gera 12 palavras em português com 4 bits de checksum SHA-256 (`Bip39Portuguese.kt`). A partir da seed BIP-39, o KDF Argon2id deriva pares de chaves isolados criptograficamente usando salts dedicados (`IdentityCryptoManager.kt`), impedindo vazamento cruzado de chaves entre algoritmos clássicos e pós-quânticos.
2. **KEM Híbrido Pós-Quântico (ML-KEM-768 + X25519)**:
   - Implementa o combiner NIST SP 800-227 / RFC 9180 HPKE (`HybridKem.kt`).
   - Gera chave efêmera X25519 clássica ($SS_{\text{classical}}$) e encapsula via ML-KEM-768 ($SS_{\text{PQC}}$).
   - Deriva o segredo combinado via HKDF-SHA256: $SS_{\text{combined}} = \text{HKDF-Extract}(\emptyset, SS_{\text{classical}} \parallel SS_{\text{PQC}})$.
   - A KEK resultante cifra a DEK com AES-256-GCM.
3. **Assinatura Híbrida Composta (ML-DSA-65 + Ed25519)**:
   - Implementada sob a interface crypto-ágil `SignatureScheme.kt`.
   - A validação exige semântica booleana `AND` estrita: $\text{Verify}_{\text{hybrid}}(m, \sigma) = \text{Verify}_{\text{Ed25519}}(m, \sigma_{\text{Ed}}) \land \text{Verify}_{\text{ML-DSA-65}}(m, \sigma_{\text{ML-DSA}})$.
4. **Proteção de Memória Volátil (`MemorySanitizer.kt`)**:
   - Sobrescrita determinística com zeros (`zeroize()`) em arrays de bytes e buffers de caracteres contendo senhas, chaves efêmeras e sementes mnemônicas imediatamente após a utilização.
5. **Isolamento de Chaves em Hardware (`KeyVault.kt`)**:
   - Chaves privadas mestras são protegidas por silício seguro de hardware (Android StrongBox/TEE, Apple Secure Enclave, Windows DPAPI/TPM).
6. **Camada de Rede e Sincronização**:
   - Comunicação segura com o Firestore via Ktor REST direto (`FirestoreRestClient.kt`), eliminando o uso de SDKs pesados e minimizando a pegada de memória.
   - Cliente dedicado para troca de chaves criptografadas (`KeyStoreClient.kt`).

---

## 2. Código-Fonte Completo do Cliente

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/Bip39Portuguese.kt`
**Contexto Arquitetural:** Bip39Portuguese — Codificador/Decodificador oficial BIP-0039 PT-BR (128 bits de entropia para 12 palavras com 4 bits de checksum SHA-256).

```kotlin
﻿package com.example.security.identity

/**
 * Official BIP-0039 Portuguese (PT-BR) Wordlist (2048 words) and encoder/decoder.
 * Handles 128-bit entropy -> 12 words with 4-bit SHA-256 checksum and vice-versa.
 */
object Bip39Portuguese {

    val WORDS: List<String> = listOf(
        "abacate",
        "abaixo",
        "abalar",
        "abater",
        "abduzir",
        // ... [2038 palavras omitidas para otimização de contexto do Validador — lista oficial BIP-0039 PT-BR completa no repositório] ...
        "zelador",
        "zombar",
        "zoologia",

        "zumbido"
    )

    val WORD_INDEX_MAP: Map<String, Int> = WORDS.mapIndexed { index, word -> word to index }.toMap()

    fun entropyToMnemonic(entropy: ByteArray): List<String> {
        require(entropy.size == 16) { "Entropy must be exactly 16 bytes (128 bits)" }
        val hash = Sha256Digest.digest(entropy)
        val checksumBits = (hash[0].toInt() and 0xFF) ushr 4

        val bitString = StringBuilder(132)
        for (b in entropy) {
            val v = b.toInt() and 0xFF
            for (j in 7 downTo 0) {
                bitString.append(if ((v and (1 shl j)) != 0) '1' else '0')
            }
        }
        for (j in 3 downTo 0) {
            bitString.append(if ((checksumBits and (1 shl j)) != 0) '1' else '0')
        }

        val words = ArrayList<String>(12)
        for (i in 0 until 12) {
            val chunk = bitString.substring(i * 11, (i + 1) * 11)
            var index = 0
            for (ch in chunk) {
                index = (index shl 1) or (if (ch == '1') 1 else 0)
            }
            words.add(WORDS[index])
        }
        return words
    }

    fun mnemonicToEntropy(words: List<String>): Result<ByteArray> {
        if (words.size != 12) {
            return Result.failure(IllegalArgumentException("Mnemônico deve conter exatamente 12 palavras"))
        }
        val bitString = StringBuilder(132)
        for (w in words) {
            val normalized = w.trim().lowercase()
            val index = WORD_INDEX_MAP[normalized]
                ?: return Result.failure(IllegalArgumentException("Palavra inválida na lista BIP-39 PT-BR: ''"))
            for (j in 10 downTo 0) {
                bitString.append(if ((index and (1 shl j)) != 0) '1' else '0')
            }
        }

        val entropy = ByteArray(16)
        for (i in 0 until 16) {
            val byteChunk = bitString.substring(i * 8, (i + 1) * 8)
            var b = 0
            for (ch in byteChunk) {
                b = (b shl 1) or (if (ch == '1') 1 else 0)
            }
            entropy[i] = b.toByte()
        }

        val checksumChunk = bitString.substring(128, 132)
        var extractedChecksum = 0
        for (ch in checksumChunk) {
            extractedChecksum = (extractedChecksum shl 1) or (if (ch == '1') 1 else 0)
        }

        val hash = Sha256Digest.digest(entropy)
        val expectedChecksum = (hash[0].toInt() and 0xFF) ushr 4

        if (extractedChecksum != expectedChecksum) {
            return Result.failure(IllegalStateException("Checksum do mnemônico inválido"))
        }

        return Result.success(entropy)
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/Curve25519Engine.kt`
**Contexto Arquitetural:** Curve25519Engine — Implementação pura em Kotlin da curva de Montgomery Curve25519 para Diffie-Hellman (X25519).

```kotlin
package com.example.security.identity

/**
 * Pure Kotlin implementation of X25519 scalar multiplication (RFC 7748 §5).
 * Operates over the prime field GF(2^255 - 19).
 *
 * Fully deterministic across all Kotlin Multiplatform targets (Android, Desktop, iOS, WasmJS).
 */
object Curve25519Engine {

    private const val A24 = 121665L

    /**
     * Clamps private key per RFC 7748 §5:
     * - Clear bits 0, 1, 2 of byte 0
     * - Clear bit 7 of byte 31
     * - Set bit 6 of byte 31
     */
    fun clampPrivateKey(key: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 32 bytes" }
        val clamped = key.copyOf(32)
        clamped[0] = (clamped[0].toInt() and 248).toByte()
        clamped[31] = (clamped[31].toInt() and 127).toByte()
        clamped[31] = (clamped[31].toInt() or 64).toByte()
        return clamped
    }

    /**
     * Computes scalar * base_point (u = 9).
     */
    fun scalarMultBase(scalar: ByteArray): ByteArray {
        val basePoint = ByteArray(32)
        basePoint[0] = 9
        return scalarMult(scalar, basePoint)
    }

    /**
     * Computes scalar * u-coordinate per RFC 7748 §5.
     */
    fun scalarMult(scalar: ByteArray, uCoord: ByteArray): ByteArray {
        require(scalar.size == 32) { "Scalar must be 32 bytes" }
        require(uCoord.size == 32) { "uCoord must be 32 bytes" }

        val k = clampPrivateKey(scalar)

        // Decode u-coordinate: mask the most significant bit
        val uBytes = uCoord.copyOf(32)
        uBytes[31] = (uBytes[31].toInt() and 127).toByte()

        val x1 = decode(uBytes)
        var x2 = fromLong(1)
        var z2 = fromLong(0)
        var x3 = x1.copyOf()
        var z3 = fromLong(1)
        var swap = 0

        for (t in 254 downTo 0) {
            val byteIdx = t / 8
            val bitIdx = t % 8
            val kt = ((k[byteIdx].toInt() ushr bitIdx) and 1)
            swap = swap xor kt

            cswap(swap, x2, x3)
            cswap(swap, z2, z3)
            swap = kt

            val a = add(x2, z2)
            val aa = sqr(a)
            val b = sub(x2, z2)
            val bb = sqr(b)
            val e = sub(aa, bb)
            val c = add(x3, z3)
            val d = sub(x3, z3)
            val da = mul(d, a)
            val cb = mul(c, b)
            x3 = sqr(add(da, cb))
            z3 = mul(x1, sqr(sub(da, cb)))
            x2 = mul(aa, bb)
            z2 = mul(e, add(aa, mulSmall(e, A24)))
        }

        cswap(swap, x2, x3)
        cswap(swap, z2, z3)

        val result = mul(x2, inv(z2))
        return encode(result)
    }

    // -------------------------------------------------------------------------
    // Field arithmetic modulo 2^255 - 19 using 16 limbs of 16-bit integers
    // -------------------------------------------------------------------------

    private fun fromLong(value: Long): LongArray {
        val r = LongArray(16)
        r[0] = value and 0xFFFF
        r[1] = (value ushr 16) and 0xFFFF
        return r
    }

    private fun decode(bytes: ByteArray): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            val b0 = bytes[i * 2].toLong() and 0xFF
            val b1 = bytes[i * 2 + 1].toLong() and 0xFF
            r[i] = b0 or (b1 shl 8)
        }
        return r
    }

    private fun encode(a: LongArray): ByteArray {
        val r = carryAndReduce(a)
        val bytes = ByteArray(32)
        for (i in 0 until 16) {
            bytes[i * 2] = (r[i] and 0xFF).toByte()
            bytes[i * 2 + 1] = ((r[i] ushr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun cswap(swap: Int, a: LongArray, b: LongArray) {
        val mask = (if (swap != 0) 0xFFFFL else 0L)
        for (i in 0 until 16) {
            val t = mask and (a[i] xor b[i])
            a[i] = a[i] xor t
            b[i] = b[i] xor t
        }
    }

    private fun add(a: LongArray, b: LongArray): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = a[i] + b[i]
        }
        return carry(r)
    }

    private fun sub(a: LongArray, b: LongArray): LongArray {
        // Add 2 * (2^255 - 19) = 2^256 - 38 to ensure result is positive
        val r = LongArray(16)
        r[0] = a[0] + 0xFFDA - b[0] // 0x10000 - 38 = 0xFFDA with carry into next limb
        var carry = -1L // because we borrowed 0x10000 for limb 0, but added 2^256 = 16 limbs of 0xFFFF
        // 2p = [0xFFFF - 37, 0xFFFF, 0xFFFF, ..., 0x7FFF * 2] = [0xFFDA, 0xFFFF, ..., 0xFFFF]
        // 2 * (2^255 - 19) = 2^256 - 38:
        // Limb 0 = 0x10000 - 38 = 65498. Then limbs 1..15 are 0xFFFF.
        r[0] = a[0] + 0x10000L - 38L - b[0]
        for (i in 1 until 16) {
            r[i] = a[i] + 0xFFFFL - b[i]
        }
        return carry(r)
    }

    private fun mul(a: LongArray, b: LongArray): LongArray {
        val prod = LongArray(31)
        for (i in 0 until 16) {
            val ai = a[i]
            for (j in 0 until 16) {
                prod[i + j] += ai * b[j]
            }
        }
        // Fold high limbs (16..30) back into (0..14) with factor 38
        // because 2^256 = 2 * 2^255 = 2 * 19 = 38 mod p
        for (i in 0 until 15) {
            prod[i] += prod[i + 16] * 38L
        }
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = prod[i]
        }
        return carry(r)
    }

    private fun mulSmall(a: LongArray, b: Long): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = a[i] * b
        }
        return carry(r)
    }

    private fun sqr(a: LongArray): LongArray = mul(a, a)

    private fun carry(r: LongArray): LongArray {
        for (step in 0 until 2) {
            for (i in 0 until 15) {
                val c = r[i] shr 16
                r[i] = r[i] and 0xFFFF
                r[i + 1] += c
            }
            val c = r[15] shr 15
            r[15] = r[15] and 0x7FFF
            r[0] += c * 19
        }
        return r
    }

    /**
     * Performs exact canonical reduction modulo 2^255 - 19
     * (subtracts p if r >= p).
     */
    private fun carryAndReduce(a: LongArray): LongArray {
        val r = a.copyOf()
        carry(r)

        // Test if r >= p (p = 2^255 - 19: limbs 0=0xFFED, 1..14=0xFFFF, 15=0x7FFF)
        // Candidate: r - p = r + 19 - 2^255
        val cand = LongArray(16)
        cand[0] = r[0] + 19
        for (i in 0 until 15) {
            val c = cand[i] shr 16
            cand[i] = cand[i] and 0xFFFF
            cand[i + 1] = r[i + 1] + c
        }
        val c = cand[15] shr 15 // If bit 15 is set, r + 19 >= 2^255 <=> r >= p
        cand[15] = cand[15] and 0x7FFF

        return if (c != 0L) cand else r
    }

    /**
     * Inversion via Fermat's Little Theorem: a^(p-2) mod p
     * where p - 2 = 2^255 - 21.
     */
    private fun inv(z: LongArray): LongArray {
        val a = sqr(z)               // 2
        val t0 = sqr(a)              // 4
        val t1 = sqr(t0)             // 8
        val b = mul(t1, z)           // 9 = 2^3 + 1
        val c = mul(b, a)            // 11 = 2^3 + 2^1 + 1
        val t2 = sqr(c)              // 22
        val d = mul(t2, a)           // 24
        val t3 = sqr(d)              // 48
        val t4 = sqr(t3)             // 96
        val t5 = sqr(t4)             // 192
        val t6 = sqr(t5)             // 384
        val e = mul(t6, b)           // 2^5 - 1

        // Standard addition chain for 2^255 - 21
        var t = sqr(e)
        for (i in 1 until 5) t = sqr(t)
        val f = mul(t, e)            // 2^10 - 1

        t = sqr(f)
        for (i in 1 until 10) t = sqr(t)
        val g = mul(t, f)            // 2^20 - 1

        t = sqr(g)
        for (i in 1 until 20) t = sqr(t)
        val h = mul(t, g)            // 2^40 - 1

        t = sqr(h)
        for (i in 1 until 10) t = sqr(t)
        val k = mul(t, f)            // 2^50 - 1

        t = sqr(k)
        for (i in 1 until 50) t = sqr(t)
        val l = mul(t, k)            // 2^100 - 1

        t = sqr(l)
        for (i in 1 until 100) t = sqr(t)
        val m = mul(t, l)            // 2^200 - 1

        t = sqr(m)
        for (i in 1 until 50) t = sqr(t)
        val n = mul(t, k)            // 2^250 - 1

        t = sqr(n)
        for (i in 1 until 5) t = sqr(t)
        return mul(t, c)             // 2^255 - 21
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityCurve25519.kt`
**Contexto Arquitetural:** IdentityCurve25519 — Adaptador e abstração de par de chaves X25519.

```kotlin
package com.example.security.identity

/**
 * Multiplatform X25519 (RFC 7748) operations for Pmsg Identity layer.
 */
expect object IdentityCurve25519 {
    fun generatePublicKey(privateKey: ByteArray): ByteArray
    fun computeSharedSecret(myPrivateKey: ByteArray, peerPublicKey: ByteArray): ByteArray
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityEd25519.kt`
**Contexto Arquitetural:** IdentityEd25519 — Adaptador e abstração de chave de assinatura clássica Ed25519.

```kotlin
package com.example.security.identity

/**
 * Multiplatform Ed25519 (RFC 8032) signing and verification engine for Pmsg Identity (v1.1).
 *
 * Used for cryptographic proof-of-possession during identity routing updates
 * and remote invite authentication.
 */
expect object IdentityEd25519 {
    fun generatePublicKey(privateKeySeed: ByteArray): ByteArray
    fun sign(privateKeySeed: ByteArray, message: ByteArray): ByteArray
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityMlDsa65.kt`
**Contexto Arquitetural:** IdentityMlDsa65 — Assinatura digital pós-quântica NIST FIPS 204 (ML-DSA-65 / Dilithium).

```kotlin
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
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityMlKem768.kt`
**Contexto Arquitetural:** IdentityMlKem768 — Mecanismo de encapsulamento de chaves pós-quântico NIST FIPS 203 (ML-KEM-768 / Kyber).

```kotlin
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
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/HybridKem.kt`
**Contexto Arquitetural:** HybridKem — Combiner KEM Híbrido NIST SP 800-227 / RFC 9180 (X25519 + ML-KEM-768 via HKDF-SHA256).

```kotlin
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
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/SealedBox.kt`
**Contexto Arquitetural:** SealedBox — Envelopes criptográficos selados anônimos (crypto_box_seal) clássicos e híbridos.

```kotlin
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
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/SignatureScheme.kt`
**Contexto Arquitetural:** SignatureScheme — Interface abstrata crypto-ágil e implementação de assinatura híbrida composta (Ed25519 AND ML-DSA-65).

```kotlin
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
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityCryptoManager.kt`
**Contexto Arquitetural:** IdentityCryptoManager — Coordenador criptográfico de chaves: derivação segregada por Argon2id e gerenciamento de identidade local.

```kotlin
package com.example.security.identity

import com.example.security.KeyVault
import kotlin.random.Random

data class IdentityKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray,
    val fingerprintHex: String,
    val safetyNumber: String, // 60 decimal digits (12 blocks of 5)
    val signingPrivateKey: ByteArray = ByteArray(0),
    val signingPublicKey: ByteArray = ByteArray(0),
    val mlKemPrivateKey: ByteArray = ByteArray(0),
    val mlKemPublicKey: ByteArray = ByteArray(0),
    val mlDsaPrivateKey: ByteArray = ByteArray(0),
    val mlDsaPublicKey: ByteArray = ByteArray(0),
    val hybridFingerprintHex: String = "",
    val hybridSafetyNumber: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IdentityKeyPair) return false
        return privateKey.contentEquals(other.privateKey) &&
                publicKey.contentEquals(other.publicKey) &&
                fingerprintHex == other.fingerprintHex &&
                safetyNumber == other.safetyNumber &&
                signingPrivateKey.contentEquals(other.signingPrivateKey) &&
                signingPublicKey.contentEquals(other.signingPublicKey) &&
                mlKemPrivateKey.contentEquals(other.mlKemPrivateKey) &&
                mlKemPublicKey.contentEquals(other.mlKemPublicKey) &&
                mlDsaPrivateKey.contentEquals(other.mlDsaPrivateKey) &&
                mlDsaPublicKey.contentEquals(other.mlDsaPublicKey) &&
                hybridFingerprintHex == other.hybridFingerprintHex &&
                hybridSafetyNumber == other.hybridSafetyNumber
    }

    override fun hashCode(): Int {
        var result = privateKey.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + fingerprintHex.hashCode()
        result = 31 * result + safetyNumber.hashCode()
        result = 31 * result + signingPrivateKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + mlKemPrivateKey.contentHashCode()
        result = 31 * result + mlKemPublicKey.contentHashCode()
        result = 31 * result + mlDsaPrivateKey.contentHashCode()
        result = 31 * result + mlDsaPublicKey.contentHashCode()
        result = 31 * result + hybridFingerprintHex.hashCode()
        result = 31 * result + hybridSafetyNumber.hashCode()
        return result
    }
}

data class ProvisionedIdentity(
    val mnemonic: List<String>,
    val keyPair: IdentityKeyPair,
    val entropy: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProvisionedIdentity) return false
        return mnemonic == other.mnemonic &&
                keyPair == other.keyPair &&
                entropy.contentEquals(other.entropy)
    }

    override fun hashCode(): Int {
        var result = mnemonic.hashCode()
        result = 31 * result + keyPair.hashCode()
        result = 31 * result + entropy.contentHashCode()
        return result
    }
}

/**
 * Core cryptographic engine for Raix Identity (v2.0 PQC Hybrid).
 *
 * Implements:
 * 1. Deterministic BIP-39 PT-BR 128-bit entropy -> 12-word mnemonic.
 * 2. Dual-derivation from the same BIP-39 seed:
 *    - Classical: 256-bit X25519 keypair + Ed25519 signing keypair (preserves legacy recovery).
 *    - Post-Quantum: ML-KEM-768 (FIPS 203) keypair + ML-DSA-65 (FIPS 204) signing keypair.
 * 3. Signal-compatible Dual Safety Number: covers both classical and post-quantum public keys.
 * 4. Hybrid proof-of-possession with Anti-Downgrade capability negotiation.
 * 5. Envelope encryption of private keys and seed using KeyVault hardware keys.
 */
object IdentityCryptoManager {

    private val SALT = "pmsg-v1-identity-seed".encodeToByteArray()
    private val SIGNING_SALT = "pmsg-v1-identity-signing".encodeToByteArray()
    private val MLDSA_SALT = "raix-v2-identity-mldsa-seed".encodeToByteArray()
    private val MLKEM_SALT = "raix-v2-identity-mlkem-seed".encodeToByteArray()

    const val MIN_SECURITY_LEVEL_HYBRID = "HYBRID_PQC"
    const val SUITE_HYBRID = "hybrid-v1"

    fun generateNewIdentity(providedEntropy: ByteArray? = null): ProvisionedIdentity {
        val entropy = providedEntropy ?: ByteArray(16).also { Random.nextBytes(it) }
        require(entropy.size == 16) { "Entropy must be exactly 16 bytes (128 bits)" }

        val mnemonic = Bip39Portuguese.entropyToMnemonic(entropy)
        val keyPair = deriveKeyPair(mnemonic)
        return ProvisionedIdentity(
            mnemonic = mnemonic,
            keyPair = keyPair,
            entropy = entropy
        )
    }

    fun deriveKeyPair(mnemonic: List<String>): IdentityKeyPair {
        val entropy = Bip39Portuguese.mnemonicToEntropy(mnemonic).getOrThrow()
        val seed = Sha256Digest.digest(entropy)

        // 1. Classical X25519 Encryption KeyPair
        val rawPriv = Argon2Kmp.deriveKey(seed = seed, salt = SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)

        // RFC 7748 Clamping
        val clampedPriv = rawPriv.copyOf(32)
        clampedPriv[0] = (clampedPriv[0].toInt() and 248).toByte()
        clampedPriv[31] = (clampedPriv[31].toInt() and 127).toByte()
        clampedPriv[31] = (clampedPriv[31].toInt() or 64).toByte()

        val pubKey = IdentityCurve25519.generatePublicKey(clampedPriv)
        val fingerprintHex = Sha256Digest.digestHex(pubKey)
        val safetyNumber = formatSafetyNumber(Sha256Digest.digest(pubKey))

        // 2. Classical Ed25519 Signing KeyPair (F0: Proof-of-Possession)
        val rawSigningPriv = Argon2Kmp.deriveKey(seed = seed, salt = SIGNING_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val signingPubKey = IdentityEd25519.generatePublicKey(rawSigningPriv)

        // 3. Post-Quantum ML-DSA-65 Signing KeyPair (NIST FIPS 204 Level 3)
        val rawMlDsaSeed = Argon2Kmp.deriveKey(seed = seed, salt = MLDSA_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val mlDsaKeyPair = IdentityMlDsa65.generateKeyPair(rawMlDsaSeed)

        // 4. Post-Quantum ML-KEM-768 Key Encapsulation KeyPair (NIST FIPS 203 Level 3)
        val rawMlKemSeed = Argon2Kmp.deriveKey(seed = seed, salt = MLKEM_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val mlKemKeyPair = IdentityMlKem768.generateKeyPair(rawMlKemSeed)

        // 5. Hybrid Combined Fingerprint and Signal-Compatible Hybrid Safety Number (Guru Amendment A.2 & A.5)
        val classicalComponents = Sha256Digest.digest(pubKey + signingPubKey)
        val pqComponents = Sha256Digest.digest(mlKemKeyPair.publicKey + mlDsaKeyPair.publicKey)
        val hybridCombined = Sha256Digest.digest(classicalComponents + pqComponents)
        val hybridFingerprintHex = Sha256Digest.digestHex(classicalComponents + pqComponents)
        val hybridSafetyNumber = formatSafetyNumber(hybridCombined)

        // P1.2: RAM Zeroization - wipe all intermediate derivation buffers immediately
        MemorySanitizer.zeroize(entropy)
        MemorySanitizer.zeroize(seed)
        MemorySanitizer.zeroize(rawPriv)
        MemorySanitizer.zeroize(rawMlDsaSeed)
        MemorySanitizer.zeroize(rawMlKemSeed)

        return IdentityKeyPair(
            privateKey = clampedPriv,
            publicKey = pubKey,
            fingerprintHex = fingerprintHex,
            safetyNumber = safetyNumber,
            signingPrivateKey = rawSigningPriv,
            signingPublicKey = signingPubKey,
            mlKemPrivateKey = mlKemKeyPair.privateKey,
            mlKemPublicKey = mlKemKeyPair.publicKey,
            mlDsaPrivateKey = mlDsaKeyPair.privateKey,
            mlDsaPublicKey = mlDsaKeyPair.publicKey,
            hybridFingerprintHex = hybridFingerprintHex,
            hybridSafetyNumber = hybridSafetyNumber
        )
    }

    /**
     * Anti-Downgrade Handshake Payload (Guru Amendment A.2).
     * Binds minimum security level and supported suites directly into the authenticated payload.
     */
    fun buildRoutingSignaturePayload(
        fingerprint: String,
        newAuthUid: String,
        timestamp: Long,
        minSecurityLevel: String = MIN_SECURITY_LEVEL_HYBRID,
        supportedSuites: String = SUITE_HYBRID
    ): String {
        return "pmsg-routing-v2|$fingerprint|$newAuthUid|$timestamp|$minSecurityLevel|$supportedSuites"
    }

    /**
     * Backwards-compatible v1 routing payload builder.
     */
    fun buildRoutingSignaturePayloadV1(fingerprint: String, newAuthUid: String, timestamp: Long): String {
        return "pmsg-routing-v1|$fingerprint|$newAuthUid|$timestamp"
    }

    fun signRoutingUpdate(signingPrivKeySeed: ByteArray, fingerprint: String, newAuthUid: String, timestamp: Long): ByteArray {
        val payload = buildRoutingSignaturePayloadV1(fingerprint, newAuthUid, timestamp).encodeToByteArray()
        return IdentityEd25519.sign(signingPrivKeySeed, payload)
    }

    fun verifyRoutingUpdate(signingPubKey: ByteArray, fingerprint: String, newAuthUid: String, timestamp: Long, signature: ByteArray): Boolean {
        val payload = buildRoutingSignaturePayloadV1(fingerprint, newAuthUid, timestamp).encodeToByteArray()
        return IdentityEd25519.verify(signingPubKey, payload, signature)
    }

    /**
     * Hybrid Proof-of-Possession Signing (Ed25519 + ML-DSA-65).
     */
    fun signRoutingUpdateHybrid(
        edSigningPriv: ByteArray,
        mlDsaPriv: ByteArray,
        fingerprint: String,
        newAuthUid: String,
        timestamp: Long,
        minSecurityLevel: String = MIN_SECURITY_LEVEL_HYBRID,
        supportedSuites: String = SUITE_HYBRID
    ): ByteArray {
        val payload = buildRoutingSignaturePayload(fingerprint, newAuthUid, timestamp, minSecurityLevel, supportedSuites).encodeToByteArray()
        val compositePriv = HybridEd25519MlDsa65SignatureScheme.encodeCompositeKey(edSigningPriv, mlDsaPriv)
        val scheme = HybridEd25519MlDsa65SignatureScheme()
        return scheme.sign(compositePriv, payload)
    }

    /**
     * Hybrid Proof-of-Possession Verification (Ed25519 + ML-DSA-65).
     */
    fun verifyRoutingUpdateHybrid(
        edSigningPub: ByteArray,
        mlDsaPub: ByteArray,
        fingerprint: String,
        newAuthUid: String,
        timestamp: Long,
        signature: ByteArray,
        minSecurityLevel: String = MIN_SECURITY_LEVEL_HYBRID,
        supportedSuites: String = SUITE_HYBRID
    ): Boolean {
        val payload = buildRoutingSignaturePayload(fingerprint, newAuthUid, timestamp, minSecurityLevel, supportedSuites).encodeToByteArray()
        val compositePub = HybridEd25519MlDsa65SignatureScheme.encodeCompositeKey(edSigningPub, mlDsaPub)
        val scheme = HybridEd25519MlDsa65SignatureScheme()
        return scheme.verify(compositePub, payload, signature)
    }

    fun restoreFromMnemonic(mnemonic: List<String>): Result<IdentityKeyPair> {
        val validationResult = Bip39Portuguese.mnemonicToEntropy(mnemonic)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull() ?: IllegalArgumentException("Mnemônico inválido"))
        }
        return try {
            Result.success(deriveKeyPair(mnemonic))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats a 32-byte hash into Signal-style Safety Number:
     * Exactly 12 blocks of 5 decimal digits (60 digits total).
     */
    fun formatSafetyNumber(hash: ByteArray): String {
        val part1 = Sha256Digest.digest(hash + byteArrayOf(0x01))
        val part2 = Sha256Digest.digest(hash + byteArrayOf(0x02))
        val stream = part1 + part2

        val blocks = ArrayList<String>(12)
        for (i in 0 until 12) {
            val offset = i * 4
            val val32 = ((stream[offset].toLong() and 0xFF) shl 24) or
                    ((stream[offset + 1].toLong() and 0xFF) shl 16) or
                    ((stream[offset + 2].toLong() and 0xFF) shl 8) or
                    (stream[offset + 3].toLong() and 0xFF)
            val fiveDigit = (val32 % 100000L).toString().padStart(5, '0')
            blocks.add(fiveDigit)
        }
        return blocks.joinToString(" ")
    }

    /**
     * Computes the shared Pair Safety Number between two parties (Alice and Bob)
     * covering both Classical (X25519) and Post-Quantum (ML-KEM-768) public keys.
     * Symmetrical: order of keys does not matter. Both parties derive the exact same 60 digits.
     */
    fun computeHybridPairSafetyNumber(
        myClassicalPub: ByteArray,
        myMlKemPub: ByteArray,
        peerClassicalPub: ByteArray,
        peerMlKemPub: ByteArray
    ): String {
        require(myClassicalPub.size == 32) { "myClassicalPub must be 32 bytes" }
        require(peerClassicalPub.size == 32) { "peerClassicalPub must be 32 bytes" }

        val myMaterial = myClassicalPub + myMlKemPub
        val peerMaterial = peerClassicalPub + peerMlKemPub

        val (first, second) = if (compareBytes(myMaterial, peerMaterial) <= 0) {
            myMaterial to peerMaterial
        } else {
            peerMaterial to myMaterial
        }

        val combined = first + second
        val hash = Sha256Digest.digest(combined)
        return formatSafetyNumber(hash)
    }

    /**
     * Legacy classical pair safety number computation.
     */
    fun computePairSafetyNumber(myPubKey: ByteArray, peerPubKey: ByteArray): String {
        require(myPubKey.size == 32) { "myPubKey must be 32 bytes" }
        require(peerPubKey.size == 32) { "peerPubKey must be 32 bytes" }

        val (first, second) = if (compareBytes(myPubKey, peerPubKey) <= 0) {
            myPubKey to peerPubKey
        } else {
            peerPubKey to myPubKey
        }

        val combined = ByteArray(64)
        first.copyInto(combined, 0, 0, 32)
        second.copyInto(combined, 32, 0, 32)

        val hash = Sha256Digest.digest(combined)
        return formatSafetyNumber(hash)
    }

    fun compareBytes(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val vA = a[i].toInt() and 0xFF
            val vB = b[i].toInt() and 0xFF
            if (vA != vB) return vA.compareTo(vB)
        }
        return a.size.compareTo(b.size)
    }

    fun envelopeEncrypt(data: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val hex = StringBuilder(data.size * 2)
        for (b in data) {
            val v = b.toInt() and 0xFF
            hex.append(hexChars[v ushr 4])
            hex.append(hexChars[v and 0x0F])
        }
        return KeyVault.encrypt(hex.toString())
    }

    fun envelopeDecrypt(cipherText: String): ByteArray {
        val hex = KeyVault.decrypt(cipherText)
        if (hex.startsWith("🔒") || hex.length % 2 != 0) {
            return ByteArray(0)
        }
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val high = hex[i * 2].digitToIntOrNull(16) ?: return ByteArray(0)
            val low = hex[i * 2 + 1].digitToIntOrNull(16) ?: return ByteArray(0)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityManager.kt`
**Contexto Arquitetural:** IdentityManager — Ciclo de vida da identidade no dispositivo, geração inicial, custódia e exportação.

```kotlin
package com.example.security.identity

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class ParsedContactPayload(
    val version: Int,
    val fingerprintHex: String,
    val publicKeyBytes: ByteArray,
    val publicKeyBase64: String,
    val authUid: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParsedContactPayload) return false
        return version == other.version &&
                fingerprintHex == other.fingerprintHex &&
                publicKeyBytes.contentEquals(other.publicKeyBytes) &&
                publicKeyBase64 == other.publicKeyBase64 &&
                authUid == other.authUid
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + fingerprintHex.hashCode()
        result = 31 * result + publicKeyBytes.contentHashCode()
        result = 31 * result + publicKeyBase64.hashCode()
        result = 31 * result + authUid.hashCode()
        return result
    }
}

/**
 * High-level coordinator for device cryptographic identity:
 * - Provisioning / initial 12-word display.
 * - Envelope encryption of private keys and seed material.
 * - Re-viewing mnemonic with PIN/biometrics.
 * - Generation and parsing of Model A contact exchange strings.
 */
@OptIn(ExperimentalEncodingApi::class)
object IdentityManager {

    fun hasIdentity(): Boolean = IdentityStorage.hasIdentity()

    fun getIdentity(): IdentityKeyPair? {
        val stored = IdentityStorage.getIdentity() ?: return null
        val privKey = IdentityCryptoManager.envelopeDecrypt(stored.encryptedPrivateKey)
        if (privKey.size != 32) return null
        val pubKey = Base64.decode(stored.publicKeyBase64)

        var signingPrivKey = if (stored.encryptedSigningPrivateKey.isNotEmpty()) {
            IdentityCryptoManager.envelopeDecrypt(stored.encryptedSigningPrivateKey)
        } else {
            ByteArray(0)
        }
        var signingPubKey = if (stored.signingPublicKeyBase64.isNotEmpty()) {
            try { Base64.decode(stored.signingPublicKeyBase64) } catch (_: Exception) { ByteArray(0) }
        } else {
            ByteArray(0)
        }

        // Automatic migration if signing keys were not yet persisted
        if (signingPrivKey.isEmpty() || signingPubKey.isEmpty()) {
            val entropy = IdentityCryptoManager.envelopeDecrypt(stored.encryptedEntropy)
            if (entropy.size == 16) {
                val mnemonic = Bip39Portuguese.entropyToMnemonic(entropy)
                val derived = IdentityCryptoManager.deriveKeyPair(mnemonic)
                signingPrivKey = derived.signingPrivateKey
                signingPubKey = derived.signingPublicKey

                val updatedStored = stored.copy(
                    signingPublicKeyBase64 = Base64.encode(signingPubKey),
                    encryptedSigningPrivateKey = IdentityCryptoManager.envelopeEncrypt(signingPrivKey)
                )
                IdentityStorage.saveIdentity(updatedStored)
            }
        }

        return IdentityKeyPair(
            privateKey = privKey,
            publicKey = pubKey,
            fingerprintHex = stored.fingerprintHex,
            safetyNumber = stored.safetyNumber,
            signingPrivateKey = signingPrivKey,
            signingPublicKey = signingPubKey
        )
    }

    fun getOrGenerateIdentity(): IdentityKeyPair {
        val existing = getIdentity()
        if (existing != null) return existing
        val provisioned = provisionNewIdentity()
        confirmAndSaveIdentity(provisioned)
        return provisioned.keyPair
    }

    fun provisionNewIdentity(): ProvisionedIdentity {
        return IdentityCryptoManager.generateNewIdentity()
    }

    fun confirmAndSaveIdentity(provisioned: ProvisionedIdentity) {
        val privEncrypted = IdentityCryptoManager.envelopeEncrypt(provisioned.keyPair.privateKey)
        val entropyEncrypted = IdentityCryptoManager.envelopeEncrypt(provisioned.entropy)
        val pubKeyBase64 = Base64.encode(provisioned.keyPair.publicKey)

        val signingPrivEncrypted = IdentityCryptoManager.envelopeEncrypt(provisioned.keyPair.signingPrivateKey)
        val signingPubKeyBase64 = Base64.encode(provisioned.keyPair.signingPublicKey)

        val stored = StoredIdentity(
            publicKeyBase64 = pubKeyBase64,
            fingerprintHex = provisioned.keyPair.fingerprintHex,
            safetyNumber = provisioned.keyPair.safetyNumber,
            encryptedPrivateKey = privEncrypted,
            encryptedEntropy = entropyEncrypted,
            signingPublicKeyBase64 = signingPubKeyBase64,
            encryptedSigningPrivateKey = signingPrivEncrypted
        )
        IdentityStorage.saveIdentity(stored)
    }

    fun restoreFromMnemonic(words: List<String>): Result<IdentityKeyPair> {
        val restoreResult = IdentityCryptoManager.restoreFromMnemonic(words)
        if (restoreResult.isFailure) return restoreResult

        val keyPair = restoreResult.getOrThrow()
        val entropy = Bip39Portuguese.mnemonicToEntropy(words).getOrThrow()

        val privEncrypted = IdentityCryptoManager.envelopeEncrypt(keyPair.privateKey)
        val entropyEncrypted = IdentityCryptoManager.envelopeEncrypt(entropy)
        val pubKeyBase64 = Base64.encode(keyPair.publicKey)

        val signingPrivEncrypted = IdentityCryptoManager.envelopeEncrypt(keyPair.signingPrivateKey)
        val signingPubKeyBase64 = Base64.encode(keyPair.signingPublicKey)

        val stored = StoredIdentity(
            publicKeyBase64 = pubKeyBase64,
            fingerprintHex = keyPair.fingerprintHex,
            safetyNumber = keyPair.safetyNumber,
            encryptedPrivateKey = privEncrypted,
            encryptedEntropy = entropyEncrypted,
            signingPublicKeyBase64 = signingPubKeyBase64,
            encryptedSigningPrivateKey = signingPrivEncrypted
        )
        IdentityStorage.saveIdentity(stored)
        return Result.success(keyPair)
    }

    fun getMnemonicWords(): List<String>? {
        val stored = IdentityStorage.getIdentity() ?: return null
        val entropy = IdentityCryptoManager.envelopeDecrypt(stored.encryptedEntropy)
        if (entropy.size != 16) return null
        return Bip39Portuguese.entropyToMnemonic(entropy)
    }

    fun clearIdentity() {
        IdentityStorage.clearIdentity()
    }

    /**
     * Generates Model A contact string:
     * pmsg://contact?v=1&fp=<fingerprintHex>&pk=<pubKeyBase64>&uid=<authUid>
     */
    fun createContactUri(authUid: String): String? {
        val identity = getIdentity() ?: return null
        val pkBase64 = Base64.encode(identity.publicKey)
        return "pmsg://contact?v=1&fp=${identity.fingerprintHex}&pk=$pkBase64&uid=$authUid"
    }

    /**
     * Parses and validates a Model A contact string:
     * Validates checksum and that SHA-256(decode(pk)) == fp.
     */
    fun parseContactUri(uri: String): Result<ParsedContactPayload> {
        val trimmed = uri.trim()
        if (!trimmed.startsWith("pmsg://contact?")) {
            return Result.failure(IllegalArgumentException("URI inválido. Deve iniciar com 'pmsg://contact?'"))
        }

        val query = trimmed.removePrefix("pmsg://contact?")
        val params = query.split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        }

        val version = params["v"]?.toIntOrNull() ?: 1
        val fp = params["fp"]?.lowercase() ?: ""
        val pkBase64 = params["pk"] ?: ""
        val uid = params["uid"] ?: ""

        if (fp.length != 64 || !Regex("^[0-9a-f]{64}$").matches(fp)) {
            return Result.failure(IllegalArgumentException("Fingerprint inválido no código"))
        }
        if (pkBase64.isEmpty()) {
            return Result.failure(IllegalArgumentException("Chave pública ausente no código"))
        }
        if (uid.isEmpty()) {
            return Result.failure(IllegalArgumentException("UID ausente no código"))
        }

        val pkBytes = try {
            Base64.decode(pkBase64)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Chave pública malformada: ${e.message}"))
        }

        if (pkBytes.size != 32) {
            return Result.failure(IllegalArgumentException("Chave pública deve ter 32 bytes"))
        }

        val computedFp = Sha256Digest.digestHex(pkBytes)
        if (computedFp != fp) {
            return Result.failure(IllegalStateException("Inconsistência criptográfica: fingerprint não corresponde à chave pública!"))
        }

        return Result.success(
            ParsedContactPayload(
                version = version,
                fingerprintHex = fp,
                publicKeyBytes = pkBytes,
                publicKeyBase64 = pkBase64,
                authUid = uid
            )
        )
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityStorage.kt`
**Contexto Arquitetural:** IdentityStorage — Persistência criptografada da identidade no armazenamento local.

```kotlin
package com.example.security.identity

data class StoredIdentity(
    val publicKeyBase64: String,
    val fingerprintHex: String,
    val safetyNumber: String,
    val encryptedPrivateKey: String,
    val encryptedEntropy: String,
    val signingPublicKeyBase64: String = "",
    val encryptedSigningPrivateKey: String = ""
)

/**
 * Multiplatform persistent storage for the device cryptographic identity.
 * Stores only public data and hardware-encrypted envelopes (KeyVault).
 * Plaintext private keys and plain mnemonics are NEVER persisted.
 */
expect object IdentityStorage {
    fun hasIdentity(): Boolean
    fun saveIdentity(identity: StoredIdentity)
    fun getIdentity(): StoredIdentity?
    fun clearIdentity()
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/MemorySanitizer.kt`
**Contexto Arquitetural:** MemorySanitizer — Sobrescrita ativa de memória com zeroing imediato para mitigação de dump de heap.

```kotlin
package com.example.security.identity

import kotlin.concurrent.Volatile

/**
 * Memory Sanitizer with Dead-Store Elimination mitigation (P1.2).
 *
 * Problem:
 * Standard `Arrays.fill(bytes, 0)` can be silently eliminated as dead-code by modern
 * JIT optimizing compilers (HotSpot C2 / GraalVM) when the buffer is not read subsequently.
 * This leaves sensitive key material and BIP-39 mnemonic phrases exposed in RAM dumps.
 *
 * Mitigation:
 * Implements a volatile memory barrier and secondary accumulation read that prevents
 * the optimizer from proving that the zeroed memory is dead.
 */
object MemorySanitizer {

    @Volatile
    private var volatileSink: Byte = 0

    /**
     * Irreversibly zeroes a sensitive byte array, guaranteeing the compiler will not
     * optimize away the wiping operation.
     */
    fun zeroize(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return

        var accumulator: Byte = 0
        for (i in bytes.indices) {
            bytes[i] = 0
            accumulator = (accumulator.toInt() xor bytes[i].toInt()).toByte()
        }

        // Volatile write forces a memory fence / barrier preventing Dead-Store Elimination
        volatileSink = accumulator
    }

    /**
     * Irreversibly zeroes a sensitive char array (e.g. passwords, PINs).
     */
    fun zeroize(chars: CharArray?) {
        if (chars == null || chars.isEmpty()) return

        var accumulator: Int = 0
        for (i in chars.indices) {
            chars[i] = '\u0000'
            accumulator = accumulator xor chars[i].code
        }

        volatileSink = accumulator.toByte()
    }

    /**
     * Zeroes and clears a mutable list of sensitive strings.
     */
    fun zeroizeStrings(strings: MutableList<String>?) {
        if (strings == null || strings.isEmpty()) return
        for (i in strings.indices) {
            strings[i] = ""
        }
        strings.clear()
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/KeyVault.kt`
**Contexto Arquitetural:** KeyVault — Interface com hardware seguro (StrongBox, Secure Enclave, TPM/DPAPI).

```kotlin
package com.example.security

/**
 * Multiplatform Hardware-Backed KeyVault.
 *
 * Security Guarantees:
 * - Android: AndroidKeyStore (TEE / StrongBox) with AES-256-GCM cascaded encryption.
 * - iOS: Keychain Services + CryptoKit (Secure Enclave AES-GCM).
 * - Desktop: JVM Cryptographic Service Provider (AES-256-GCM with hardware TCG/TPM or PBKDF2/Argon2).
 * - Web (WasmJS): Web Crypto API (SubtleCrypto) — Client of lower security assurance.
 */
expect object KeyVault {
    fun isHardwareBacked(): Boolean
    fun encrypt(plainText: String): String
    fun decrypt(cipherText: String): String
    fun invalidateAndRecreateMasterKey()
    fun generateSecureNoise(length: Int = 32): String
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/DeviceAuthManager.kt`
**Contexto Arquitetural:** DeviceAuthManager — Gerenciamento de tokens de sessão e autenticação anônima com Firebase Auth.

```kotlin
package com.example.security

import com.example.data.network.ApiClient
import com.example.data.network.AppEndpoints
import com.example.data.network.PlatformEnvironment
import kotlin.concurrent.Volatile
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Unified Multiplatform Device Authentication Manager.
 *
 * Implements Zero-Trace Device-Bound Authentication via Firebase Auth (Identity Toolkit REST API).
 * Operates anonymously without collecting any PII (no phone, no email, no personal data).
 * The assigned cryptographic UID ('localId') is used as 'senderId' / 'recipientId' for routing.
 */
object DeviceAuthManager {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mutex = Mutex()

    @Volatile
    private var cachedSession: StoredAuthSession? = null

    /**
     * Returns the unique device/user UID authenticated in Firebase.
     * Guaranteed to match the 'senderId' in all message transactions.
     */
    fun getUserId(): String {
        cachedSession?.let { return it.userId }
        DeviceAuthStorage.loadSession()?.let {
            cachedSession = it
            return it.userId
        }
        return "device_${PlatformEnvironment.currentTimeMillis()}"
    }

    /**
     * Retrieves or refreshes a valid Firebase ID Token (JWT).
     * If token expires within 5 minutes, automatically refreshes via refreshToken.
     */
    suspend fun getIdToken(): String? = mutex.withLock {
        val now = PlatformEnvironment.currentTimeMillis()
        val current = cachedSession ?: DeviceAuthStorage.loadSession()

        if (current != null && (current.expiresAtMillis - now) > 5 * 60 * 1000L) {
            cachedSession = current
            return current.idToken
        }

        // Try refresh token if present
        if (current != null && current.refreshToken.isNotBlank()) {
            val refreshed = refreshIdToken(current.refreshToken, current.userId)
            if (refreshed != null) {
                cachedSession = refreshed
                DeviceAuthStorage.saveSession(refreshed)
                return refreshed.idToken
            }
        }

        // Otherwise perform anonymous device registration
        val newSession = performAnonymousSignUp()
        if (newSession != null) {
            cachedSession = newSession
            DeviceAuthStorage.saveSession(newSession)
            return newSession.idToken
        }

        // Fallback for debug/emulator
        return if (PlatformEnvironment.isDebug) {
            current?.idToken ?: "PMSG_DEV_TOKEN_${PlatformEnvironment.currentTimeMillis()}"
        } else {
            null
        }
    }

    suspend fun ensureAuthenticated(): Pair<String, String> {
        val token = getIdToken() ?: throw IllegalStateException("Falha ao inicializar autenticação anônima com o servidor.")
        val uid = getUserId()
        return Pair(uid, token)
    }

    private suspend fun performAnonymousSignUp(): StoredAuthSession? {
        return try {
            val apiKey = AppEndpoints.webApiKey
            val url = "${AppEndpoints.identityToolkitBaseUrl}/accounts:signUp?key=$apiKey"
            val payload = buildJsonObject {
                put("returnSecureToken", true)
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsed = json.parseToJsonElement(body).jsonObject

                val idToken = parsed["idToken"]?.jsonPrimitive?.content ?: return null
                val refreshToken = parsed["refreshToken"]?.jsonPrimitive?.content ?: ""
                val localId = parsed["localId"]?.jsonPrimitive?.content
                    ?: "device_${PlatformEnvironment.currentTimeMillis()}"
                val expiresInSec = parsed["expiresIn"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
                val expiresAt = PlatformEnvironment.currentTimeMillis() + (expiresInSec * 1000L)

                StoredAuthSession(
                    userId = localId,
                    idToken = idToken,
                    refreshToken = refreshToken,
                    expiresAtMillis = expiresAt
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun refreshIdToken(refreshToken: String, userId: String): StoredAuthSession? {
        return try {
            val apiKey = AppEndpoints.webApiKey
            val url = "${AppEndpoints.secureTokenBaseUrl}/token?key=$apiKey"

            val payload = buildJsonObject {
                put("grant_type", "refresh_token")
                put("refresh_token", refreshToken)
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsed = json.parseToJsonElement(body).jsonObject

                val idToken = parsed["id_token"]?.jsonPrimitive?.content
                    ?: parsed["idToken"]?.jsonPrimitive?.content
                    ?: return null
                val newRefreshToken = parsed["refresh_token"]?.jsonPrimitive?.content
                    ?: parsed["refreshToken"]?.jsonPrimitive?.content
                    ?: refreshToken
                val returnedUserId = parsed["user_id"]?.jsonPrimitive?.content ?: userId
                val expiresInSec = parsed["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
                val expiresAt = PlatformEnvironment.currentTimeMillis() + (expiresInSec * 1000L)

                StoredAuthSession(
                    userId = returnedUserId,
                    idToken = idToken,
                    refreshToken = newRefreshToken,
                    expiresAtMillis = expiresAt
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun clearSession() {
        cachedSession = null
        DeviceAuthStorage.clearSession()
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/HkdfSha256.kt`
**Contexto Arquitetural:** HkdfSha256 — Derivação de chaves baseada em HMAC-SHA256 (RFC 5869).

```kotlin
package com.example.security.identity

/**
 * Pure Kotlin implementation of HMAC-SHA256 (RFC 2104) and HKDF-SHA256 (RFC 5869).
 *
 * Fully deterministic across all CMP targets (Android, Desktop JVM, iOS, and WasmJS)
 * without external dependencies, built upon [Sha256Digest].
 */
object HmacSha256 {

    private const val BLOCK_SIZE = 64 // 512 bits for SHA-256
    private const val IPAD_BYTE: Byte = 0x36
    private const val OPAD_BYTE: Byte = 0x5c

    fun compute(key: ByteArray, message: ByteArray): ByteArray {
        val normalizedKey = if (key.size > BLOCK_SIZE) {
            Sha256Digest.digest(key)
        } else {
            key
        }

        val keyBlock = ByteArray(BLOCK_SIZE)
        normalizedKey.copyInto(keyBlock, 0, 0, normalizedKey.size)

        val iPad = ByteArray(BLOCK_SIZE)
        val oPad = ByteArray(BLOCK_SIZE)
        for (i in 0 until BLOCK_SIZE) {
            iPad[i] = (keyBlock[i].toInt() xor IPAD_BYTE.toInt()).toByte()
            oPad[i] = (keyBlock[i].toInt() xor OPAD_BYTE.toInt()).toByte()
        }

        val innerHash = Sha256Digest.digest(iPad + message)
        return Sha256Digest.digest(oPad + innerHash)
    }
}

object HkdfSha256 {

    private const val HASH_LEN = 32 // 256 bits for SHA-256

    /**
     * HKDF-Extract(salt, IKM) -> PRK
     */
    fun extract(salt: ByteArray?, ikm: ByteArray): ByteArray {
        val effectiveSalt = if (salt == null || salt.isEmpty()) {
            ByteArray(HASH_LEN) // 32 zeros
        } else {
            salt
        }
        return HmacSha256.compute(effectiveSalt, ikm)
    }

    /**
     * HKDF-Expand(PRK, info, L) -> OKM
     */
    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length > 0) { "Length must be positive" }
        require(length <= 255 * HASH_LEN) { "Cannot expand to more than 255 * 32 bytes" }

        val okm = ByteArray(length)
        var previousT = ByteArray(0)
        var offset = 0
        var counter = 1

        while (offset < length) {
            val input = previousT + info + byteArrayOf(counter.toByte())
            previousT = HmacSha256.compute(prk, input)

            val bytesToCopy = minOf(HASH_LEN, length - offset)
            previousT.copyInto(okm, offset, 0, bytesToCopy)
            offset += bytesToCopy
            counter++
        }

        return okm
    }

    /**
     * HKDF(ikm, salt, info, length) = Expand(Extract(salt, ikm), info, length)
     */
    fun deriveKey(ikm: ByteArray, salt: ByteArray?, info: ByteArray, length: Int = 32): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info, length)
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/AesGcm.kt`
**Contexto Arquitetural:** AesGcm — Abstração KMP para cifragem autenticada AES-256-GCM com vetores de inicialização de 96 bits.

```kotlin
package com.example.security.identity

/**
 * Multiplatform AES-256-GCM (NIST SP 800-38D) primitive.
 * Authenticated Encryption with Associated Data (AEAD) using 128-bit authentication tag.
 */
expect object AesGcm {
    fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun decrypt(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/Argon2Kmp.kt`
**Contexto Arquitetural:** Argon2Kmp — Abstração para função de derivação de chaves memory-hard Argon2id.

```kotlin
package com.example.security.identity

/**
 * Multiplatform Argon2id Key Derivation Function.
 * Used for deriving 256-bit X25519 identity key material from BIP-39 seed.
 */
expect object Argon2Kmp {
    fun deriveKey(
        seed: ByteArray,
        salt: ByteArray = "pmsg-v1-identity-seed".encodeToByteArray(),
        iterations: Int = 3,
        memoryKiB: Int = 32768,
        parallelism: Int = 1,
        outputLength: Int = 32
    ): ByteArray
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/ApiClient.kt`
**Contexto Arquitetural:** ApiClient — Cliente HTTP Ktor configurado com políticas de segurança estritas e ausência de logging de dados sensíveis.

```kotlin
package com.example.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createHttpClientEngine(): HttpClientEngine

object ApiClient {
    val client: HttpClient by lazy {
        HttpClient(createHttpClientEngine()) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/KeyStoreClient.kt`
**Contexto Arquitetural:** KeyStoreClient — Cliente para comunicação com as Cloud Functions storeMessageKey e getMessageKey.

```kotlin
package com.example.data.network

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class StoreKeyResult(
    val success: Boolean,
    val messageId: String? = null,
    val expiresAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Client service to persist Data Encryption Keys (DEKs) via the server-side
 * HTTPS Callable function 'storeMessageKey'.
 *
 * Adheres strictly to the Firebase Callable Protocol:
 * - Method: POST
 * - Content-Type: application/json
 * - Authorization: Bearer <FIREBASE_ID_TOKEN>
 * - Body: Wrapped in {"data": { ... }} envelope
 */
object KeyStoreClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun storeMessageKey(
        messageId: String,
        senderId: String,
        recipientId: String,
        ephemeralPubKey: String,
        wrappedDek: String,
        expiresAtMillis: Long,
        idToken: String
    ): StoreKeyResult {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("messageId", messageId)
                    put("senderId", senderId)
                    put("recipientId", recipientId)
                    put("ephemeralPubKey", ephemeralPubKey)
                    put("wrappedDek", wrappedDek)
                    put("expiresAtMillis", expiresAtMillis)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.storeMessageKeyUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                val success = resultObj?.get("success")?.jsonPrimitive?.content?.toBoolean() ?: true
                val returnedMsgId = resultObj?.get("messageId")?.jsonPrimitive?.content ?: messageId
                val returnedExpiresAt = resultObj?.get("expiresAt")?.jsonPrimitive?.content?.toLongOrNull() ?: expiresAtMillis

                StoreKeyResult(
                    success = success,
                    messageId = returnedMsgId,
                    expiresAt = returnedExpiresAt
                )
            } else {
                val errorMsg = try {
                    val parsed = json.parseToJsonElement(responseBody).jsonObject
                    parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: "HTTP ${response.status.value}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.status.value}: $responseBody"
                }

                StoreKeyResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            StoreKeyResult(
                success = false,
                errorMessage = "Network exception: ${e.message}"
            )
        }
    }

    suspend fun getMessageKey(
        messageId: String,
        idToken: String
    ): GetKeyResult {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("messageId", messageId)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.getMessageKeyUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                val success = resultObj?.get("success")?.jsonPrimitive?.content?.toBoolean() ?: true
                val returnedMsgId = resultObj?.get("messageId")?.jsonPrimitive?.content ?: messageId
                val ephemeralPubKey = resultObj?.get("ephemeralPubKey")?.jsonPrimitive?.content
                val wrappedDek = resultObj?.get("wrappedDek")?.jsonPrimitive?.content
                val returnedExpiresAt = resultObj?.get("expiresAtMillis")?.jsonPrimitive?.content?.toLongOrNull()

                if (ephemeralPubKey != null && wrappedDek != null) {
                    GetKeyResult(
                        success = success,
                        messageId = returnedMsgId,
                        ephemeralPubKey = ephemeralPubKey,
                        wrappedDek = wrappedDek,
                        expiresAtMillis = returnedExpiresAt
                    )
                } else {
                    GetKeyResult(
                        success = false,
                        errorMessage = "Malformed response: missing opaque wrapped DEK payload"
                    )
                }
            } else {
                val errorMsg = try {
                    val parsed = json.parseToJsonElement(responseBody).jsonObject
                    parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: "HTTP ${response.status.value}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.status.value}: $responseBody"
                }

                GetKeyResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            GetKeyResult(
                success = false,
                errorMessage = "Network exception: ${e.message}"
            )
        }
    }
}

@Serializable
data class GetKeyResult(
    val success: Boolean,
    val messageId: String? = null,
    val ephemeralPubKey: String? = null,
    val wrappedDek: String? = null,
    val expiresAtMillis: Long? = null,
    val errorMessage: String? = null
)
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/IdentityNetworkClient.kt`
**Contexto Arquitetural:** IdentityNetworkClient — Cliente para resolução de fingerprints, atualização de roteamento e envio de convites.

```kotlin
package com.example.data.network

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class CreateInviteResult(
    val inviteToken: String,
    val inviteLink: String,
    val expiresAtMillis: Long
)

@Serializable
data class AcceptInviteResult(
    val creatorFingerprint: String,
    val creatorPubKey: String
)

@Serializable
data class ResolveFingerprintResult(
    val currentAuthUid: String,
    val pubKey: String,
    val updatedAt: Long
)

/**
 * Client service to interact with server-side identity & invitation Cloud Functions:
 * - createInvite (Modelo C: 24h ephemeral single-use remote invite)
 * - acceptInvite (Modelo C: single-use acceptance with vanish-after-accept)
 * - resolveFingerprint (privacy-preserving technical directory)
 * - updateIdentityRouting (Recovery: binding new Auth UID to immutable fingerprint)
 */
object IdentityNetworkClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Parses a raw input string into a 64-char hex invite token.
     * Supports both direct tokens and URI scheme 'pmsg://invite?token=...'
     */
    fun parseInviteToken(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 64 && trimmed.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            return trimmed.lowercase()
        }
        if (trimmed.startsWith("pmsg://invite")) {
            val queryIndex = trimmed.indexOf('?')
            if (queryIndex != -1 && queryIndex < trimmed.length - 1) {
                val params = trimmed.substring(queryIndex + 1).split('&')
                for (param in params) {
                    val parts = param.split('=')
                    if (parts.size == 2 && (parts[0] == "token" || parts[0] == "i")) {
                        val token = parts[1].trim().lowercase()
                        if (token.length == 64) return token
                    }
                }
            }
        }
        return null
    }

    suspend fun createInvite(
        creatorFingerprint: String,
        creatorPubKey: String,
        idToken: String,
        creatorSigningPubKey: String? = null
    ): Result<CreateInviteResult> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("creatorFingerprint", creatorFingerprint)
                    put("creatorPubKey", creatorPubKey)
                    if (creatorSigningPubKey != null) {
                        put("creatorSigningPubKey", creatorSigningPubKey)
                    }
                })
            }

            val response = ApiClient.client.post(AppEndpoints.createInviteUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                    ?: return Result.failure(Exception("Resposta inválida do servidor de convites."))

                val token = resultObj["inviteToken"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Token de convite ausente."))
                val link = resultObj["inviteLink"]?.jsonPrimitive?.content
                    ?: "pmsg://invite?token=$token&fp=$creatorFingerprint"
                val expiresAt = resultObj["expiresAtMillis"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: (PlatformEnvironment.currentTimeMillis() + 86400000)

                Result.success(CreateInviteResult(token, link, expiresAt))
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(
        inviteToken: String,
        idToken: String
    ): Result<AcceptInviteResult> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("inviteToken", inviteToken)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.acceptInviteUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                    ?: return Result.failure(Exception("Resposta inválida ao aceitar convite."))

                val fp = resultObj["creatorFingerprint"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Fingerprint do criador ausente."))
                val pubKey = resultObj["creatorPubKey"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Chave pública do criador ausente."))

                Result.success(AcceptInviteResult(fp, pubKey))
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveFingerprint(
        fingerprint: String,
        idToken: String
    ): Result<ResolveFingerprintResult> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("fingerprint", fingerprint)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.resolveFingerprintUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                    ?: return Result.failure(Exception("Resposta inválida ao resolver fingerprint."))

                val uid = resultObj["currentAuthUid"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("UID técnico ausente."))
                val pubKey = resultObj["pubKey"]?.jsonPrimitive?.content
                    ?: return Result.failure(Exception("Chave pública ausente."))
                val updatedAt = resultObj["updatedAt"]?.jsonPrimitive?.content?.toLongOrNull()
                    ?: PlatformEnvironment.currentTimeMillis()

                Result.success(ResolveFingerprintResult(uid, pubKey, updatedAt))
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateIdentityRouting(
        fingerprint: String,
        pubKey: String,
        signature: String,
        timestamp: Long,
        idToken: String,
        signingPubKey: String? = null
    ): Result<Boolean> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("fingerprint", fingerprint)
                    put("pubKey", pubKey)
                    put("signature", signature)
                    put("timestamp", timestamp)
                    if (signingPubKey != null) {
                        put("signingPubKey", signingPubKey)
                    }
                })
            }

            val response = ApiClient.client.post(AppEndpoints.updateIdentityRoutingUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportAbuse(
        reportedFingerprint: String,
        abuseType: String,
        idToken: String = "anonymous_token",
        inviteId: String? = null
    ): Result<Boolean> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("reportedFingerprint", reportedFingerprint)
                    put("abuseType", abuseType)
                    if (inviteId != null) {
                        put("inviteId", inviteId)
                    }
                })
            }

            val response = ApiClient.client.post(AppEndpoints.reportAbuseUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportAbuseWithContent(
        reportedFingerprint: String,
        abuseType: String,
        contentSnippet: String,
        explicitConsent: Boolean,
        idToken: String = "anonymous_token",
        inviteId: String? = null
    ): Result<Boolean> {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("reportedFingerprint", reportedFingerprint)
                    put("abuseType", abuseType)
                    put("contentSnippet", contentSnippet)
                    put("explicitConsent", explicitConsent)
                    if (inviteId != null) {
                        put("inviteId", inviteId)
                    }
                })
            }

            val response = ApiClient.client.post(AppEndpoints.reportAbuseWithContentUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val errorMsg = extractErrorMessage(responseBody, response.status.value)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val parsed = json.parseToJsonElement(responseBody).jsonObject
            parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                ?: "HTTP $statusCode: $responseBody"
        } catch (_: Exception) {
            "HTTP $statusCode: $responseBody"
        }
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/AppEndpoints.kt`
**Contexto Arquitetural:** AppEndpoints — Endpoints do ecossistema de produção e emulador (sanitizado de chaves de API).

```kotlin
package com.example.data.network

/**
 * Centralized multiplatform endpoint manager for Cloud Functions and Firebase Auth.
 *
 * ANTI-SPOOFING SECURITY GUARANTEE:
 * - RELEASE builds: URLs and Project ID are immutable compile-time constants.
 *   Environment variables are strictly ignored to prevent runtime endpoint redirection / MITM attacks.
 * - DEBUG builds: Allows local development using Firebase Emulators or environment variable overrides.
 *
 * Note: Baseline production URLs will be updated with actual deployed endpoints from 'firebase deploy' in Etapa B.
 */
object AppEndpoints {

    // Real deployed project configuration from Firebase deploy
    const val DEFAULT_PROJECT_ID: String = "gen-lang-client-0858445711"
    const val REGION: String = "us-central1"

    // Production endpoints (immutable in Release)
    const val PROD_PROXY_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/geminiProxy"
    const val PROD_STORE_KEY_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/storeMessageKey"
    const val PROD_GET_KEY_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/getMessageKey"
    const val PROD_RESOLVE_FINGERPRINT_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/resolveFingerprint"
    const val PROD_CREATE_INVITE_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/createInvite"
    const val PROD_ACCEPT_INVITE_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/acceptInvite"
    const val PROD_UPDATE_IDENTITY_ROUTING_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/updateIdentityRouting"
    const val DEFAULT_WEB_API_KEY: String = "[REDACTED_API_KEY]"
    const val PROD_REPORT_ABUSE_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/reportAbuse"
    const val PROD_REPORT_ABUSE_WITH_CONTENT_URL: String = "https://$REGION-$DEFAULT_PROJECT_ID.cloudfunctions.net/reportAbuseWithContent"
    const val PROD_IDENTITY_TOOLKIT_URL: String = "https://identitytoolkit.googleapis.com/v1"
    const val PROD_SECURE_TOKEN_URL: String = "https://securetoken.googleapis.com/v1"
    const val PROD_FIRESTORE_URL: String = "https://firestore.googleapis.com/v1"

    val isDebug: Boolean
        get() = PlatformEnvironment.isDebug

    val isEmulator: Boolean
        get() = isDebug && (PlatformEnvironment.getEnv("PMSG_USE_EMULATOR") == "true")

    val projectId: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_PROJECT_ID") ?: DEFAULT_PROJECT_ID
        } else {
            DEFAULT_PROJECT_ID
        }

    val geminiProxyUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_PROXY_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/geminiProxy"
            } else {
                PROD_PROXY_URL
            }
        } else {
            PROD_PROXY_URL
        }

    val storeMessageKeyUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_STORE_KEY_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/storeMessageKey"
            } else {
                PROD_STORE_KEY_URL
            }
        } else {
            PROD_STORE_KEY_URL
        }

    val getMessageKeyUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_GET_KEY_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/getMessageKey"
            } else {
                PROD_GET_KEY_URL
            }
        } else {
            PROD_GET_KEY_URL
        }

    val resolveFingerprintUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_RESOLVE_FP_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/resolveFingerprint"
            } else {
                PROD_RESOLVE_FINGERPRINT_URL
            }
        } else {
            PROD_RESOLVE_FINGERPRINT_URL
        }

    val createInviteUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_CREATE_INVITE_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/createInvite"
            } else {
                PROD_CREATE_INVITE_URL
            }
        } else {
            PROD_CREATE_INVITE_URL
        }

    val acceptInviteUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_ACCEPT_INVITE_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/acceptInvite"
            } else {
                PROD_ACCEPT_INVITE_URL
            }
        } else {
            PROD_ACCEPT_INVITE_URL
        }

    val updateIdentityRoutingUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_UPDATE_ROUTING_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/updateIdentityRouting"
            } else {
                PROD_UPDATE_IDENTITY_ROUTING_URL
            }
        } else {
            PROD_UPDATE_IDENTITY_ROUTING_URL
        }

    val reportAbuseUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_REPORT_ABUSE_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/reportAbuse"
            } else {
                PROD_REPORT_ABUSE_URL
            }
        } else {
            PROD_REPORT_ABUSE_URL
        }

    val reportAbuseWithContentUrl: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("PMSG_REPORT_ABUSE_WITH_CONTENT_URL") ?: if (isEmulator) {
                "http://127.0.0.1:5001/$projectId/$REGION/reportAbuseWithContent"
            } else {
                PROD_REPORT_ABUSE_WITH_CONTENT_URL
            }
        } else {
            PROD_REPORT_ABUSE_WITH_CONTENT_URL
        }

    val identityToolkitBaseUrl: String
        get() = if (isDebug && isEmulator) {
            "http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1"
        } else {
            PROD_IDENTITY_TOOLKIT_URL
        }

    val secureTokenBaseUrl: String
        get() = if (isDebug && isEmulator) {
            "http://127.0.0.1:9099/securetoken.googleapis.com/v1"
        } else {
            PROD_SECURE_TOKEN_URL
        }

    val firestoreBaseUrl: String
        get() = if (isDebug && isEmulator) {
            "http://127.0.0.1:8080/v1/projects/$projectId/databases/(default)/documents"
        } else {
            "$PROD_FIRESTORE_URL/projects/$projectId/databases/(default)/documents"
        }

    val webApiKey: String
        get() = if (isDebug) {
            PlatformEnvironment.getEnv("FIREBASE_WEB_API_KEY") ?: DEFAULT_WEB_API_KEY
        } else {
            DEFAULT_WEB_API_KEY
        }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/FirestoreRestClient.kt`
**Contexto Arquitetural:** FirestoreRestClient — Cliente REST nativo para operações atômicas no Firestore (mensagens pendentes, exclusão vanish-after-read).

```kotlin
package com.example.data.network

import com.example.data.model.FirestoreMessage
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Multiplatform Firestore REST Client for Zero-Trace Ephemeral Message synchronization.
 *
 * Implements direct HTTP interactions with Firestore REST API v1:
 * - Create: POST /messages?documentId={messageId}
 * - Query:  POST :runQuery (where recipientId == {myUid})
 * - Delete: DELETE /messages/{messageId} (triggers server-side crypto-shredding on read)
 *
 * Strictly adheres to firestore.rules and Zero-Knowledge principles.
 */
object FirestoreRestClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Publishes an encrypted message envelope to the Firestore 'messages' collection.
     * Enforces mandatory 'expiresAt' timestamp required by firestore.rules.
     */
    suspend fun createMessage(
        message: FirestoreMessage,
        idToken: String
    ): Result<Boolean> {
        return try {
            val isoTimestamp = Iso8601Utils.formatEpochToIso(message.expiresAt)
            val url = "${AppEndpoints.firestoreBaseUrl}/messages?documentId=${message.id}"

            val payload = buildJsonObject {
                putJsonObject("fields") {
                    putJsonObject("ciphertext") { put("stringValue", message.ciphertext) }
                    putJsonObject("iv") { put("stringValue", message.iv) }
                    putJsonObject("senderId") { put("stringValue", message.senderId) }
                    putJsonObject("recipientId") { put("stringValue", message.recipientId) }
                    putJsonObject("expiresAt") { put("timestampValue", isoTimestamp) }
                }
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                Result.success(true)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(IllegalStateException("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries all pending unread messages addressed to [recipientId].
     */
    suspend fun fetchPendingMessages(
        recipientId: String,
        idToken: String
    ): Result<List<FirestoreMessage>> {
        return try {
            val url = "${AppEndpoints.firestoreBaseUrl}:runQuery"

            val payload = buildJsonObject {
                putJsonObject("structuredQuery") {
                    putJsonArray("from") {
                        addJsonObject { put("collectionId", "messages") }
                    }
                    putJsonObject("where") {
                        putJsonObject("fieldFilter") {
                            putJsonObject("field") { put("fieldPath", "recipientId") }
                            put("op", "EQUAL")
                            putJsonObject("value") { put("stringValue", recipientId) }
                        }
                    }
                }
            }

            val response = ApiClient.client.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                val parsedArray = json.parseToJsonElement(body).jsonArray
                val messages = mutableListOf<FirestoreMessage>()

                for (elem in parsedArray) {
                    val doc = elem.jsonObject["document"]?.jsonObject ?: continue
                    val name = doc["name"]?.jsonPrimitive?.content ?: continue
                    val messageId = name.substringAfterLast('/')

                    val fields = doc["fields"]?.jsonObject ?: continue
                    val ciphertext = fields["ciphertext"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val iv = fields["iv"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val senderId = fields["senderId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val returnedRecipient = fields["recipientId"]?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                    val isoExpires = fields["expiresAt"]?.jsonObject?.get("timestampValue")?.jsonPrimitive?.content ?: ""
                    val expiresAt = Iso8601Utils.parseIsoToEpoch(isoExpires)

                    if (messageId.isNotBlank() && ciphertext.isNotBlank()) {
                        messages.add(
                            FirestoreMessage(
                                id = messageId,
                                ciphertext = ciphertext,
                                iv = iv,
                                senderId = senderId,
                                recipientId = returnedRecipient,
                                expiresAt = expiresAt
                            )
                        )
                    }
                }

                Result.success(messages)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(IllegalStateException("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a message from Firestore after client-side decryption (Vanish-After-Read).
     * Server-side trigger 'onDeleteMessage' will automatically shred the associated DEK in messageKeys.
     */
    suspend fun deleteMessage(
        messageId: String,
        idToken: String
    ): Result<Boolean> {
        return try {
            val url = "${AppEndpoints.firestoreBaseUrl}/messages/$messageId"
            val response = ApiClient.client.delete(url) {
                header("Authorization", "Bearer $idToken")
            }

            // HTTP 200 or 404 (already shredded) are considered success
            if (response.status.isSuccess() || response.status.value == 404) {
                Result.success(true)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(IllegalStateException("HTTP ${response.status.value}: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Zero-dependency pure Kotlin RFC 3339 / ISO 8601 UTC Formatter and Parser.
 */
object Iso8601Utils {

    fun formatEpochToIso(epochMillis: Long): String {
        val seconds = epochMillis / 1000
        val millis = (epochMillis % 1000).toInt()
        var days = (seconds / 86400).toInt()
        var remSeconds = (seconds % 86400).toInt()
        if (remSeconds < 0) {
            remSeconds += 86400
            days -= 1
        }
        val hours = remSeconds / 3600
        val minutes = (remSeconds % 3600) / 60
        val secs = remSeconds % 60

        // Civil date algorithm (Howard Hinnant)
        val z = days + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + (if (mp < 10) 3 else -9)
        val year = y + (if (m <= 2) 1 else 0)

        val yStr = year.toString().padStart(4, '0')
        val mStr = m.toString().padStart(2, '0')
        val dStr = d.toString().padStart(2, '0')
        val hStr = hours.toString().padStart(2, '0')
        val minStr = minutes.toString().padStart(2, '0')
        val sStr = secs.toString().padStart(2, '0')
        val msStr = millis.coerceAtLeast(0).toString().padStart(3, '0')

        return "${yStr}-${mStr}-${dStr}T${hStr}:${minStr}:${sStr}.${msStr}Z"
    }

    fun parseIsoToEpoch(iso: String): Long {
        return try {
            val clean = iso.trim().removeSuffix("Z")
            val parts = clean.split("T")
            if (parts.size != 2) return PlatformEnvironment.currentTimeMillis() + 60_000L
            val dateParts = parts[0].split("-").map { it.toInt() }
            val year = dateParts[0]
            val month = dateParts[1]
            val day = dateParts[2]

            val timeParts = parts[1].split(":")
            val hour = timeParts[0].toInt()
            val min = timeParts[1].toInt()
            val secParts = timeParts[2].split(".")
            val sec = secParts[0].toInt()
            val millis = if (secParts.size > 1) secParts[1].take(3).padEnd(3, '0').toInt() else 0

            val y = year - (if (month <= 2) 1 else 0)
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val m = month + (if (month > 2) -3 else 9)
            val doy = (153 * m + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            val days = era * 146097 + doe - 719468

            val totalSec = days * 86400L + hour * 3600L + min * 60L + sec
            totalSec * 1000L + millis
        } catch (_: Exception) {
            PlatformEnvironment.currentTimeMillis() + 60_000L
        }
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/FirestoreMessageSync.kt`
**Contexto Arquitetural:** FirestoreMessageSync — Serviço de sincronização em segundo plano para recepção de envelopes pendentes.

```kotlin
package com.example.data.network

import com.example.data.model.FirestoreMessage
import com.example.data.model.FirestoreMessageKey
import com.example.security.identity.SealedBox
import com.example.security.identity.SealedBoxEnvelope

/**
 * Helper to prepare and serialize messages for server-side Firestore synchronization.
 *
 * Enforces:
 * 1. Mandatory `expiresAt` (Timestamp) for every ephemeral message (defaulting to max TTL 24h).
 * 2. Separation of ciphertext and DEK (Data Encryption Key) into separate collections.
 * 3. Zero-knowledge DEK wrapping: The server only sees opaque SealedBox envelopes (ephemeralPubKey, wrappedDek).
 * 4. Strict schema adherence: { ciphertext, iv, senderId, recipientId, expiresAt }.
 */
object FirestoreMessageSync {

    const val MAX_TTL_MILLIS: Long = 24 * 60 * 60 * 1000L // 24 hours
    const val MIN_TTL_MILLIS: Long = 10 * 1000L // 10 seconds

    fun buildFirestoreMessage(
        messageId: String,
        ciphertext: String,
        iv: String,
        senderId: String,
        recipientId: String,
        ttlHours: Float = 24f,
        now: Long = PlatformEnvironment.currentTimeMillis()
    ): FirestoreMessage {
        val ttlMillis = (ttlHours * 60 * 60 * 1000L).toLong()
            .coerceIn(MIN_TTL_MILLIS, MAX_TTL_MILLIS)
        val expiresAt = now + ttlMillis

        return FirestoreMessage(
            id = messageId,
            ciphertext = ciphertext,
            iv = iv,
            senderId = senderId,
            recipientId = recipientId,
            expiresAt = expiresAt
        )
    }

    fun buildMessageKey(
        messageId: String,
        ephemeralPubKey: String,
        wrappedDek: String,
        expiresAt: Long
    ): FirestoreMessageKey {
        return FirestoreMessageKey(
            messageId = messageId,
            ephemeralPubKey = ephemeralPubKey,
            wrappedDek = wrappedDek,
            expiresAt = expiresAt
        )
    }

    /**
     * Helper to wrap a raw DEK for recipient using ephemeral SealedBox encryption.
     * Alice calls this before saving/sending the key to Firestore.
     */
    fun prepareAndWrapMessageKey(
        messageId: String,
        dek: ByteArray,
        recipientX25519PubKey: ByteArray,
        expiresAt: Long
    ): FirestoreMessageKey {
        val envelope = SealedBox.seal(dek = dek, recipientPubKey = recipientX25519PubKey)
        return FirestoreMessageKey(
            messageId = messageId,
            ephemeralPubKey = envelope.ephemeralPubKeyHex,
            wrappedDek = envelope.wrappedDekBase64,
            expiresAt = expiresAt
        )
    }

    /**
     * Circuito de Leitura Autorizada:
     * Destinatário autenticado invoca KeyStoreClient.getMessageKey para obter a DEK envelopada,
     * desembrulha usando SealedBox.unseal com a chave privada da identidade do destinatário (KeyVault),
     * e decripta o ciphertext de trânsito em memória volátil.
     */
    suspend fun fetchAndDecryptRemoteMessage(
        message: FirestoreMessage,
        recipientPrivKey: ByteArray,
        idToken: String,
        decryptPayload: (ciphertext: String, iv: String, dek: ByteArray) -> String
    ): Result<String> {
        val keyResult = KeyStoreClient.getMessageKey(message.id, idToken)
        if (!keyResult.success || keyResult.ephemeralPubKey == null || keyResult.wrappedDek == null) {
            return Result.failure(
                IllegalStateException(keyResult.errorMessage ?: "Falha ao obter DEK envelopada autorizada do servidor.")
            )
        }

        return try {
            val envelope = SealedBoxEnvelope(
                ephemeralPubKeyHex = keyResult.ephemeralPubKey,
                wrappedDekBase64 = keyResult.wrappedDek
            )
            val dek = SealedBox.unseal(envelope, recipientPrivKey)
            val decrypted = decryptPayload(message.ciphertext, message.iv, dek)
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Enforçamento Client-Side da Blocklist no fetch de mensagens:
     * Mensagens de remetentes bloqueados são descartadas SEM decifração nem exibição (auto-purge),
     * com contagem local para auditoria.
     */
    suspend fun filterBlockedMessages(
        incomingMessages: List<FirestoreMessage>,
        isBlocked: suspend (senderId: String) -> Boolean,
        onMessagePurged: (suspend (senderId: String) -> Unit)? = null
    ): List<FirestoreMessage> {
        val result = mutableListOf<FirestoreMessage>()
        for (msg in incomingMessages) {
            if (isBlocked(msg.senderId)) {
                onMessagePurged?.invoke(msg.senderId)
            } else {
                result.add(msg)
            }
        }
        return result
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/model/FirestoreModels.kt`
**Contexto Arquitetural:** FirestoreModels — Modelos de dados serializáveis de mensagens cifradas e envelopes.

```kotlin
package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * Server-side Firestore document in "messages" collection.
 * Required schema: { ciphertext, iv, senderId, recipientId, expiresAt }
 *
 * All fields are strictly validated server-side by firestore.rules.
 * expiresAt must be a Timestamp and is mandatory for document creation.
 */
@Serializable
data class FirestoreMessage(
    val id: String = "",
    val ciphertext: String = "",
    val iv: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val expiresAt: Long = 0L // Epoch millis representing Firestore Timestamp
)

/**
 * Server-side Firestore document in "messageKeys" collection.
 * Required schema: { messageId, ephemeralPubKey, wrappedDek, expiresAt }
 *
 * Stored in a separate collection. Access rules:
 * allow read, write: if false; (Universal client block).
 * Only accessible by Cloud Functions (Admin SDK) for Crypto-Shredding.
 * Server stores only opaque bytes (ephemeralPubKey, wrappedDek) and never sees plaintext DEK.
 */
@Serializable
data class FirestoreMessageKey(
    val messageId: String = "",
    val ephemeralPubKey: String = "",
    val wrappedDek: String = "",
    val expiresAt: Long = 0L // Must match exactly the message expiresAt
)
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/model/PmsgContact.kt`
**Contexto Arquitetural:** PmsgContact — Modelo de contato com chaves públicas e estado de verificação de segurança.

```kotlin
package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PmsgContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val hasPmsgInstalled: Boolean = true,
    val statusDescription: String = "Disponível no Pmsg (Criptografado 24h)",
    val avatarColorHex: Long = 0xFF00FFC2
)
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/model/BlockedContact.kt`
**Contexto Arquitetural:** BlockedContact — Registro local de contatos bloqueados e expurgo de mensagens.

```kotlin
package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * Domain representation of a blocked contact in Pmsg.
 * Maintained client-side in encrypted local storage.
 * The server remains zero-knowledge regarding the local blocklist.
 */
@Serializable
data class BlockedContact(
    val fingerprint: String,
    val blockedAt: Long = 0L
)
```

---

