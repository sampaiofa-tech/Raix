package com.example.security.forensic

import kotlin.test.*

class CustodyChainTest {

    @BeforeTest
    fun setUp() {
        LocalAuditTrail.clearForTesting()
    }

    @Test
    fun testGenesisBlockInitialization() {
        val trail = LocalAuditTrail.getAuditTrail()

        assertTrue(trail.isNotEmpty(), "A cadeia de custódia deve inicializar com pelo menos o Bloco Gênese")
        val genesis = trail.first()

        assertEquals(0L, genesis.index, "O índice do bloco gênese deve ser exatamente 0")
        assertEquals(CustodyEntry.GENESIS_PREVIOUS_HASH, genesis.previousHash, "O previousHash do gênese deve ser 64 zeros")
        assertEquals(SecurityEventType.GENESIS_BLOCK, genesis.eventType)
        assertTrue(genesis.entryHash.isNotEmpty(), "O hash do elo não pode ser vazio")
        assertTrue(genesis.signature.isNotEmpty(), "A assinatura de hardware do gênese não pode ser vazia")
        assertTrue(genesis.signerPublicKey.isNotEmpty(), "A chave pública do assinador deve estar presente")
    }

    @Test
    fun testSequentialHashChainingAndEventRecording() {
        val event1 = LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.ROOT_OR_JAILBREAK_DETECTED,
            payload = "Detectado binário su em /system/xbin/su"
        )
        val event2 = LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.INTEGRITY_TAMPER_DETECTED,
            payload = "Assinatura do APK diverge do certificado canônico Raix"
        )
        val event3 = LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.SCREENSHOT_OR_RECORD_ATTEMPT,
            payload = "Tentativa de captura de tela bloqueada por FLAG_SECURE"
        )

        val trail = LocalAuditTrail.getAuditTrail()
        assertEquals(4, trail.size, "A cadeia deve conter 4 elos: Gênese + 3 eventos registrados")

        assertEquals(1L, event1.index)
        assertEquals(2L, event2.index)
        assertEquals(3L, event3.index)

        // Validação de encadeamento estrito (hash chaining)
        assertEquals(trail[0].entryHash, trail[1].previousHash)
        assertEquals(trail[1].entryHash, trail[2].previousHash)
        assertEquals(trail[2].entryHash, trail[3].previousHash)

        // Validação de auto-consistência dos hashes
        for (entry in trail) {
            assertEquals(entry.computeExpectedHash(), entry.entryHash)
        }
    }

    @Test
    fun testIntegrityVerificationPassesOnUnmodifiedChain() {
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.AUTHENTICATION_FAILURE,
            payload = "3 falhas consecutivas de PIN local"
        )
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.PANIC_TRIGGERED,
            payload = "Gesto de pânico acionado pelo usuário"
        )

        val result = LocalAuditTrail.verifyChainIntegrity()

        assertTrue(result.isValid, "A cadeia original e íntegra deve ser 100% válida")
        assertEquals(3, result.totalEntries)
        assertNull(result.failureIndex, "Não deve haver elo com falha em cadeia íntegra")
        assertNull(result.failureReason, "Não deve haver motivo de falha em cadeia íntegra")
    }

    @Test
    fun testTamperedPayloadDetectedImmediately() {
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.DEBUGGER_OR_HOOK_DETECTED,
            payload = "Frida runtime agent detectado em memória"
        )
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.CLIPBOARD_SENSITIVE_LEAK,
            payload = "Tentativa de cópia de mnemônico para área de transferência bloqueada"
        )

        val originalTrail = LocalAuditTrail.getAuditTrail()

        // Simula ataque adversário: adultera o payload do elo 1 tentando ocultar o alerta do Frida
        val tamperedTrail = originalTrail.map { entry ->
            if (entry.index == 1L) {
                entry.copy(eventPayload = "Dispositivo seguro sem alterações")
            } else {
                entry
            }
        }

        val result = LocalAuditTrail.verifyChainIntegrity(tamperedTrail)

        assertFalse(result.isValid, "A verificação pericial deve reprovar cadeia com payload adulterado")
        assertEquals(1L, result.failureIndex, "O elo 1 deve ser apontado exatamente como o ponto de adulteração")
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("Adulteração detectada no elo 1"))
    }

    @Test
    fun testBrokenHashChainDetectedImmediately() {
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.HARDWARE_KEY_EVENT,
            payload = "Chave mestra de custódia gerada no silício StrongBox"
        )
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.IDENTITY_ROTATION_OR_RECOVERY,
            payload = "Rotação de chaves efêmeras concluída"
        )

        val originalTrail = LocalAuditTrail.getAuditTrail()

        // Simula ataque de substituição de bloco: altera previousHash do elo 2
        val tamperedTrail = originalTrail.map { entry ->
            if (entry.index == 2L) {
                entry.copy(previousHash = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            } else {
                entry
            }
        }

        val result = LocalAuditTrail.verifyChainIntegrity(tamperedTrail)

        assertFalse(result.isValid, "A verificação pericial deve reprovar quebra de encadeamento")
        assertEquals(2L, result.failureIndex, "O rompimento deve ser detectado no elo 2")
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("Rompimento de cadeia no elo 2"))
    }

    @Test
    fun testForgedSignatureDetectedImmediately() {
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.CUSTOM_FORENSIC_EVIDENCE,
            payload = "Evidência de integridade coletada para auditoria externa"
        )

        val originalTrail = LocalAuditTrail.getAuditTrail()

        // Simula assinatura forjada ou corrompida no elo 1
        val tamperedTrail = originalTrail.map { entry ->
            if (entry.index == 1L) {
                val forgedSig = if (entry.signature.endsWith("0")) {
                    entry.signature.dropLast(1) + "1"
                } else {
                    entry.signature.dropLast(1) + "0"
                }
                entry.copy(signature = forgedSig)
            } else {
                entry
            }
        }

        val result = LocalAuditTrail.verifyChainIntegrity(tamperedTrail)

        assertFalse(result.isValid, "A verificação pericial deve reprovar assinatura adulterada")
        assertEquals(1L, result.failureIndex, "A falha de assinatura deve ser reportada no elo 1")
        assertNotNull(result.failureReason)
        assertTrue(result.failureReason!!.contains("Assinatura digital inválida ou forjada no elo 1"))
    }

    @Test
    fun testForensicReportExport() {
        LocalAuditTrail.recordSecurityEvent(
            type = SecurityEventType.INTEGRITY_TAMPER_DETECTED,
            payload = "Tentativa de injeção de hook detectada"
        )

        val report = LocalAuditTrail.exportForensicReport()

        assertTrue(report.contains("LAUDO PERICIAL FORENSE DE CADEIA DE CUSTÓDIA DIGITAL"))
        assertTrue(report.contains("Dispositivo PK"))
        assertTrue(report.contains("ÍNDICE ÍNTEGRO"))
        assertTrue(report.contains("CERTIFICAÇÃO CRIPTOGRÁFICA"))
        assertTrue(report.contains("Tentativa de injeção de hook detectada"))
    }

    @Test
    fun testGovernanceStatusLockedForCounterIntelligence() {
        // Validação estrita de governança RFI
        assertEquals(
            RaixForensicIntelligence.GovernanceStatus.AUTHORIZED_ACTIVE,
            RaixForensicIntelligence.CUSTODY_CHAIN_STATUS,
            "Cadeia de custódia deve estar formalmente autorizada e ativa"
        )
        assertEquals(
            RaixForensicIntelligence.GovernanceStatus.LOCKED_PENDING_LEGAL_REVIEW,
            RaixForensicIntelligence.COUNTER_INTELLIGENCE_STATUS,
            "Contra-inteligência deve permanecer estritamente travada sob revisão jurídica"
        )
        assertEquals(
            RaixForensicIntelligence.GovernanceStatus.LOCKED_PENDING_LEGAL_REVIEW,
            RaixForensicIntelligence.MIRROR_SANDBOX_STATUS,
            "Mirror Sandbox deve estar bloqueado"
        )
        assertEquals(
            RaixForensicIntelligence.GovernanceStatus.LOCKED_PENDING_LEGAL_REVIEW,
            RaixForensicIntelligence.HONEY_VAULTS_STATUS,
            "Honey-Vaults deve estar bloqueado"
        )
        assertEquals(
            RaixForensicIntelligence.GovernanceStatus.LOCKED_PENDING_LEGAL_REVIEW,
            RaixForensicIntelligence.OFFENSIVE_METADATA_COLLECTION_STATUS,
            "Coleta ofensiva de metadados deve estar bloqueada"
        )
    }
}
