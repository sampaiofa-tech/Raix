package com.example.security.notification

/**
 * iOS Push and Local Notification Manager (v1.6).
 *
 * NOTE: Remote APNs push notifications require an active Apple Developer account
 * and valid APNs signing certificates/keys.
 * This implementation provides the registration hooks, token handling, and local
 * notification stubs ready for immediate activation once the Apple Developer credentials arrive.
 */
actual object PushNotificationManager {

    private var cachedApnsToken: String? = null

    actual fun getPushToken(): String? {
        return cachedApnsToken
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        // Will be connected to UNUserNotificationCenter upon Apple Developer provisioning
    }

    actual fun hasPermission(): Boolean {
        return false
    }

    fun setApnsDeviceToken(tokenHex: String) {
        cachedApnsToken = tokenHex
    }
}
