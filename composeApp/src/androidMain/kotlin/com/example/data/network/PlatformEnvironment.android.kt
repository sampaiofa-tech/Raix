package com.example.data.network

import com.example.BuildConfig

actual object PlatformEnvironment {
    actual val isDebug: Boolean = BuildConfig.DEBUG
    actual val webApiKey: String = BuildConfig.FIREBASE_ANDROID_WEB_API_KEY

    // Android: No environment variable access (prevents runtime spoofing)
    actual fun getEnv(name: String): String? = null

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
