package com.example.security.integrity

import java.lang.management.ManagementFactory

actual object RuntimeIntegrityVerifier {
    actual fun verifyIntegrity(): RuntimeIntegrityReport {
        val threats = mutableListOf<String>()

        // 1. Inspect JVM launch arguments for injected instrumentation agents or debuggers
        val runtimeMxBean = ManagementFactory.getRuntimeMXBean()
        val inputArguments = runtimeMxBean.inputArguments ?: emptyList()

        val hasJavaAgent = inputArguments.any { it.startsWith("-javaagent:") }
        if (hasJavaAgent) {
            threats.add("INSTRUMENTATION_AGENT_DETECTED: Injeção de agente Java (-javaagent) detectada no processo")
        }

        val hasJdwpDebugger = inputArguments.any { it.contains("-agentlib:jdwp") || it.contains("-Xrunjdwp") || it.contains("-Xdebug") }
        if (hasJdwpDebugger) {
            threats.add("DEBUGGER_ATTACHED: Protocolo JDWP de depuração ativo na JVM")
        }

        // 2. Verify security classloader integrity
        try {
            val classLoader = RuntimeIntegrityVerifier::class.java.classLoader
            if (classLoader != null && classLoader::class.java.name.contains("Agent")) {
                threats.add("MODIFIED_CLASSLOADER: ClassLoader adulterado por ferramenta de instrumentação")
            }
        } catch (_: Throwable) {}

        val isSecure = threats.isEmpty()
        val details = if (isSecure) {
            "Runtime Desktop JVM íntegro (Zero agentes ou depuradores detectados)"
        } else {
            "Violação de integridade detectada: ${threats.joinToString("; ")}"
        }

        return RuntimeIntegrityReport(
            isSecure = isSecure,
            threatsDetected = threats,
            details = details,
            platformLabel = "Desktop (JVM)"
        )
    }
}
