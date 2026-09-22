package com.example.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.example.data.model.ContactItem
import com.example.data.model.FirestoreMessage
import com.example.data.repository.ContactRepository
import com.example.data.network.FirestoreRestClient
import com.example.data.network.IdentityNetworkClient
import com.example.data.network.KeyStoreClient
import com.example.data.network.PlatformEnvironment
import com.example.security.identity.AesGcm
import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.IdentityManager
import com.example.security.identity.SealedBox
import com.example.security.identity.SealedBoxEnvelope
import com.example.security.notification.PushNotificationManager
import com.example.ui.screens.EphemeralUiMessage
import com.example.ui.screens.InMemoryMessageCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Listener E2E unificado (commonMain).
 * Faz polling de mensagens pendentes a cada 3s, descriptografa via SealedBox,
 * trata auto-handshake e mensagens normais (cache efemero + notificacao local).
 *
 * @param contactRepository repositorio de contatos (local, criptografado)
 * @param onNavigateToContact callback opcional quando usuario clica na notificacao
 */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun E2EMessageListenerEffect(
    contactRepository: ContactRepository,
    onNavigateToContact: (ContactItem) -> Unit = {}
) {
    val notifiedMessages = remember { mutableSetOf<String>() }

    LaunchedEffect(Unit) {
        var pushTokenRegistered = false
        while (true) {
            try {
                val authManager = DeviceAuthManager
                val myToken = authManager.getIdToken()
                val myUid = authManager.getUserId()
                if (myToken != null) {
                    // Registra push token uma vez
                    if (!pushTokenRegistered) {
                        try {
                            val token = PushNotificationManager.getPushToken()
                            if (token != null) {
                                IdentityNetworkClient.registerPushToken(token, "android", myToken)
                                pushTokenRegistered = true
                            }
                        } catch (_: Exception) {}
                    }

                    // Busca mensagens pendentes
                    val pendingResult = FirestoreRestClient.fetchPendingMessages(myUid, myToken)
                    if (pendingResult.isSuccess) {
                        val pending = pendingResult.getOrThrow()
                        for (msg in pending) {
                            val keyResult = KeyStoreClient.getMessageKey(msg.id, myToken)
                            if (keyResult.success && keyResult.ephemeralPubKey != null && keyResult.wrappedDek != null) {
                                val myPrivKey = IdentityManager.getIdentity()?.privateKey
                                if (myPrivKey != null) {
                                    val env = SealedBoxEnvelope(
                                        ephemeralPubKeyHex = keyResult.ephemeralPubKey,
                                        wrappedDekBase64 = keyResult.wrappedDek
                                    )
                                    val dek = SealedBox.unseal(env, myPrivKey)
                                    val cipherBytes = Base64.decode(msg.ciphertext)
                                    val ivBytes = Base64.decode(msg.iv)
                                    val decryptedBytes = AesGcm.decrypt(cipherBytes, dek, ivBytes)
                                    val decryptedText = decryptedBytes.decodeToString()

                                    if (decryptedText.startsWith("[AUTO-HANDSHAKE] ")) {
                                        handleAutoHandshake(decryptedText, contactRepository, msg.id, myToken)
                                    } else {
                                        handleNormalMessage(
                                            msg, decryptedText, contactRepository,
                                            myToken, notifiedMessages
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Verifica clique em notificacao
                    val clickedFingerprint = PushNotificationManager.getClickedNotificationMessageId()
                    if (clickedFingerprint != null) {
                        val allContacts = contactRepository.getContacts().first()
                        val contactToOpen = allContacts.find {
                            it.fingerprint == clickedFingerprint || it.currentAuthUid == clickedFingerprint
                        }
                        if (contactToOpen != null) {
                            onNavigateToContact(contactToOpen)
                        }
                    }
                }
            } catch (e: Exception) {
                println("[DIAGNOSTICO] Listener E2E: ${e.message}")
            }
            delay(3000L)
        }
    }
}

private suspend fun handleAutoHandshake(
    decryptedText: String,
    contactRepository: ContactRepository,
    messageId: String,
    authToken: String
) {
    val uri = decryptedText.substringAfter("[AUTO-HANDSHAKE] ").trim()
    val parseRes = IdentityManager.parseContactUri(uri)
    if (parseRes.isSuccess) {
        val contactData = parseRes.getOrThrow()
        val existing = contactRepository.getContact(contactData.fingerprintHex)
        if (existing == null) {
            val myIdentity = IdentityManager.getIdentity()
            val pairSafetyNumber = IdentityCryptoManager.computePairSafetyNumber(
                myPubKey = myIdentity!!.publicKey,
                peerPubKey = contactData.publicKeyBytes
            )
            val newContact = ContactItem(
                fingerprint = contactData.fingerprintHex,
                pubKey = contactData.publicKeyBase64,
                currentAuthUid = contactData.authUid,
                displayName = "Contato_${contactData.fingerprintHex.take(6)}",
                securityNumber = pairSafetyNumber,
                verified = false,
                addedAt = PlatformEnvironment.currentTimeMillis()
            )
            contactRepository.saveContact(newContact)
        }
        FirestoreRestClient.deleteMessage(messageId, authToken)
    }
}

private suspend fun handleNormalMessage(
    msg: FirestoreMessage,
    decryptedText: String,
    contactRepository: ContactRepository,
    authToken: String,
    notifiedMessages: MutableSet<String>
) {
    val allContacts = contactRepository.getContacts().first()
    val existingContact = allContacts.find {
        it.currentAuthUid == msg.senderId || it.fingerprint == msg.senderId
    }
    val contactFingerprint = existingContact?.fingerprint ?: msg.senderId

    if (existingContact != null && contactRepository.isContactBlocked(contactFingerprint)) {
        contactRepository.recordBlockedPurge(contactFingerprint)
        FirestoreRestClient.deleteMessage(msg.id, authToken)
    } else {
        val now = PlatformEnvironment.currentTimeMillis()
        val remainingTtl = (msg.expiresAt - now).coerceAtLeast(10_000L)
        val ephemeralMsg = EphemeralUiMessage(
            id = msg.id,
            senderId = contactFingerprint,
            senderName = existingContact?.displayName ?: "Desconhecido",
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
            FirestoreRestClient.deleteMessage(msg.id, authToken)
            if (notifiedMessages.add(msg.id)) {
                PushNotificationManager.showLocalNotification(
                    title = "Raix",
                    body = "Nova mensagem criptografada",
                    messageId = contactFingerprint
                )
            }
        }
    }
}
