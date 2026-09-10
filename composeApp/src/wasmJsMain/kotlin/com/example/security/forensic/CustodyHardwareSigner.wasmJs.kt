package com.example.security.forensic

import com.example.security.identity.Sha256Digest

/**
 * Implementação Web (WasmJs) do assinador da Cadeia de Custódia (RFI).
 *
 * Cliente de menor garantia técnica (sem enclave de hardware físico acessível pelo navegador).
 */
actual object CustodyHardwareSigner {

    private val webSeed = "pmsg_web_wasm_custody_signer_seed_v1".encodeToByteArray()

    actual fun isHardwareBacked(): Boolean {
        // Navegadores web não oferecem ancoragem em silício físico isolado (StrongBox/Secure Enclave)
        return false
    }

    actual fun getPublicKeyHex(): String {
        return Sha256Digest.digestHex(webSeed)
    }

    actual fun sign(data: ByteArray): ByteArray {
        return Sha256Digest.digest(webSeed + data)
    }

    actual fun verify(publicKeyHex: String, data: ByteArray, signature: ByteArray): Boolean {
        if (signature.isEmpty()) return false
        val expected = Sha256Digest.digest(webSeed + data)
        return signature.contentEquals(expected)
    }
}
