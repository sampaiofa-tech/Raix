package com.example.security.forensic

import com.example.security.identity.Sha256Digest

/**
 * Catálogo tipado de eventos críticos de segurança para a Cadeia de Custódia Forense (RFI).
 */
enum class SecurityEventType(val title: String, val severity: String) {
    GENESIS_BLOCK("Bloco Gênese da Cadeia de Custódia Local", "INFO"),
    ROOT_OR_JAILBREAK_DETECTED("Deteção de Root / Jailbreak / Quebra de Sandbox", "CRITICAL"),
    DEBUGGER_OR_HOOK_DETECTED("Depurador ou Hooking (Frida/Xposed) Anexado", "CRITICAL"),
    INTEGRITY_TAMPER_DETECTED("Violação de Integridade de Executável / Assinatura", "CRITICAL"),
    HARDWARE_KEY_EVENT("Geração, Rotação ou Invalidação de Chaves de Hardware", "HIGH"),
    SCREENSHOT_OR_RECORD_ATTEMPT("Tentativa de Captura de Tela / Gravação Bloqueada", "MEDIUM"),
    CLIPBOARD_SENSITIVE_LEAK("Tentativa de Exfiltração via Área de Transferência", "HIGH"),
    IDENTITY_ROTATION_OR_RECOVERY("Recuperação de Identidade ou Rotação Criptográfica", "MEDIUM"),
    AUTHENTICATION_FAILURE("Falha Crítica de Autenticação Biométrica / PIN", "HIGH"),
    PANIC_TRIGGERED("Acionamento do Modo de Pânico / Autodestruição Imediata", "CRITICAL"),
    CUSTOM_FORENSIC_EVIDENCE("Registro Pericial Avulso de Evidência", "INFO")
}

/**
 * Registro atômico e imutável de evidência na Cadeia de Custódia Forense (RFI).
 *
 * Cada elo é matematicamente amarrado ao anterior por hash chaining SHA-256 e assinado
 * digitalmente por chave ancorada em hardware (StrongBox/Secure Enclave/TPM).
 */
data class CustodyEntry(
    val index: Long,
    val timestamp: Long,
    val eventType: SecurityEventType,
    val eventPayload: String,
    val previousHash: String,
    val entryHash: String,
    val signature: String,
    val signerPublicKey: String,
    val isHardwareBacked: Boolean
) {
    /**
     * Recalcula deterministicamente o hash do bloco com base nos atributos imutáveis.
     */
    fun computeExpectedHash(): String {
        val canonicalRepresentation = "$index|$timestamp|${eventType.name}|$eventPayload|$previousHash"
        return Sha256Digest.digestHex(canonicalRepresentation.encodeToByteArray())
    }

    /**
     * Serializa a entrada em formato delimitado por tabulação com payload protegido.
     */
    fun toLogLine(): String {
        val hexPayload = bytesToHex(eventPayload.encodeToByteArray())
        return "$index\t$timestamp\t${eventType.name}\t$hexPayload\t$previousHash\t$entryHash\t$signature\t$signerPublicKey\t$isHardwareBacked"
    }

    companion object {
        const val GENESIS_PREVIOUS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        fun fromLogLine(line: String): CustodyEntry? {
            val parts = line.split("\t")
            if (parts.size != 9) return null
            val idx = parts[0].toLongOrNull() ?: return null
            val ts = parts[1].toLongOrNull() ?: return null
            val evType = try { SecurityEventType.valueOf(parts[2]) } catch (_: Exception) { return null }
            val payloadStr = try {
                hexToBytes(parts[3]).decodeToString()
            } catch (_: Exception) {
                return null
            }
            val prevH = parts[4]
            val entH = parts[5]
            val sig = parts[6]
            val pubKey = parts[7]
            val hwBacked = parts[8].toBoolean()
            return CustodyEntry(idx, ts, evType, payloadStr, prevH, entH, sig, pubKey, hwBacked)
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
}

/**
 * Resultado formal de verificação e auditoria matemática da Cadeia de Custódia.
 */
data class CustodyVerificationResult(
    val isValid: Boolean,
    val totalEntries: Int,
    val failureIndex: Long? = null,
    val failureReason: String? = null,
    val verifiedEntries: List<CustodyEntry> = emptyList()
)
