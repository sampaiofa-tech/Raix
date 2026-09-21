package com.example.security.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Desktop notification data, observed by the mini-window in main.kt.
 */
data class DesktopNotificationData(
    val title: String,
    val body: String,
    val contactFingerprint: String?
)

actual object PushNotificationManager {

    actual fun getPushToken(): String? = null

    /**
     * Compose snapshot state: when non-null, main.kt renders a mini-window notification.
     * Written by showLocalNotification(), cleared by the mini-window on dismiss/click.
     */
    var pendingNotification by mutableStateOf<DesktopNotificationData?>(null)

    /**
     * Set by the mini-window click handler in main.kt.
     * Consumed once by getClickedNotificationMessageId() in App.kt's poller.
     */
    @Volatile
    var pendingClickedMessageId: String? = null

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        println("[NOTIF] showLocalNotification: title=$title, messageId=$messageId")
        pendingNotification = DesktopNotificationData(title, body, messageId)
    }

    actual fun hasPermission(): Boolean = true

    actual fun getClickedNotificationMessageId(): String? {
        val id = pendingClickedMessageId
        if (id != null) {
            pendingClickedMessageId = null
            println("[NOTIF] getClickedNotificationMessageId consumido: $id")
            return id
        }
        return null
    }
}
