package com.example.data.network

import com.example.BuildConfig
import android.content.pm.PackageManager
import android.os.Build
import com.example.util.AndroidContextHolder
import java.security.MessageDigest

actual object PlatformEnvironment {
    actual val isDebug: Boolean = BuildConfig.DEBUG
    actual val webApiKey: String = BuildConfig.FIREBASE_ANDROID_WEB_API_KEY

    // Android: No environment variable access (prevents runtime spoofing)
    actual fun getEnv(name: String): String? = null

    actual fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual fun getRestHeaders(): Map<String, String> {
        val context = AndroidContextHolder.appContext ?: return emptyMap()
        try {
            val packageName = context.packageName
            val packageManager = context.packageManager
            val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            } ?: return emptyMap()
            
            val md = MessageDigest.getInstance("SHA-1")
            val sha1Hex = md.digest(certBytes).joinToString("") { "%02X".format(it) }
            
            return mapOf(
                "X-Android-Package" to packageName,
                "X-Android-Cert" to sha1Hex
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        }
    }
}
