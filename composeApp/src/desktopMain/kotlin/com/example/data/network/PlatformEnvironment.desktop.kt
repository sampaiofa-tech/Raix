package com.example.data.network

import com.example.DesktopBuildConfig

actual object PlatformEnvironment {
    // In production releases, run with -Dpmsg.debug=false or packaged native distribution
    actual val isDebug: Boolean = System.getProperty("pmsg.debug", "true") == "true"
    actual val webApiKey: String = DesktopBuildConfig.FIREBASE_DESKTOP_WEB_API_KEY

    // ANTI-SPOOFING: Environment variables are strictly ignored in release mode
    actual fun getEnv(name: String): String? {
        return if (isDebug) {
            System.getenv(name)
        } else {
            null
        }
    }

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun getRestHeaders(): Map<String, String> = emptyMap()
}
