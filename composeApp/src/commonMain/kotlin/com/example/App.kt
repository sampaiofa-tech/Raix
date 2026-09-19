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
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
import com.example.ui.screens.AddContactModelAScreen
import com.example.ui.screens.AgeGateScreen
import com.example.ui.screens.AppLockGate
import com.example.ui.screens.BlockedContactsScreen
import com.example.ui.screens.ContactChatScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DataPrivacyScreen
import com.example.ui.screens.IdentityScreen
import com.example.ui.screens.SafetyNumberScreen
import com.example.ui.screens.RecoverySeedScreen
import com.example.security.identity.IdentityManager

sealed interface AppDestination {
    data object AppLock : AppDestination
    data object AgeGate : AppDestination
    data object RecoverySeed : AppDestination
    data object Contacts : AppDestination
    data object BlockedContacts : AppDestination
    data class Chat(val contact: ContactItem) : AppDestination
    data object Identity : AppDestination
    data object DataPrivacy : AppDestination
    data object AddModelA : AppDestination
    data class SafetyNumber(val contact: ContactItem) : AppDestination
    data object QrHandshake : AppDestination
}

private val RaixDarkColors = darkColorScheme(
    primary = Color(0xFFD4AF37),      // Gold
    onPrimary = Color(0xFF0A1128),
    secondary = Color(0xFF043927),    // Emerald Green
    onSecondary = Color(0xFFF1F5F9),
    tertiary = Color(0xFFD4AF37),
    surface = Color(0xFF0A1128),      // Navy Blue
    onSurface = Color(0xFFF1F5F9),
    background = Color(0xFF0A1128),   // Navy Blue
    onBackground = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF064D35),
    outline = Color(0x80D4AF37)       // Semi-transparent Gold
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

    val RaixTypography = Typography(
        displayLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.25.sp
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp
        )
    )

    MaterialTheme(colorScheme = RaixDarkColors, typography = RaixTypography) {
        Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding(), color = MaterialTheme.colorScheme.background) {
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

