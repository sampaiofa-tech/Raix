package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepositoryProvider
import com.example.data.worker.ExpiredMessageCleanupWorker
import com.example.ui.screens.AddContactModelAScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.BlockedContactsScreen
import com.example.ui.screens.ChannelListScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ContactChatScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DataPrivacyScreen
import com.example.ui.screens.IdentityScreen
import com.example.ui.screens.SafetyNumberScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ImmersiveSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.util.ContactsHelper
import com.example.util.NotificationHelper
import com.example.util.ScreenshotDetector
import com.example.util.ShakeDetector

class MainActivity : FragmentActivity() {

  private val viewModel: ChatViewModel by viewModels()
  private lateinit var screenshotDetector: ScreenshotDetector
  private lateinit var shakeDetector: ShakeDetector

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Enforce fail-secure screen protection immediately to prevent any leak in recents/snapshots
    window.setFlags(
      WindowManager.LayoutParams.FLAG_SECURE,
      WindowManager.LayoutParams.FLAG_SECURE
    )
    com.example.util.AndroidContextHolder.appContext = applicationContext
    com.example.security.consent.LegalConsentStorage.initialize(applicationContext)
    enableEdgeToEdge()

    // Initialize notification channels
    NotificationHelper.createNotificationChannels(applicationContext)

    // Initialize screenshot detector
    screenshotDetector = ScreenshotDetector(this) {
      viewModel.onScreenshotDetected()
    }

    // Initialize shake detector for instant zero-trace chat wipe
    shakeDetector = ShakeDetector(this) {
      viewModel.onDeviceShaken()
    }

    // Initialize background Worker to automatically purge all Room messages older than 24h at regular intervals
    ExpiredMessageCleanupWorker.schedulePeriodicCleanup(applicationContext)
    ExpiredMessageCleanupWorker.runImmediateCleanup(applicationContext)

    handleIncomingRoomIntent(intent)

    setContent {
      val screenProtectionEnabled by viewModel.screenProtectionEnabled.collectAsStateWithLifecycle()
      val screenshotDetectionEnabled by viewModel.screenshotDetectionEnabled.collectAsStateWithLifecycle()
      val context = LocalContext.current

      var hasNotificationPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
      }

      val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
      ) { isGranted ->
        hasNotificationPermission = isGranted
      }

      // Permissao de notificacao solicitada no gate do onboarding (AndroidAppWithGates)

      // Fail-secure permanent enforcement of FLAG_SECURE to prevent screenshots, recordings and recents leaks
      LaunchedEffect(Unit) {
        window.setFlags(
          WindowManager.LayoutParams.FLAG_SECURE,
          WindowManager.LayoutParams.FLAG_SECURE
        )
      }

      // Dynamically start/stop real-time screenshot detector
      LaunchedEffect(screenshotDetectionEnabled) {
        if (screenshotDetectionEnabled) {
          screenshotDetector.startListening()
        } else {
          screenshotDetector.stopListening()
        }
      }

      val shakeToClearEnabled by viewModel.shakeToClearEnabled.collectAsStateWithLifecycle()
      val shakeSensitivity by viewModel.shakeSensitivity.collectAsStateWithLifecycle()

      // Dynamically start/stop shake detector with adjusted sensitivity
      LaunchedEffect(shakeToClearEnabled, shakeSensitivity) {
        shakeDetector.sensitivityThreshold = when (shakeSensitivity) {
          "HIGH" -> 1.8f
          "LOW" -> 3.2f
          else -> 2.4f
        }
        if (shakeToClearEnabled) {
          shakeDetector.startListening()
        } else {
          shakeDetector.stopListening()
        }
      }

      MyApplicationTheme(darkTheme = true) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = ImmersiveSurface
        ) {
          AndroidAppWithGates(
            viewModel = viewModel,
            notificationsEnabled = hasNotificationPermission,
            onRequestNotificationPermission = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              }
            }
          )
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    if (com.example.util.security.SecurePrefsHelper.isPrivacyCurtainEnabled(this)) {
      window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
      )
    }
  }

  override fun onResume() {
    super.onResume()
    window.setFlags(
      WindowManager.LayoutParams.FLAG_SECURE,
      WindowManager.LayoutParams.FLAG_SECURE
    )
  }

  override fun onStart() {
    super.onStart()
    if (::screenshotDetector.isInitialized && viewModel.screenshotDetectionEnabled.value) {
      screenshotDetector.startListening()
    }
    if (::shakeDetector.isInitialized && viewModel.shakeToClearEnabled.value) {
      shakeDetector.startListening()
    }
  }

  override fun onStop() {
    super.onStop()
    if (::screenshotDetector.isInitialized) {
      screenshotDetector.stopListening()
    }
    if (::shakeDetector.isInitialized) {
      shakeDetector.stopListening()
    }
  }

  override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
    viewModel.onUserInteraction()
    return super.dispatchTouchEvent(ev)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIncomingRoomIntent(intent)
  }

  private var pendingRoomId: String? = null

  private fun handleIncomingRoomIntent(intent: Intent?) {
    val roomId = intent?.getStringExtra("SELECTED_ROOM_ID") ?: intent?.getStringExtra("messageId")
    if (!roomId.isNullOrBlank()) {
      com.example.security.notification.PushNotificationManager.setClickedMessageId(roomId)
    }
  }

  internal fun trySelectPendingRoom() {
    val roomId = pendingRoomId ?: return
    val channel = viewModel.channels.value.find { it.id == roomId }
    if (channel != null) {
      viewModel.selectChannel(channel)
      pendingRoomId = null
    }
  }
}

/**
 * Composable Android-only que executa os gates (AgeGate, RecoverySeed, MasterPassword)
 * e depois delega para VanishApp (ChannelListScreen + ChatScreen + SettingsScreen).
 * O App() do commonMain fica reservado ao desktop.
 */
@Composable
fun AndroidAppWithGates(
  viewModel: ChatViewModel,
  notificationsEnabled: Boolean,
  onRequestNotificationPermission: () -> Unit
) {
  val context = LocalContext.current
  val isConsentValid = remember { com.example.security.consent.LegalConsentManager.isConsentValid() }
  val hasIdentity = remember { com.example.security.identity.IdentityManager.hasIdentity() }
  val hasPinSet = remember { com.example.util.security.SecurePrefsHelper.isPinSet(context) }

  var gateState by remember {
    mutableStateOf(
      when {
        !isConsentValid -> "AGE_GATE"
        !hasIdentity -> "RECOVERY_SEED"
        !hasPinSet -> "MASTER_PASSWORD"
        else -> "UNLOCKED"
      }
    )
  }

  when (gateState) {
    "AGE_GATE" -> {
      com.example.ui.screens.AgeGateScreen(
        onConsentAccepted = {
          gateState = if (!hasIdentity) "RECOVERY_SEED" else if (!hasPinSet) "MASTER_PASSWORD" else "UNLOCKED"
        }
      )
    }
    "RECOVERY_SEED" -> {
      com.example.ui.screens.RecoverySeedScreen(
        onSeedSaved = { gateState = if (!hasPinSet) "MASTER_PASSWORD" else "UNLOCKED" }
      )
    }
    "MASTER_PASSWORD" -> {
      com.example.ui.screens.MasterPasswordSetupScreen(
        onSetupComplete = { gateState = "BIOMETRIC_OFFER" }
      )
    }
    "BIOMETRIC_OFFER" -> {
      com.example.ui.screens.BiometricOfferScreen(
        onAccept = { gateState = "NOTIFICATION_PERMISSION" },
        onDecline = { gateState = "NOTIFICATION_PERMISSION" }
      )
    }
    "NOTIFICATION_PERMISSION" -> {
      // Solicitar POST_NOTIFICATIONS (Android 13+) apos o onboarding
      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsEnabled) {
          onRequestNotificationPermission()
        }
      }
      // Avanca para UNLOCKED apos a resposta (ou imediato se Android < 13)
      LaunchedEffect(notificationsEnabled) {
        if (notificationsEnabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
          gateState = "UNLOCKED"
        }
      }
      // Timeout: se o usuario negar, avanca apos 3 segundos
      LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        if (gateState == "NOTIFICATION_PERMISSION") {
          gateState = "UNLOCKED"
        }
      }
    }
    else -> {
      VanishApp(
        viewModel = viewModel,
        notificationsEnabled = notificationsEnabled,
        hasContactsPermission = true,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onRequestContactsPermission = {}
      )
    }
  }
}

@Composable
fun VanishApp(
  viewModel: ChatViewModel,
  notificationsEnabled: Boolean,
  hasContactsPermission: Boolean,
  onRequestNotificationPermission: () -> Unit,
  onRequestContactsPermission: () -> Unit
) {
  val channels by viewModel.channels.collectAsStateWithLifecycle()
  val context = LocalContext.current
  LaunchedEffect(channels) {
    if (channels.isNotEmpty()) {
      (context as? MainActivity)?.trySelectPendingRoom()
    }
  }
  // Registro imediato do push token apos onboarding (fecha janela wipe x push)
  LaunchedEffect(Unit) {
    try {
      val authManager = com.example.security.DeviceAuthManager
      val idToken = authManager.getIdToken()
      if (idToken != null) {
        val pushToken = com.example.security.notification.PushNotificationManager.getPushToken()
        if (pushToken != null) {
          com.example.data.network.IdentityNetworkClient.registerPushToken(pushToken, "android", idToken)
          println("[DIAGNOSTICO] Push token registrado imediatamente apos onboarding")
        }
      }
    } catch (e: Exception) {
      println("[DIAGNOSTICO] Falha ao registrar push token imediato: ${e.message}")
    }
  }
  val contacts by viewModel.contacts.collectAsStateWithLifecycle()
  val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
  val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
  val selectedTtl by viewModel.selectedTtl.collectAsStateWithLifecycle()
  val screenProtectionEnabled by viewModel.screenProtectionEnabled.collectAsStateWithLifecycle()
  val screenshotDetectionEnabled by viewModel.screenshotDetectionEnabled.collectAsStateWithLifecycle()
  val blockSensitiveOnScreenshot by viewModel.blockSensitiveOnScreenshot.collectAsStateWithLifecycle()
  val isScreenshotLockdownActive by viewModel.isScreenshotLockdownActive.collectAsStateWithLifecycle()
  val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()
  val autoLockEnabled by viewModel.autoLockEnabled.collectAsStateWithLifecycle()
  val autoLockTimeoutMinutes by viewModel.autoLockTimeoutMinutes.collectAsStateWithLifecycle()
  val securityPin by viewModel.securityPin.collectAsStateWithLifecycle()
  val readReceiptsEnabled by viewModel.readReceiptsEnabled.collectAsStateWithLifecycle()
  val vanishAfterReadPresetSeconds by viewModel.vanishAfterReadPresetSeconds.collectAsStateWithLifecycle()
  val shakeToClearEnabled by viewModel.shakeToClearEnabled.collectAsStateWithLifecycle()
  val shakeSensitivity by viewModel.shakeSensitivity.collectAsStateWithLifecycle()
  val shakeRequiresConfirmation by viewModel.shakeRequiresConfirmation.collectAsStateWithLifecycle()
  val notifyOnNewConversation by viewModel.notifyOnNewConversation.collectAsStateWithLifecycle()
  val shakeDialogVisible by viewModel.shakeDialogVisible.collectAsStateWithLifecycle()
  val shakeWipeEventTimestamp by viewModel.shakeWipeEventTimestamp.collectAsStateWithLifecycle()
  val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
  val userFeedback by viewModel.userFeedback.collectAsStateWithLifecycle()
  var isSettingsOpen by remember { mutableStateOf(false) }
  // Destino secundario para telas do commonMain (Identity, DataPrivacy, etc.)
  var secondaryScreen by remember { mutableStateOf<String?>(null) }
  // Contato selecionado para SafetyNumber ou ContactChat E2E
  var selectedContact by remember { mutableStateOf<ContactItem?>(null) }
  val contactRepository = remember { ContactRepositoryProvider.get() }
  val e2eContacts by contactRepository.getContacts().collectAsStateWithLifecycle(initialValue = emptyList())
  val scope = androidx.compose.runtime.rememberCoroutineScope()

  // --- Listener E2E unificado (commonMain) ---
  com.example.security.E2EMessageListenerEffect(
    contactRepository = contactRepository,
    onNavigateToContact = { contact ->
      selectedContact = contact
      secondaryScreen = "CONTACT_CHAT"
    }
  )

  // If auto-lock or biometric lock is active and app is locked, display the Lock Screen
  if (!isAppUnlocked && (biometricLockEnabled || autoLockEnabled)) {
    BiometricLockScreen(
      onVerifyPin = { viewModel.verifySecurityPin(it) },
      biometricEnabled = biometricLockEnabled,
      autoLockTimeoutMinutes = autoLockTimeoutMinutes,
      onDuressTriggered = { viewModel.onPanicWipe() },
      onUnlocked = { viewModel.unlockApp() }
    )
    return
  }

  // Handle Android system back press
  BackHandler(enabled = isSettingsOpen || selectedChannel != null || secondaryScreen != null) {
    when {
      secondaryScreen != null -> secondaryScreen = null
      isSettingsOpen -> isSettingsOpen = false
      selectedChannel != null -> viewModel.selectChannel(null)
    }
  }

  // Sealed representation of current full page destination
  val currentScreen = when {
    secondaryScreen != null -> secondaryScreen!!
    isSettingsOpen -> "SETTINGS"
    selectedChannel != null -> "CHAT"
    else -> "CHANNEL_LIST"
  }

  AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
      if (targetState == "SETTINGS" || targetState == "CHAT") {
        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
          slideOutHorizontally { width -> -width / 3 } + fadeOut()
        )
      } else {
        (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
          slideOutHorizontally { width -> width } + fadeOut()
        )
      }
    },
    label = "screen_transition"
  ) { screenState ->
    when (screenState) {
      "SETTINGS" -> {
        SettingsScreen(
          screenProtectionEnabled = screenProtectionEnabled,
          screenshotDetectionEnabled = screenshotDetectionEnabled,
          blockSensitiveOnScreenshot = blockSensitiveOnScreenshot,
          biometricLockEnabled = biometricLockEnabled,
          autoLockEnabled = autoLockEnabled,
          autoLockTimeoutMinutes = autoLockTimeoutMinutes,
          securityPin = "••••",
          readReceiptsEnabled = readReceiptsEnabled,
          vanishAfterReadPresetSeconds = vanishAfterReadPresetSeconds,
          shakeToClearEnabled = shakeToClearEnabled,
          shakeSensitivity = shakeSensitivity,
          shakeRequiresConfirmation = shakeRequiresConfirmation,
          isHardwareBackedCrypto = viewModel.isHardwareBackedCrypto,
          notificationsEnabled = notificationsEnabled,
          notifyOnNewConversation = notifyOnNewConversation,
          onRequestNotificationPermission = onRequestNotificationPermission,
          onTestNotification = { viewModel.triggerTestNotification() },
          onToggleNotifyOnNewConversation = { viewModel.setNotifyOnNewConversation(it) },
          onToggleScreenProtection = { viewModel.toggleScreenProtection() },
          onToggleScreenshotDetection = { viewModel.setScreenshotDetectionEnabled(it) },
          onToggleBlockSensitiveOnScreenshot = { viewModel.setBlockSensitiveOnScreenshot(it) },
          onSimulateScreenshot = { viewModel.simulateScreenshotDetection() },
          onToggleBiometricLock = { viewModel.setBiometricLockEnabled(it) },
          onToggleAutoLock = { viewModel.setAutoLockEnabled(it) },
          onSetAutoLockTimeout = { viewModel.setAutoLockTimeoutMinutes(it) },
          onSetSecurityPin = { viewModel.setSecurityPin(it) },
          onToggleReadReceipts = { viewModel.setReadReceiptsEnabled(it) },
          onSetVanishAfterReadPresetSeconds = { viewModel.setVanishAfterReadPresetSeconds(it) },
          onToggleShakeToClear = { viewModel.setShakeToClearEnabled(it) },
          onSetShakeSensitivity = { viewModel.setShakeSensitivity(it) },
          onToggleShakeRequiresConfirmation = { viewModel.setShakeRequiresConfirmation(it) },
          onSimulateShake = { viewModel.onDeviceShaken() },
          onLockNow = { viewModel.lockApp() },
          onTriggerWorkManagerCleanup = { viewModel.triggerWorkManagerCleanup() },
          onPanicWipe = { viewModel.panicWipeAll() },
          onBack = { isSettingsOpen = false }
        )
      }
      "CHAT" -> {
        selectedChannel?.let { targetChannel ->
          ChatScreen(
            channel = targetChannel,
            messages = activeMessages,
            currentTime = currentTime,
            selectedTtl = selectedTtl,
            ttlPresets = viewModel.ttlPresets,
            userFeedback = userFeedback,
            isScreenshotLockdownActive = isScreenshotLockdownActive,
            vanishAfterReadPresetSeconds = vanishAfterReadPresetSeconds,
            shakeDialogVisible = shakeDialogVisible,
            shakeWipeEventTimestamp = shakeWipeEventTimestamp,
            onSetVanishAfterReadPresetSeconds = { viewModel.setVanishAfterReadPresetSeconds(it) },
            onDismissScreenshotLockdown = { viewModel.dismissScreenshotLockdown() },
            onSimulateScreenshot = { viewModel.simulateScreenshotDetection() },
            onSimulateRead = { viewModel.simulateRecipientRead(it) },
            onTriggerShakeWipe = { viewModel.wipeActiveChatHistory() },
            onDismissShakeDialog = { viewModel.dismissShakeDialog() },
            onSimulateShake = { viewModel.onDeviceShaken() },
            onBack = { viewModel.selectChannel(null) },
            onSendMessage = { text, isBurnerNote, isViewOnce, customTtl ->
              viewModel.sendMessage(
                text = text,
                isBurnerNote = isBurnerNote,
                customTtlHours = customTtl,
                isViewOnce = isViewOnce
              )
            },
            onSendAudio = { duration, isViewOnce, customTtl ->
              viewModel.sendAudioMessage(
                durationSeconds = duration,
                isViewOnce = isViewOnce,
                customTtlHours = customTtl
              )
            },
            onSendMedia = { mediaType, mediaUri, fileName, fileSize, caption, isViewOnce, customTtl ->
              viewModel.sendMediaMessage(
                mediaType = mediaType,
                mediaUri = mediaUri,
                fileName = fileName,
                fileSize = fileSize,
                caption = caption,
                isViewOnce = isViewOnce,
                customTtlHours = customTtl
              )
            },
            onSimulateReply = { viewModel.simulateContactReply() },
            onShredMessage = { viewModel.shredMessage(it) },
            onIncinerateRoom = { viewModel.incinerateRoom(it) },
            onSelectTtl = { viewModel.setTtl(it) },
            onClearFeedback = { viewModel.clearFeedback() }
          )
        }
      }
      "IDENTITY" -> {
        IdentityScreen(
          onBack = { secondaryScreen = null },
          onProvisioned = { secondaryScreen = null },
          onOpenDataPrivacy = { secondaryScreen = "DATA_PRIVACY" }
        )
      }
      "DATA_PRIVACY" -> {
        DataPrivacyScreen(
          onBack = { secondaryScreen = null }
        )
      }
      "CONTACTS" -> {
        ContactsScreen(
          contactRepository = contactRepository,
          onContactSelected = { contact ->
            selectedContact = contact
            secondaryScreen = "CONTACT_CHAT"
          },
          onOpenIdentity = { secondaryScreen = "IDENTITY" },
          onOpenDataPrivacy = { secondaryScreen = "DATA_PRIVACY" },
          onOpenBlockedContacts = { secondaryScreen = "BLOCKED_CONTACTS" },
          onOpenAgenda = { secondaryScreen = "AGENDA" },
          onAddContactModelA = { secondaryScreen = "ADD_MODEL_A" },
          onCompareSafetyNumber = { contact ->
            selectedContact = contact
            secondaryScreen = "SAFETY_NUMBER"
          }
        )
      }
      "CONTACT_CHAT" -> {
        selectedContact?.let { contact ->
          ContactChatScreen(
            contact = contact,
            onBack = { secondaryScreen = null; selectedContact = null },
            onCompareSafetyNumber = { secondaryScreen = "SAFETY_NUMBER" }
          )
        }
      }
      "SAFETY_NUMBER" -> {
        selectedContact?.let { contact ->
          SafetyNumberScreen(
            contact = contact,
            contactRepository = contactRepository,
            onBack = { secondaryScreen = null },
            onVerifiedComplete = { secondaryScreen = null }
          )
        }
      }
      "ADD_MODEL_A" -> {
        AddContactModelAScreen(
          contactRepository = contactRepository,
          onBack = { secondaryScreen = null },
          onContactCreated = { newContact ->
            selectedContact = newContact
            secondaryScreen = "SAFETY_NUMBER"
          }
        )
      }
      "QR_HANDSHAKE" -> {
        com.example.ui.screens.QrScannerHandshakeScreen(
          onHandshakeSuccess = { secondaryScreen = null },
          onBack = { secondaryScreen = null }
        )
      }
      "AGENDA" -> {
        com.example.ui.screens.AgendaScreen(
          contactRepository = contactRepository,
          onBack = { secondaryScreen = null },
          onCompareSafetyNumber = { contact ->
            selectedContact = contact
            secondaryScreen = "SAFETY_NUMBER"
          }
        )
      }
      "BLOCKED_CONTACTS" -> {
        BlockedContactsScreen(
          contactRepository = contactRepository,
          onBack = { secondaryScreen = null }
        )
      }
      else -> {
        ChannelListScreen(
          channels = channels,
          contacts = contacts,
          e2eContacts = e2eContacts,
          currentTime = currentTime,
          screenProtectionEnabled = screenProtectionEnabled,
          biometricLockEnabled = biometricLockEnabled,
          autoLockEnabled = autoLockEnabled,
          autoLockTimeoutMinutes = autoLockTimeoutMinutes,
          securityPin = securityPin,
          notificationsEnabled = notificationsEnabled,
          hasContactsPermission = hasContactsPermission,
          userFeedback = userFeedback,
          onSelectChannel = { viewModel.selectChannel(it) },
          onCreateChannel = { name, code, ttlHours -> viewModel.createBurnerChannel(name, code, ttlHours) },
          onStartChatWithContact = { viewModel.startChatWithContact(it) },
          onSelectE2eContact = { contact ->
            selectedContact = contact
            secondaryScreen = "CONTACT_CHAT"
          },
          onDeleteChannel = { viewModel.deleteChannel(it) },
          onPanicWipe = { viewModel.panicWipeAll() },
          onToggleScreenProtection = { viewModel.toggleScreenProtection() },
          onToggleBiometricLock = { viewModel.setBiometricLockEnabled(it) },
          onToggleAutoLock = { viewModel.setAutoLockEnabled(it) },
          onSetAutoLockTimeout = { viewModel.setAutoLockTimeoutMinutes(it) },
          onSetSecurityPin = { viewModel.setSecurityPin(it) },
          onLockNow = { viewModel.lockApp() },
          onRequestNotificationPermission = onRequestNotificationPermission,
          onRequestContactsPermission = onRequestContactsPermission,
          onRefreshContacts = { viewModel.refreshContacts() },
          onSimulateIncomingNewConversation = { viewModel.simulateIncomingNewConversation() },
          onTestNotification = { viewModel.triggerTestNotification() },
          onOpenSettings = { isSettingsOpen = true },
          onOpenContacts = { secondaryScreen = "CONTACTS" },
          onOpenIdentity = { secondaryScreen = "IDENTITY" },
          onOpenQrHandshake = { secondaryScreen = "ADD_MODEL_A" },
          onOpenAddContact = { secondaryScreen = "ADD_MODEL_A" },
          onToggleFavorite = { contact ->
            scope.launch { contactRepository.setFavorite(contact.fingerprint, !contact.isFavorite) }
          },
          onRenameContact = { contact, newName ->
            scope.launch { contactRepository.renameContact(contact.fingerprint, newName) }
          },
          onDeleteContact = { contact ->
            scope.launch { contactRepository.deleteContact(contact.fingerprint) }
          },
          onBlockContact = { contact ->
            scope.launch { contactRepository.blockContact(contact.fingerprint) }
          },
          onClearFeedback = { viewModel.clearFeedback() }
        )
      }
    }
  }
}
