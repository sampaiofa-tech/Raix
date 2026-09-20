package com.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepositoryProvider
import com.example.security.consent.LegalConsentManager
import com.example.security.identity.IdentityManager
import com.example.ui.screens.AddContactModelAScreen
import com.example.ui.screens.AgeGateScreen
import com.example.ui.screens.AppLockGate
import com.example.ui.screens.BlockedContactsScreen
import com.example.ui.screens.ContactChatScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DataPrivacyScreen
import com.example.ui.screens.IdentityScreen
import com.example.ui.screens.RecoverySeedScreen
import com.example.ui.screens.SafetyNumberScreen
import kotlinx.coroutines.delay
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import androidx.compose.runtime.LaunchedEffect

sealed interface AppDestination {
    data object AppLock : AppDestination
    data object AgeGate : AppDestination
    data object RecoverySeed : AppDestination
    data object Contacts : AppDestination
    data class Chat(val contact: ContactItem) : AppDestination
    data object BlockedContacts : AppDestination
    data object Identity : AppDestination
    data object DataPrivacy : AppDestination
    data object AddModelA : AppDestination
    data class SafetyNumber(val contact: ContactItem) : AppDestination
    data object QrHandshake : AppDestination
}

private val RaixDarkColors = darkColorScheme(
    primary = Color(0xFF00E676),
    onPrimary = Color.Black,
    background = Color(0xFF0B1325),
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0xFF1E2432),
    onSurface = Color(0xFFF5F7FA),
    surfaceVariant = Color(0xFF1E2432),
    onSurfaceVariant = Color(0xFF8A93A6),
    secondary = Color(0xFF00E676),
    onSecondary = Color.Black,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    tertiary = Color(0xFFD4AF37),
    onTertiary = Color.Black
)

@Composable
fun App() {
    val contactRepository = remember { ContactRepositoryProvider.get() }
    val isConsentValid = remember { LegalConsentManager.isConsentValid() }
    val hasIdentity = remember { IdentityManager.hasIdentity() }
    var currentDestination by remember {
        mutableStateOf<AppDestination>(
            if (!isConsentValid) AppDestination.AgeGate
            else if (!hasIdentity) AppDestination.RecoverySeed
            else AppDestination.AppLock
        )
    }

    val notifiedMessages = remember { mutableSetOf<String>() }

    @OptIn(ExperimentalEncodingApi::class)
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val authManager = com.example.security.DeviceAuthManager
                val myToken = authManager.getIdToken()
                val myUid = authManager.getUserId()
                
                if (myToken != null) {
                    val pendingResult = com.example.data.network.FirestoreRestClient.fetchPendingMessages(myUid, myToken)
                    if (pendingResult.isSuccess) {
                        val pending = pendingResult.getOrThrow()
                        for (msg in pending) {
                            val keyResult = com.example.data.network.KeyStoreClient.getMessageKey(msg.id, myToken)
                            if (keyResult.success && keyResult.ephemeralPubKey != null && keyResult.wrappedDek != null) {
                                val myPrivKey = com.example.security.identity.IdentityManager.getIdentity()?.privateKey
                                if (myPrivKey != null) {
                                    val env = com.example.security.identity.SealedBoxEnvelope(
                                        ephemeralPubKeyHex = keyResult.ephemeralPubKey,
                                        wrappedDekBase64 = keyResult.wrappedDek
                                    )
                                    val dek = com.example.security.identity.SealedBox.unseal(env, myPrivKey)
                                    val cipherBytes = Base64.decode(msg.ciphertext)
                                    val ivBytes = Base64.decode(msg.iv)
                                    val decryptedBytes = com.example.security.identity.AesGcm.decrypt(cipherBytes, dek, ivBytes)
                                    val decryptedText = decryptedBytes.decodeToString()
                                    
                                    if (decryptedText.startsWith("[AUTO-HANDSHAKE] ")) {
                                        val uri = decryptedText.substringAfter("[AUTO-HANDSHAKE] ").trim()
                                        val parseRes = com.example.security.identity.IdentityManager.parseContactUri(uri)
                                        if (parseRes.isSuccess) {
                                            val contactData = parseRes.getOrThrow()
                                            val existing = contactRepository.getContact(contactData.fingerprintHex)
                                            if (existing == null) {
                                                val myIdentity = com.example.security.identity.IdentityManager.getIdentity()
                                                val pairSafetyNumber = com.example.security.identity.IdentityCryptoManager.computePairSafetyNumber(
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
                                                    addedAt = com.example.data.network.PlatformEnvironment.currentTimeMillis()
                                                )
                                                contactRepository.saveContact(newContact)
                                            }
                                            // Exclui a mensagem (Vanish-after-read)
                                            com.example.data.network.FirestoreRestClient.deleteMessage(msg.id, myToken)
                                        }
                                    } else {
                                        // É uma mensagem normal recebida em 2º plano no desktop
                                        if (notifiedMessages.add(msg.id)) {
                                            com.example.security.notification.PushNotificationManager.showLocalNotification(
                                                title = "RAIX",
                                                body = "Nova mensagem recebida",
                                                messageId = msg.id
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fail silently to not disrupt the UI
            }
            delay(10000L) // Verifica a cada 10s
        }
    }

    MaterialTheme(colorScheme = RaixDarkColors, typography = com.example.ui.theme.Typography) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
            
            com.example.ui.components.BackHandler(enabled = currentDestination != AppDestination.Contacts && currentDestination != AppDestination.AppLock && currentDestination != AppDestination.AgeGate && currentDestination != AppDestination.RecoverySeed) {
                currentDestination = AppDestination.Contacts
            }

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut()
                    )
                },
                label = "app_navigation_transition"
            ) { destination ->
                when (destination) {
                    is AppDestination.AppLock -> {
                        AppLockGate(
                            onUnlocked = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }

                    is AppDestination.AgeGate -> {
                        AgeGateScreen(
                            onConsentAccepted = {
                                currentDestination = if (!hasIdentity) AppDestination.RecoverySeed else AppDestination.AppLock
                            }
                        )
                    }

                    is AppDestination.RecoverySeed -> {
                        RecoverySeedScreen(
                            onSeedSaved = {
                                currentDestination = AppDestination.AppLock
                            }
                        )
                    }

                    is AppDestination.Contacts -> {
                        ContactsScreen(
                            contactRepository = contactRepository,
                            onContactSelected = { contact ->
                                currentDestination = AppDestination.Chat(contact)
                            },
                            onOpenIdentity = {
                                currentDestination = AppDestination.Identity
                            },
                            onOpenDataPrivacy = {
                                currentDestination = AppDestination.DataPrivacy
                            },
                            onOpenBlockedContacts = {
                                currentDestination = AppDestination.BlockedContacts
                            },
                            onAddContactModelA = {
                                currentDestination = AppDestination.AddModelA
                            },
                            onCompareSafetyNumber = { contact ->
                                currentDestination = AppDestination.SafetyNumber(contact)
                            }
                        )
                    }

                    is AppDestination.BlockedContacts -> {
                        BlockedContactsScreen(
                            contactRepository = contactRepository,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }

                    is AppDestination.Chat -> {
                        ContactChatScreen(
                            contact = destination.contact,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onCompareSafetyNumber = {
                                currentDestination = AppDestination.SafetyNumber(destination.contact)
                            }
                        )
                    }

                    is AppDestination.Identity -> {
                        IdentityScreen(
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onProvisioned = {
                                currentDestination = AppDestination.Contacts
                            },
                            onOpenDataPrivacy = {
                                currentDestination = AppDestination.DataPrivacy
                            }
                        )
                    }

                    is AppDestination.DataPrivacy -> {
                        DataPrivacyScreen(
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }

                    is AppDestination.AddModelA -> {
                        AddContactModelAScreen(
                            contactRepository = contactRepository,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onContactCreated = { newContact ->
                                currentDestination = AppDestination.SafetyNumber(newContact)
                            }
                        )
                    }

                    is AppDestination.SafetyNumber -> {
                        SafetyNumberScreen(
                            contact = destination.contact,
                            contactRepository = contactRepository,
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            },
                            onVerifiedComplete = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }

                    is AppDestination.QrHandshake -> {
                        com.example.ui.screens.QrHandshakeScreen(
                            onHandshakeSuccess = {
                                currentDestination = AppDestination.Contacts
                            },
                            onBack = {
                                currentDestination = AppDestination.Contacts
                            }
                        )
                    }
                }
            }
        }
    }
}
