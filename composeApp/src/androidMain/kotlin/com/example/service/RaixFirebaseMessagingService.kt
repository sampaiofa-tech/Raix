package com.example.service

import android.util.Log
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Raix Firebase Cloud Messaging Service (v1.6).
 *
 * Implements:
 * 1. Zero-Knowledge Background Delivery: Only alerts the user that an ephemeral message
 *    is waiting on the server. Never parses, routes or handles message plaintext.
 * 2. High-Priority Display: Dispatches native Android Messaging Notifications via NotificationHelper.
 * 3. Token Rotation: Handles onNewToken and persists device push token.
 */
class RaixFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Raix FCM Token refreshed: ${token.take(8)}...")
        // Save token locally for registration upon device authentication
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Raix FCM Push received from: ${remoteMessage.from}")

        // Zero-Knowledge check: never rely on plaintext in push payload
        val type = remoteMessage.data["type"] ?: "new_message"
        val messageId = remoteMessage.data["messageId"] ?: "unknown"

        if (NotificationHelper.hasNotificationPermission(this)) {
            // Trigger high-priority notification with standard privacy-preserving copy
            NotificationHelper.showPushNotification(
                context = this,
                title = "Raix",
                body = "Nova mensagem efêmera recebida.",
                roomId = messageId
            )
        }
    }

    companion object {
        private const val TAG = "RaixFCMService"
        const val PREFS_NAME = "raix_push_prefs"
        const val KEY_FCM_TOKEN = "fcm_token"

        fun getSavedToken(context: android.content.Context): String? {
            return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
        }
    }
}
