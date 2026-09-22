package com.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.first
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
import com.example.security.E2EMessageListenerEffect

sealed interface AppDestination {
    data object AppLock : AppDestination
    data object AgeGate : AppDestination
    data object RecoverySeed : AppDestination
    data object Contacts : AppDestination
    data object Agenda : AppDestination
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

    // Listener E2E unificado (commonMain)
    E2EMessageListenerEffect(
        contactRepository = contactRepository,
        onNavigateToContact = { contact ->
            currentDestination = AppDestination.Chat(contact)
        }
    )

    MaterialTheme(colorScheme = RaixDarkColors, typography = com.example.ui.theme.getRaixTypography()) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {

            com.example.ui.components.BackHandler(enabled = currentDestination != AppDestination.Contacts && currentDestination != AppDestination.AppLock && currentDestination != AppDestination.AgeGate && currentDestination != AppDestination.RecoverySeed) {
                if (currentDestination == AppDestination.Agenda) {
                    currentDestination = AppDestination.Contacts
                } else {
                    currentDestination = AppDestination.Contacts
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDesktop = maxWidth >= androidx.compose.ui.unit.Dp(900f)

                if (isDesktop && currentDestination !is AppDestination.AppLock && currentDestination !is AppDestination.AgeGate && currentDestination !is AppDestination.RecoverySeed) {
                    // Desktop 3-column layout: Rail | List | Detail
                    var selectedRailIndex by remember { mutableStateOf(0) }
                    // O detalhe sobreposto pelo rail (nao-Chat):
                    // null = mostra o destino do currentDestination normal
                    var desktopDetailOverride by remember { mutableStateOf<AppDestination?>(null) }

                    Row(modifier = Modifier.fillMaxSize()) {
                        // Column 1: Navigation Rail (64dp)
                        DesktopNavigationRail(
                            selectedIndex = selectedRailIndex,
                            onSelectIndex = { index ->
                                selectedRailIndex = index
                                when (index) {
                                    0 -> desktopDetailOverride = null // Chat: detalhe segue selecao
                                    1 -> desktopDetailOverride = AppDestination.Agenda
                                    2 -> desktopDetailOverride = AppDestination.Identity
                                    3 -> desktopDetailOverride = AppDestination.DataPrivacy
                                }
                            }
                        )

                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            thickness = androidx.compose.ui.unit.Dp(1f),
                            color = Color(0xFF1E2432)
                        )

                        // Column 2: Lista de conversas (320dp) — SEMPRE ContactsScreen
                        Surface(
                            modifier = Modifier.width(androidx.compose.ui.unit.Dp(320f)).fillMaxHeight(),
                            color = Color(0xFF111827)
                        ) {
                            ContactsScreen(
                                contactRepository = contactRepository,
                                onContactSelected = { contact ->
                                    selectedRailIndex = 0
                                    desktopDetailOverride = null
                                    currentDestination = AppDestination.Chat(contact)
                                },
                                onOpenIdentity = {
                                    selectedRailIndex = 2
                                    desktopDetailOverride = AppDestination.Identity
                                },
                                onOpenDataPrivacy = {
                                    selectedRailIndex = 3
                                    desktopDetailOverride = AppDestination.DataPrivacy
                                },
                                onOpenBlockedContacts = {
                                    desktopDetailOverride = AppDestination.BlockedContacts
                                },
                                onOpenAgenda = {
                                    selectedRailIndex = 1
                                    desktopDetailOverride = AppDestination.Agenda
                                },
                                onAddContactModelA = {
                                    desktopDetailOverride = AppDestination.AddModelA
                                },
                                onCompareSafetyNumber = { contact ->
                                    desktopDetailOverride = AppDestination.SafetyNumber(contact)
                                }
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight(),
                            thickness = androidx.compose.ui.unit.Dp(1f),
                            color = Color(0xFF1E2432)
                        )

                        // Column 3: Detail pane — segue o rail ou a selecao de conversa
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            DesktopDetailPane(
                                destination = desktopDetailOverride ?: currentDestination,
                                contactRepository = contactRepository,
                                onNavigate = { dest ->
                                    if (dest == AppDestination.Contacts) {
                                        desktopDetailOverride = null
                                        selectedRailIndex = 0
                                    } else {
                                        currentDestination = dest
                                        desktopDetailOverride = null
                                    }
                                }
                            )
                        }
                    }
                } else {
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
                            onOpenAgenda = {
                                currentDestination = AppDestination.Agenda
                            },
                            onAddContactModelA = {
                                currentDestination = AppDestination.AddModelA
                            },
                            onCompareSafetyNumber = { contact ->
                                currentDestination = AppDestination.SafetyNumber(contact)
                            }
                        )
                    }

                    is AppDestination.Agenda -> {
                        com.example.ui.screens.AgendaScreen(
                            contactRepository = contactRepository,
                            onBack = {
                                currentDestination = AppDestination.Contacts
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
                } // else
            } // BoxWithConstraints
        }
    }
}

/**
 * Navigation Rail desacoplado para desktop (64dp).
 */
@Composable
private fun DesktopNavigationRail(
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF0B1325),
        contentColor = Color(0xFFF5F7FA),
        modifier = Modifier.width(androidx.compose.ui.unit.Dp(64f)).fillMaxHeight()
    ) {
        Spacer(Modifier.height(androidx.compose.ui.unit.Dp(16f)))
        NavigationRailItem(
            selected = selectedIndex == 0,
            onClick = { onSelectIndex(0) },
            icon = { Icon(Icons.Default.Chat, contentDescription = "Conversas", tint = if (selectedIndex == 0) Color(0xFF00E676) else Color(0xFF8A93A6)) },
            label = { Text("Chat", fontSize = 10.sp, color = if (selectedIndex == 0) Color(0xFF00E676) else Color(0xFF8A93A6)) }
        )
        NavigationRailItem(
            selected = selectedIndex == 1,
            onClick = { onSelectIndex(1) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Contatos", tint = if (selectedIndex == 1) Color(0xFF00E676) else Color(0xFF8A93A6)) },
            label = { Text("Agenda", fontSize = 10.sp, color = if (selectedIndex == 1) Color(0xFF00E676) else Color(0xFF8A93A6)) }
        )
        NavigationRailItem(
            selected = selectedIndex == 2,
            onClick = { onSelectIndex(2) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Identidade", tint = if (selectedIndex == 2) Color(0xFF00E676) else Color(0xFF8A93A6)) },
            label = { Text("ID", fontSize = 10.sp, color = if (selectedIndex == 2) Color(0xFF00E676) else Color(0xFF8A93A6)) }
        )
        NavigationRailItem(
            selected = selectedIndex == 3,
            onClick = { onSelectIndex(3) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Config", tint = if (selectedIndex == 3) Color(0xFF00E676) else Color(0xFF8A93A6)) },
            label = { Text("Config", fontSize = 10.sp, color = if (selectedIndex == 3) Color(0xFF00E676) else Color(0xFF8A93A6)) }
        )
    }
}

/**
 * Detail pane desacoplado para desktop — renderiza a tela de detalhe conforme destino.
 */
@Composable
private fun DesktopDetailPane(
    destination: AppDestination,
    contactRepository: com.example.data.repository.ContactRepository,
    onNavigate: (AppDestination) -> Unit
) {
    when (destination) {
        is AppDestination.Chat -> {
            ContactChatScreen(
                contact = destination.contact,
                onBack = { onNavigate(AppDestination.Contacts) },
                onCompareSafetyNumber = { onNavigate(AppDestination.SafetyNumber(destination.contact)) }
            )
        }
        is AppDestination.Identity -> {
            IdentityScreen(
                onBack = { onNavigate(AppDestination.Contacts) },
                onProvisioned = { onNavigate(AppDestination.Contacts) },
                onOpenDataPrivacy = { onNavigate(AppDestination.DataPrivacy) }
            )
        }
        is AppDestination.DataPrivacy -> {
            DataPrivacyScreen(onBack = { onNavigate(AppDestination.Contacts) })
        }
        is AppDestination.SafetyNumber -> {
            SafetyNumberScreen(
                contact = destination.contact,
                contactRepository = contactRepository,
                onBack = { onNavigate(AppDestination.Contacts) },
                onVerifiedComplete = { onNavigate(AppDestination.Contacts) }
            )
        }
        is AppDestination.AddModelA -> {
            AddContactModelAScreen(
                contactRepository = contactRepository,
                onBack = { onNavigate(AppDestination.Contacts) },
                onContactCreated = { newContact -> onNavigate(AppDestination.SafetyNumber(newContact)) }
            )
        }
        is AppDestination.BlockedContacts -> {
            BlockedContactsScreen(
                contactRepository = contactRepository,
                onBack = { onNavigate(AppDestination.Contacts) }
            )
        }
        is AppDestination.Agenda -> {
            com.example.ui.screens.AgendaScreen(
                contactRepository = contactRepository,
                onBack = { onNavigate(AppDestination.Contacts) },
                onCompareSafetyNumber = { contact -> onNavigate(AppDestination.SafetyNumber(contact)) }
            )
        }
        is AppDestination.QrHandshake -> {
            com.example.ui.screens.QrHandshakeScreen(
                onHandshakeSuccess = { onNavigate(AppDestination.Contacts) },
                onBack = { onNavigate(AppDestination.Contacts) }
            )
        }
        else -> {
            // Contacts destination no detail — show placeholder
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0B1325)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = "Selecione uma conversa",
                        color = Color(0xFF8A93A6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}
