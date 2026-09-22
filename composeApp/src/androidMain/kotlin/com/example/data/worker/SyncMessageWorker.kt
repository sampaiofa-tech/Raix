package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.network.FirestoreRestClient
import com.example.data.network.KeyStoreClient
import com.example.data.network.PlatformEnvironment
import com.example.security.DeviceAuthManager
import com.example.security.identity.AesGcm
import com.example.security.identity.IdentityManager
import com.example.security.identity.SealedBox
import com.example.security.identity.SealedBoxEnvelope
import com.example.ui.screens.EphemeralUiMessage
import com.example.ui.screens.InMemoryMessageCache
import com.example.util.NotificationHelper
import com.example.util.PushDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Worker responsavel pelo fetch e descriptografia de mensagens em segundo plano.
 *
 * Acionado pelo RaixFirebaseMessagingService.onMessageReceived quando um push FCM chega.
 * Executa o mesmo pipeline do polling (E2EMessageListenerEffect), mas SEM depender
 * da UI composta -- funciona com o app minimizado ou em background.
 *
 * Fluxo:
 * 1. Obter token de autenticacao (DeviceAuthManager)
 * 2. Buscar mensagens pendentes (FirestoreRestClient)
 * 3. Para cada mensagem: obter chave efemera (KeyStoreClient), abrir SealedBox, descriptografar AES-GCM
 * 4. Salvar no InMemoryMessageCache e deletar do servidor
 * 5. Notificacao disparada pelo chamador (RaixFirebaseMessagingService) -- aqui NAO notifica
 */
@OptIn(ExperimentalEncodingApi::class)
class SyncMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "SyncMessageWorker iniciado. Buscando mensagens pendentes...")

            val authManager = DeviceAuthManager
            val myToken = authManager.getIdToken()
            val myUid = authManager.getUserId()

            if (myToken == null || myUid == null) {
                Log.d(TAG, "Sem autenticacao disponivel. Abortando fetch.")
                return@withContext Result.success()
            }

            val myIdentity = IdentityManager.getIdentity()
            val myPrivKey = myIdentity?.privateKey
            if (myIdentity == null || myPrivKey == null) {
                Log.d(TAG, "Sem identidade disponivel. Abortando fetch.")
                return@withContext Result.success()
            }

            val pendingResult = FirestoreRestClient.fetchPendingMessages(myUid, myToken)
            if (pendingResult.isFailure) {
                Log.d(TAG, "Falha ao buscar mensagens: ${pendingResult.exceptionOrNull()?.message}")
                return@withContext Result.retry()
            }

            val pending = pendingResult.getOrThrow()
            Log.d(TAG, "Mensagens pendentes encontradas: ${pending.size}")

            var processedCount = 0
            for (msg in pending) {
                try {
                    val keyResult = KeyStoreClient.getMessageKey(msg.id, myToken)
                    if (!keyResult.success || keyResult.ephemeralPubKey == null || keyResult.wrappedDek == null) {
                        continue
                    }

                    val env = SealedBoxEnvelope(
                        ephemeralPubKeyHex = keyResult.ephemeralPubKey,
                        wrappedDekBase64 = keyResult.wrappedDek,
                        mlKemCiphertextBase64 = keyResult.mlKemCiphertextBase64
                    )
                    val dek = SealedBox.unseal(
                        envelope = env,
                        recipientPrivKey = myPrivKey,
                        recipientMlKemPrivKey = myIdentity.mlKemPrivateKey,
                        enforceHybrid = myIdentity.mlKemPrivateKey.isNotEmpty()
                    )

                    val cipherBytes = Base64.decode(msg.ciphertext)
                    val ivBytes = Base64.decode(msg.iv)
                    val decryptedBytes = AesGcm.decrypt(cipherBytes, dek, ivBytes)
                    val decryptedText = decryptedBytes.decodeToString()

                    // Auto-handshake: processar silenciosamente
                    if (decryptedText.startsWith("[AUTO-HANDSHAKE] ")) {
                        FirestoreRestClient.deleteMessage(msg.id, myToken)
                        Log.d(TAG, "Auto-handshake processado em background (msg ${msg.id.take(8)})")
                        processedCount++
                        continue
                    }

                    // Mensagem normal: cachear e deletar do servidor
                    val contactFingerprint = msg.senderId
                    val now = PlatformEnvironment.currentTimeMillis()
                    val remainingTtl = (msg.expiresAt - now).coerceAtLeast(10_000L)
                    val ephemeralMsg = EphemeralUiMessage(
                        id = msg.id,
                        senderId = contactFingerprint,
                        senderName = "Contato",
                        isMe = false,
                        text = decryptedText,
                        timestamp = now,
                        ttlMillis = remainingTtl,
                        expiresAt = msg.expiresAt
                    )
                    val cacheList = InMemoryMessageCache.getMessages(contactFingerprint)
                    if (cacheList.none { it.id == msg.id }) {
                        cacheList.add(ephemeralMsg)
                        InMemoryMessageCache.saveMessages(contactFingerprint, cacheList)
                        FirestoreRestClient.deleteMessage(msg.id, myToken)
                        processedCount++
                        // Notificar em background: usa o mesmo roomId (contactFingerprint)
                        // que o FCM, portanto o Android substitui (mesma ID de notificacao).
                        NotificationHelper.showPushNotification(
                            context = applicationContext,
                            title = "Raix",
                            body = "Nova mensagem criptografada",
                            roomId = contactFingerprint
                        )
                        Log.d(TAG, "Mensagem descriptografada, cacheada e notificada em background (msg ${msg.id.take(8)})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar mensagem ${msg.id.take(8)}: ${e.message}")
                }
            }

            Log.d(TAG, "SyncMessageWorker concluido. $processedCount mensagens processadas.")
            PushDiagnostics.markWorkerRun(processedCount)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erro no SyncMessageWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SyncMessageWorker"

        fun enqueueSync(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<SyncMessageWorker>().build()
            WorkManager.getInstance(context).enqueue(syncRequest)
            Log.d(TAG, "SyncMessageWorker enfileirado.")
        }
    }
}
