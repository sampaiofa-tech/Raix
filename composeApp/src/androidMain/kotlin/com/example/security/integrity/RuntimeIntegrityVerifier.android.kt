package com.example.security.integrity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import java.io.File

actual object RuntimeIntegrityVerifier {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    actual fun verifyIntegrity(): RuntimeIntegrityReport {
        val threats = mutableListOf<String>()

        // 1. Root & SU binary detection
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/su/bin/su"
        )
        val hasRootBinary = rootPaths.any { path ->
            try { File(path).exists() } catch (_: Throwable) { false }
        }
        if (hasRootBinary) {
            threats.add("ROOT_PRIVILEGES_DETECTED: Binários de superusuário (su) detectados")
        }

        // 2. Frida & Dynamic Hooking Framework detection via /proc/self/maps
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists()) {
                val mapsContent = mapsFile.readText()
                if (mapsContent.contains("frida-agent") || mapsContent.contains("frida-gadget") || mapsContent.contains("xposed")) {
                    threats.add("DYNAMIC_HOOK_FRAMEWORK: Assinaturas de Frida ou Xposed detectadas na memória do processo")
                }
            }
        } catch (_: Throwable) {}

        // 3. Debugger & Debuggable APK flags
        val isDebuggerAttached = Debug.isDebuggerConnected() || Debug.waitingForDebugger()
        if (isDebuggerAttached) {
            threats.add("DEBUGGER_ATTACHED: Depurador conectado ativamente à aplicação")
        }

        val ctx = appContext
        if (ctx != null) {
            val isDebuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) {
                threats.add("DEBUGGABLE_BUILD: Pacote APK compilado em modo de depuração")
            }
        }

        val isSecure = threats.isEmpty()
        val details = if (isSecure) {
            "Ambiente Android íntegro (Zero hooks, root ou depuradores detectados)"
        } else {
            "Violações de integridade detectadas: ${threats.joinToString("; ")}"
        }

        return RuntimeIntegrityReport(
            isSecure = isSecure,
            threatsDetected = threats,
            details = details,
            platformLabel = "Android"
        )
    }
}
