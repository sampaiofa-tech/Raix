package com.example.security.notification

/**
 * Multiplatform Push and Local Notification Manager (v1.7).
 *
 * Supported Targets:
 * - Android: Firebase Cloud Messaging (FCM) + Android NotificationManager.
 * - Desktop (JVM/Windows): Compose mini-window (alwaysOnTop, auto-dismiss).
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

    /**
     * Checks if a notification was clicked and returns its messageId (or contactFingerprint),
     * consuming the event. Returns null if no notification was clicked.
     */
    fun getClickedNotificationMessageId(): String?
}
