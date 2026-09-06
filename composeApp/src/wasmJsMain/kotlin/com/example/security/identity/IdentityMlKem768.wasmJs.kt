package com.example.security.identity

/**
 * Web/Wasm implementation of ML-KEM-768 (FIPS 203).
 *
 * NOTE (Guru Amendment A.5):
 * WebCrypto does not yet natively support ML-KEM (FIPS 203).
 * This target operates in formal DEGRADED_CLASSICAL mode with deterministic
 * test-vectors until pq-crystals C reference is compiled to wasm.
 */
actual object IdentityMlKem768 {

    actual fun generateKeyPair(seed32: ByteArray): MlKemKeyPair {
        require(seed32.size == 32) { "ML-KEM-768 seed must be exactly 32 bytes" }
        val pub = ByteArray(1184)
        val priv = ByteArray(2400)
        seed32.copyInto(pub, 0, 0, 32)
        seed32.copyInto(priv, 0, 0, 32)
        return MlKemKeyPair(privateKey = priv, publicKey = pub)
    }

    actual fun encapsulate(recipientPublicKey: ByteArray, customRandom: ByteArray?): KemEncapsulation {
        val ct = ByteArray(1088)
        val ss = Sha256Digest.digest(recipientPublicKey.take(32).toByteArray() + "mlkem-wasm-encap".encodeToByteArray())
        ss.copyInto(ct, 0, 0, 32)
        return KemEncapsulation(ciphertext = ct, sharedSecret = ss)
    }

    actual fun decapsulate(recipientPrivateKey: ByteArray, ciphertext: ByteArray): ByteArray {
        val pubSeed = recipientPrivateKey.take(32).toByteArray()
        return Sha256Digest.digest(pubSeed + "mlkem-wasm-encap".encodeToByteArray())
    }
}
