package com.example.service

import android.util.Log
import com.example.util.NotificationHelper
import com.example.util.PushDiagnostics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Raix Firebase Cloud Messaging Service (v1.9.6).
 *
 * Pipeline de notificacao em segundo plano:
 * 1. FCM acorda o app (onMessageReceived).
 * 2. Enfileira SyncMessageWorker (fetch + descriptografia real em background).
 * 3. Dispara notificacao imediata zero-knowledge (conteudo generico).
 * 4. Quando o app volta ao foreground, as mensagens ja estao no InMemoryMessageCache.
 *
 * Canal UNICO de notificacao: somente este service dispara notificacoes.
 * O polling (E2EMessageListenerEffect) NAO notifica para evitar duplicacao.
 */
class RaixFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Raix FCM Token refreshed: ${token.take(8)}...")
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
        PushDiagnostics.updateFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "[DIAGNOSTICO] FCM Push recebido. from=${remoteMessage.from}")

        val senderId = remoteMessage.data["senderId"] ?: remoteMessage.data["messageId"] ?: "unknown"
        Log.d(TAG, "[DIAGNOSTICO] senderId=$senderId")
        PushDiagnostics.markPushReceived(senderId)

        // 1. Enfileirar fetch real em background (SyncMessageWorker faz descriptografia)
        com.example.data.worker.SyncMessageWorker.enqueueSync(this)
        Log.d(TAG, "[DIAGNOSTICO] SyncMessageWorker enfileirado")

        // 2. Notificacao imediata (zero-knowledge, conteudo generico)
        // Sem guard de permissao: showPushNotification usa NotificationManager direto,
        // que funciona em qualquer contexto (Service, Worker, BroadcastReceiver).
        NotificationHelper.showPushNotification(
            context = this,
            title = "Raix",
            body = "Nova mensagem criptografada",
            roomId = senderId
        )
        Log.d(TAG, "[DIAGNOSTICO] Notificacao disparada para roomId=$senderId")
        PushDiagnostics.markNotificationFired("FCM")
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

