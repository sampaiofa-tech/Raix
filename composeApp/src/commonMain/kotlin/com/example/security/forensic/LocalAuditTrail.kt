package com.example.security.forensic

import com.example.data.network.PlatformEnvironment
import com.example.security.identity.Sha256Digest

/**
 * Trilho de Auditoria Local com Cadeia de Custódia Forense (RFI).
 *
 * Implementa o ciclo de vida:
 * Evento de Segurança ──> Captura de Log ──> Assinatura de Hardware ──> Hash Chaining ──> Prova Judicial
 */
object LocalAuditTrail {

    private val lock = Any()

    init {
        ensureGenesisBlock()
    }

    /**
     * Garante a presença determinística do Bloco Gênese (Índice 0) na inicialização da cadeia.
     */
    private fun ensureGenesisBlock() {
        synchronized(lock) {
            val chain = CustodyStorage.loadChain()
            if (chain.isEmpty()) {
                val index = 0L
                val timestamp = PlatformEnvironment.currentTimeMillis()
                val eventType = SecurityEventType.GENESIS_BLOCK
                val payload = "RAIX Forensic Intelligence (RFI) - Genesis Block Initialized"
                val previousHash = CustodyEntry.GENESIS_PREVIOUS_HASH
                val canonical = "$index|$timestamp|${eventType.name}|$payload|$previousHash"
                val entryHash = Sha256Digest.digestHex(canonical.encodeToByteArray())

                val sigBytes = CustodyHardwareSigner.sign(entryHash.encodeToByteArray())
                val signatureHex = bytesToHex(sigBytes)
                val publicKeyHex = CustodyHardwareSigner.getPublicKeyHex()
                val isHw = CustodyHardwareSigner.isHardwareBacked()

                val genesisEntry = CustodyEntry(
                    index = index,
                    timestamp = timestamp,
                    eventType = eventType,
                    eventPayload = payload,
                    previousHash = previousHash,
                    entryHash = entryHash,
                    signature = signatureHex,
                    signerPublicKey = publicKeyHex,
                    isHardwareBacked = isHw
                )
                CustodyStorage.appendEntry(genesisEntry)
            }
        }
    }

    /**
     * Retorna a lista completa e ordenada de todas as evidências registradas no trilho.
     */
    fun getAuditTrail(): List<CustodyEntry> {
        ensureGenesisBlock()
        return CustodyStorage.loadChain()
    }

    /**
     * Registra um novo evento de segurança na cadeia de custódia com hash chaining e assinatura.
     */
    fun recordSecurityEvent(
        type: SecurityEventType,
        payload: String,
        timestamp: Long = PlatformEnvironment.currentTimeMillis()
    ): CustodyEntry {
        return synchronized(lock) {
            val currentChain = getAuditTrail()
            val lastEntry = currentChain.last()

            val newIndex = lastEntry.index + 1
            val previousHash = lastEntry.entryHash
            val canonical = "$newIndex|$timestamp|${type.name}|$payload|$previousHash"
            val entryHash = Sha256Digest.digestHex(canonical.encodeToByteArray())

            val sigBytes = CustodyHardwareSigner.sign(entryHash.encodeToByteArray())
            val signatureHex = bytesToHex(sigBytes)
            val publicKeyHex = CustodyHardwareSigner.getPublicKeyHex()
            val isHw = CustodyHardwareSigner.isHardwareBacked()

            val newEntry = CustodyEntry(
                index = newIndex,
                timestamp = timestamp,
                eventType = type,
                eventPayload = payload,
                previousHash = previousHash,
                entryHash = entryHash,
                signature = signatureHex,
                signerPublicKey = publicKeyHex,
                isHardwareBacked = isHw
            )

            CustodyStorage.appendEntry(newEntry)
            newEntry
        }
    }

    /**
     * Executa a auditoria matemática completa e independente de todos os elos da cadeia.
     */
    fun verifyChainIntegrity(chain: List<CustodyEntry> = getAuditTrail()): CustodyVerificationResult {
        if (chain.isEmpty()) {
            return CustodyVerificationResult(
                isValid = true,
                totalEntries = 0,
                verifiedEntries = emptyList()
            )
        }

        // Validação do Bloco Gênese
        val genesis = chain.first()
        if (genesis.index != 0L) {
            return CustodyVerificationResult(
                isValid = false,
                totalEntries = chain.size,
                failureIndex = genesis.index,
                failureReason = "Bloco gênese corrompido: índice esperado 0, encontrado ${genesis.index}"
            )
        }
        if (genesis.previousHash != CustodyEntry.GENESIS_PREVIOUS_HASH) {
            return CustodyVerificationResult(
                isValid = false,
                totalEntries = chain.size,
                failureIndex = genesis.index,
                failureReason = "Bloco gênese corrompido: previousHash inválido (${genesis.previousHash})"
            )
        }

        for (i in chain.indices) {
            val entry = chain[i]

            // 1. Verifica o encadeamento contínuo com o elo anterior
            if (i > 0) {
                val prior = chain[i - 1]
                if (entry.previousHash != prior.entryHash) {
                    return CustodyVerificationResult(
                        isValid = false,
                        totalEntries = chain.size,
                        failureIndex = entry.index,
                        failureReason = "Rompimento de cadeia no elo ${entry.index}: previousHash não aponta para o entryHash do elo anterior (${prior.entryHash})"
                    )
                }
                if (entry.index != prior.index + 1) {
                    return CustodyVerificationResult(
                        isValid = false,
                        totalEntries = chain.size,
                        failureIndex = entry.index,
                        failureReason = "Salto de sequência no elo ${entry.index}: esperado índice ${prior.index + 1}"
                    )
                }
            }

            // 2. Recalcula deterministicamente o hash do bloco
            val expectedHash = entry.computeExpectedHash()
            if (entry.entryHash != expectedHash) {
                return CustodyVerificationResult(
                    isValid = false,
                    totalEntries = chain.size,
                    failureIndex = entry.index,
                    failureReason = "Adulteração detectada no elo ${entry.index}: hash registrado (${entry.entryHash}) diverge do hash recalculado ($expectedHash)"
                )
            }

            // 3. Verifica a assinatura criptográfica ancorada no hardware
            val sigBytes = try {
                hexToBytes(entry.signature)
            } catch (_: Exception) {
                return CustodyVerificationResult(
                    isValid = false,
                    totalEntries = chain.size,
                    failureIndex = entry.index,
                    failureReason = "Assinatura em formato hexadecimal inválido no elo ${entry.index}"
                )
            }

            val isSignatureValid = CustodyHardwareSigner.verify(
                publicKeyHex = entry.signerPublicKey,
                data = entry.entryHash.encodeToByteArray(),
                signature = sigBytes
            )

            if (!isSignatureValid) {
                return CustodyVerificationResult(
                    isValid = false,
                    totalEntries = chain.size,
                    failureIndex = entry.index,
                    failureReason = "Assinatura digital inválida ou forjada no elo ${entry.index}"
                )
            }
        }

        return CustodyVerificationResult(
            isValid = true,
            totalEntries = chain.size,
            verifiedEntries = chain
        )
    }

    /**
     * Emite o laudo pericial formal e estruturado para utilização como prova inalterável em juízo.
     */
    fun exportForensicReport(): String {
        val auditResult = verifyChainIntegrity()
        val chain = getAuditTrail()
        val sb = StringBuilder()

        sb.appendLine("================================================================================")
        sb.appendLine("       LAUDO PERICIAL FORENSE DE CADEIA DE CUSTÓDIA DIGITAL (RAIX RFI)         ")
        sb.appendLine("================================================================================")
        sb.appendLine("Data de Emissão : ${PlatformEnvironment.currentTimeMillis()} (Epoch ms)")
        sb.appendLine("Repositório     : sampaiofa-tech/Raix")
        sb.appendLine("Dispositivo PK  : ${CustodyHardwareSigner.getPublicKeyHex()}")
        sb.appendLine("Hardware Silício: ${if (CustodyHardwareSigner.isHardwareBacked()) "SIM (StrongBox / Secure Enclave)" else "NÃO (Software CSP / DPAPI)"}")
        sb.appendLine("Status Cadeia   : ${if (auditResult.isValid) "ÍNDICE ÍNTEGRO (100% AUDITADO E VÁLIDO)" else "COMPROMETIDA / ADULTERADA"}")
        sb.appendLine("Total de Elos   : ${auditResult.totalEntries}")
        if (!auditResult.isValid) {
            sb.appendLine("Elo da Falha    : ${auditResult.failureIndex}")
            sb.appendLine("Motivo da Falha : ${auditResult.failureReason}")
        }
        sb.appendLine("--------------------------------------------------------------------------------")
        sb.appendLine("REGISTRO DETALHADO DOS ELOS DE CUSTÓDIA:")
        sb.appendLine("--------------------------------------------------------------------------------")

        for (entry in chain) {
            sb.appendLine("[ELO #${entry.index}]")
            sb.appendLine("  Timestamp     : ${entry.timestamp}")
            sb.appendLine("  Evento        : ${entry.eventType.name} (${entry.eventType.title})")
            sb.appendLine("  Severidade    : ${entry.eventType.severity}")
            sb.appendLine("  Payload       : ${entry.eventPayload}")
            sb.appendLine("  PreviousHash  : ${entry.previousHash}")
            sb.appendLine("  EntryHash     : ${entry.entryHash}")
            sb.appendLine("  Assinatura    : ${entry.signature}")
            sb.appendLine("  Silício Seguro: ${entry.isHardwareBacked}")
            sb.appendLine()
        }

        sb.appendLine("================================================================================")
        sb.appendLine("CERTIFICAÇÃO CRIPTOGRÁFICA DE INALTERABILIDADE:")
        sb.appendLine("Este documento constitui extrato forense assinado criptograficamente.")
        sb.appendLine("Qualquer alteração de texto, metadados ou ordem rompe irrevogavelmente os")
        sb.appendLine("hashes SHA-256 encadeados e invalida as assinaturas digitais do hardware.")
        sb.appendLine("================================================================================")

        return sb.toString()
    }

    /**
     * Utilitário reservado para suítes de testes unitários herméticos.
     */
    fun clearForTesting() {
        synchronized(lock) {
            CustodyStorage.clearChain()
            ensureGenesisBlock()
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
