package com.example.security.notification

/**
 * Web/Wasm implementation of PushNotificationManager (v1.6).
 *
 * Full Web Push (VAPID) is scheduled for Cycle v1.7.
 */
actual object PushNotificationManager {

    actual fun getPushToken(): String? {
        return null
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        // Fallback for Web browser notifications
    }

    actual fun hasPermission(): Boolean {
        return false
    }
}
