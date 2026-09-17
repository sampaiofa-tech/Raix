package com.example.security.identity

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
object AddressBookCrypto {
    fun encrypt(plainJson: String, key: ByteArray): String {
        val iv = ByteArray(12).also { Random.nextBytes(it) }
        val encrypted = AesGcm.encrypt(plainJson.encodeToByteArray(), key, iv)
        val combined = iv + encrypted
        return Base64.encode(combined)
    }

    fun decrypt(cipherTextBase64: String, key: ByteArray): String {
        try {
            val combined = Base64.decode(cipherTextBase64)
            if (combined.size < 12) return ""
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)
            val plainBytes = AesGcm.decrypt(cipherText, key, iv)
            return plainBytes.decodeToString()
        } catch (_: Throwable) {
            return ""
        }
    }
}
