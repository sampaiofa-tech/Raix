package com.example.security.notification

import com.example.service.RaixFirebaseMessagingService
import com.example.util.AndroidContextHolder
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging

actual object PushNotificationManager {

    actual fun getPushToken(): String? {
        val context = AndroidContextHolder.appContext ?: return null
        val saved = RaixFirebaseMessagingService.getSavedToken(context)
        if (!saved.isNullOrBlank()) return saved

        return try {
            FirebaseMessaging.getInstance().token.result
        } catch (_: Throwable) {
            null
        }
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        val context = AndroidContextHolder.appContext ?: return
        if (!hasPermission()) return

        NotificationHelper.showPushNotification(
            context = context,
            title = title,
            body = body,
            roomId = messageId ?: "direct_msg"
        )
    }

    actual fun hasPermission(): Boolean {
        val context = AndroidContextHolder.appContext ?: return false
        return NotificationHelper.hasNotificationPermission(context)
    }
}
