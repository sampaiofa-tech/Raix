package com.example.security.integrity

actual object RuntimeIntegrityVerifier {
    actual fun verifyIntegrity(): RuntimeIntegrityReport {
        return RuntimeIntegrityReport(
            isSecure = true,
            threatsDetected = emptyList(),
            details = "Ambiente iOS em sandbox padrão do sistema",
            platformLabel = "iOS"
        )
    }
}
