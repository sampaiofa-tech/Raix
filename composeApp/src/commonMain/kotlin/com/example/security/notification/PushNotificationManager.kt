package com.example.security.notification

/**
 * Multiplatform Push and Local Notification Manager (v1.6).
 *
 * Supported Targets:
 * - Android: Firebase Cloud Messaging (FCM) + Android NotificationManager.
 * - Desktop (JVM/Windows): Native Windows Toast via PowerShell/WinRT with SystemTray fallback.
 * - iOS: APNs stub (ready for Apple Developer account activation).
 * - Web (Wasm): Session-based notification interface.
 */
expect object PushNotificationManager {
    /**
     * Retrieves the platform push token (FCM token on Android, APNs token on iOS).
     * Returns null on platforms without remote push services (Desktop/Web).
     */
    fun getPushToken(): String?

    /**
     * Dispatches a local high-priority notification to the system tray or notification center.
     */
    fun showLocalNotification(title: String, body: String, messageId: String? = null)

    /**
     * Checks if notification permission is currently granted.
     */
    fun hasPermission(): Boolean
}
