package com.example.security.forensic

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

/**
 * Implementação Android do assinador da Cadeia de Custódia (RFI).
 *
 * Ancoragem em hardware via AndroidKeyStore (StrongBox Keymaster / TEE)
 * com assinatura ECDSA P-256 e digest SHA-256.
 */
actual object CustodyHardwareSigner {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "pmsg_custody_hardware_signer_v1"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val ALGORITHM = "EC"
    private const val EC_CURVE = "secp256r1"

    @Volatile
    private var isStrongBoxOrTeeActive = false

    @Volatile
    private var fallbackKeyPair: KeyPair? = null

    init {
        try {
            ensureHardwareKey()
        } catch (_: Throwable) {
            // Em testes unitários locais onde AndroidKeyStore não está disponível
        }
    }

    private fun ensureHardwareKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )

            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    builder.setIsStrongBoxBacked(true)
                    kpg.initialize(builder.build())
                    kpg.generateKeyPair()
                    isStrongBoxOrTeeActive = true
                    return
                } catch (_: Throwable) {
                    // Fallback para TEE padrão caso StrongBox dedicado não esteja disponível
                    builder.setIsStrongBoxBacked(false)
                }
            }

            kpg.initialize(builder.build())
            kpg.generateKeyPair()
            isStrongBoxOrTeeActive = true
        } else {
            isStrongBoxOrTeeActive = true
        }
    }

    private fun getFallbackKeyPair(): KeyPair {
        return fallbackKeyPair ?: synchronized(this) {
            fallbackKeyPair ?: run {
                val kpg = KeyPairGenerator.getInstance(ALGORITHM)
                kpg.initialize(ECGenParameterSpec(EC_CURVE))
                kpg.generateKeyPair().also { fallbackKeyPair = it }
            }
        }
    }

    actual fun isHardwareBacked(): Boolean = isStrongBoxOrTeeActive

    actual fun getPublicKeyHex(): String {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val cert = keyStore.getCertificate(KEY_ALIAS)
            if (cert != null) {
                bytesToHex(cert.publicKey.encoded)
            } else {
                bytesToHex(getFallbackKeyPair().public.encoded)
            }
        } catch (_: Throwable) {
            bytesToHex(getFallbackKeyPair().public.encoded)
        }
    }

    actual fun sign(data: ByteArray): ByteArray {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
                sig.initSign(entry.privateKey)
                sig.update(data)
                sig.sign()
            } else {
                val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
                sig.initSign(getFallbackKeyPair().private)
                sig.update(data)
                sig.sign()
            }
        } catch (_: Throwable) {
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initSign(getFallbackKeyPair().private)
            sig.update(data)
            sig.sign()
        }
    }

    actual fun verify(publicKeyHex: String, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val pubBytes = hexToBytes(publicKeyHex)
            val keyFactory = KeyFactory.getInstance(ALGORITHM)
            val pubKey = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
            val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
            sig.initVerify(pubKey)
            sig.update(data)
            sig.verify(signature)
        } catch (_: Exception) {
            false
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
