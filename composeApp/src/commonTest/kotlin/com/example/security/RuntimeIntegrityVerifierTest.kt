package com.example.security

import com.example.security.integrity.RuntimeIntegrityVerifier
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuntimeIntegrityVerifierTest {

    @Test
    fun testRuntimeIntegrityExecutionDoesNotCrashAndReturnsConsistentReport() {
        val report = RuntimeIntegrityVerifier.verifyIntegrity()

        assertNotNull(report, "O relatório de integridade de runtime não deve ser nulo")
        assertNotNull(report.platformLabel, "O label da plataforma deve ser informado")
        assertNotNull(report.details, "Os detalhes da verificação devem ser preenchidos")
        assertNotNull(report.threatsDetected, "A lista de ameaças detectadas deve ser inicializada")

        assertTrue(
            report.platformLabel.isNotEmpty(),
            "A plataforma em execução deve ser devidamente identificada no relatório"
        )
    }
}
