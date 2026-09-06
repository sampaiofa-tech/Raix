package com.example.security.integrity

actual object RuntimeIntegrityVerifier {
    actual fun verifyIntegrity(): RuntimeIntegrityReport {
        val threats = mutableListOf<String>()

        // Web (WasmJs) integrity posture
        // Web environments operate under browser sandbox, but cannot guarantee host memory immunity against DevTools inspection
        threats.add("WEB_ENVIRONMENT_ADVISORY: Execução em navegador Web sujeita a inspeção de contexto/DevTools")

        return RuntimeIntegrityReport(
            isSecure = true, // Operação permitida com alerta
            threatsDetected = threats,
            details = "Ambiente Web/WasmJs operacional sob sandbox do navegador",
            platformLabel = "Web (WasmJs)"
        )
    }
}
