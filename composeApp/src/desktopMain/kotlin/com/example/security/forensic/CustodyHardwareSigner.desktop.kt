package com.example.security.forensic

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * Implementação Desktop (JVM) do assinador da Cadeia de Custódia (RFI).
 *
 * Utiliza chaves assimétricas EC (NIST P-256 / secp256r1) com assinatura SHA256withECDSA,
 * protegidas em repouso via Windows DPAPI quando disponível.
 */
actual object CustodyHardwareSigner {

    private const val ALGORITHM = "EC"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val EC_CURVE = "secp256r1"

    @Volatile
    private var cachedKeyPair: KeyPair? = null

    private val storageFile: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        File(dir, "custody_key.dpapi")
    }

    private fun getOrCreateKeyPair(): KeyPair {
        return cachedKeyPair ?: synchronized(this) {
            cachedKeyPair ?: loadOrGenerateKeyPair().also { cachedKeyPair = it }
        }
    }

    private fun loadOrGenerateKeyPair(): KeyPair {
        if (Platform.isWindows() && storageFile.exists()) {
            try {
                val protectedBytes = storageFile.readBytes()
                val rawBytes = Crypt32Util.cryptUnprotectData(protectedBytes)
                if (rawBytes.size > 4) {
                    val pubLen = ((rawBytes[0].toInt() and 0xFF) shl 24) or
                            ((rawBytes[1].toInt() and 0xFF) shl 16) or
                            ((rawBytes[2].toInt() and 0xFF) shl 8) or
                            (rawBytes[3].toInt() and 0xFF)
                    if (rawBytes.size >= 4 + pubLen) {
                        val pubBytes = rawBytes.copyOfRange(4, 4 + pubLen)
                        val privBytes = rawBytes.copyOfRange(4 + pubLen, rawBytes.size)

                        val keyFactory = KeyFactory.getInstance(ALGORITHM)
                        val pubKey = keyFactory.generatePublic(X509EncodedKeySpec(pubBytes))
                        val privKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privBytes))
                        return KeyPair(pubKey, privKey)
                    }
                }
            } catch (_: Exception) {
                // Se a chave estiver corrompida, regenera nova chave
            }
        }

        // Gera nova chave assimétrica EC P-256
        val kpg = KeyPairGenerator.getInstance(ALGORITHM)
        kpg.initialize(ECGenParameterSpec(EC_CURVE))
        val newKeyPair = kpg.generateKeyPair()

        if (Platform.isWindows()) {
            try {
                val pubEncoded = newKeyPair.public.encoded
                val privEncoded = newKeyPair.private.encoded
                val raw = ByteArray(4 + pubEncoded.size + privEncoded.size)
                raw[0] = ((pubEncoded.size ushr 24) and 0xFF).toByte()
                raw[1] = ((pubEncoded.size ushr 16) and 0xFF).toByte()
                raw[2] = ((pubEncoded.size ushr 8) and 0xFF).toByte()
                raw[3] = (pubEncoded.size and 0xFF).toByte()
                System.arraycopy(pubEncoded, 0, raw, 4, pubEncoded.size)
                System.arraycopy(privEncoded, 0, raw, 4 + pubEncoded.size, privEncoded.size)

                val protectedBytes = Crypt32Util.cryptProtectData(raw)
                storageFile.writeBytes(protectedBytes)
            } catch (_: Exception) {
                // Fallback em memória
            }
        }
        return newKeyPair
    }

    actual fun isHardwareBacked(): Boolean {
        // Em Desktop, chaves residem em JVM CSP protegidas por DPAPI (sem enclave StrongBox/Secure Enclave nativo)
        return false
    }

    actual fun getPublicKeyHex(): String {
        val pub = getOrCreateKeyPair().public.encoded
        return bytesToHex(pub)
    }

    actual fun sign(data: ByteArray): ByteArray {
        val sig = Signature.getInstance(SIGNATURE_ALGORITHM)
        sig.initSign(getOrCreateKeyPair().private)
        sig.update(data)
        return sig.sign()
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
