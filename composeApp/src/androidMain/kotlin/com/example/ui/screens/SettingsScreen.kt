package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoDelete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PmsgLogoBadge
import com.example.ui.components.PmsgWordmark
import com.example.ui.theme.RaixError
import com.example.ui.theme.RaixErrorContainer
import com.example.ui.theme.RaixSurface
import com.example.ui.theme.RaixSurfaceElevated
import com.example.ui.theme.RaixTextSecondary
import com.example.ui.theme.RaixTextPrimary
import com.example.ui.theme.RaixBackground
import com.example.ui.theme.RaixBorder
import com.example.ui.theme.RaixActionPrimary
import com.example.ui.theme.RaixPremiumGold
import com.example.util.BiometricAuthHelper
import com.example.util.security.SecurePrefsHelper

@Composable
fun SettingsScreen(
    screenProtectionEnabled: Boolean,
    screenshotDetectionEnabled: Boolean = true,
    blockSensitiveOnScreenshot: Boolean = true,
    biometricLockEnabled: Boolean = false,
    autoLockEnabled: Boolean = true,
    autoLockTimeoutMinutes: Int = 5,
    securityPin: String = "1234",
    readReceiptsEnabled: Boolean = true,
    vanishAfterReadPresetSeconds: Int = 0,
    shakeToClearEnabled: Boolean = true,
    shakeSensitivity: String = "NORMAL",
    shakeRequiresConfirmation: Boolean = false,
    isHardwareBackedCrypto: Boolean = true,
    notificationsEnabled: Boolean,
    notifyOnNewConversation: Boolean = false,
    onRequestNotificationPermission: () -> Unit,
    onTestNotification: () -> Unit,
    onToggleNotifyOnNewConversation: (Boolean) -> Unit = {},
    onToggleScreenProtection: () -> Unit,
    onToggleScreenshotDetection: (Boolean) -> Unit = {},
    onToggleBlockSensitiveOnScreenshot: (Boolean) -> Unit = {},
    onSimulateScreenshot: () -> Unit = {},
    onToggleBiometricLock: (Boolean) -> Unit = {},
    onToggleAutoLock: (Boolean) -> Unit = {},
    onSetAutoLockTimeout: (Int) -> Unit = {},
    onSetSecurityPin: (String) -> Unit = {},
    onToggleReadReceipts: (Boolean) -> Unit = {},
    onSetVanishAfterReadPresetSeconds: (Int) -> Unit = {},
    onToggleShakeToClear: (Boolean) -> Unit = {},
    onSetShakeSensitivity: (String) -> Unit = {},
    onToggleShakeRequiresConfirmation: (Boolean) -> Unit = {},
    onSimulateShake: () -> Unit = {},
    onLockNow: () -> Unit = {},
    onTriggerWorkManagerCleanup: () -> Unit = {},
    onPanicWipe: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isEditingPin by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showPanicConfirmDialog by remember { mutableStateOf(false) }
    var showNotificationDiagnostics by remember { mutableStateOf(false) }

    // Duress PIN, Clipboard Auto-Purge & Privacy Curtain
    var isEditingDuressPin by remember { mutableStateOf(false) }
    var duressPinInput by remember { mutableStateOf("") }
    var duressPinError by remember { mutableStateOf<String?>(null) }
    var hasDuressPin by remember { mutableStateOf(SecurePrefsHelper.isDuressPinConfigured(context)) }

    var clipboardClearSeconds by remember { androidx.compose.runtime.mutableIntStateOf(SecurePrefsHelper.getClipboardClearSeconds(context)) }
    var privacyCurtainEnabled by remember { mutableStateOf(SecurePrefsHelper.isPrivacyCurtainEnabled(context)) }

    val securityPosture = remember(context) { com.example.util.security.DeviceIntegrityChecker.checkSecurityPosture(context) }
    val biometricStatus = remember(context) { BiometricAuthHelper.getBiometricStatusDescription(context) }

    val autoLockTimeoutOptions = listOf(
        1 to "1 min",
        5 to "5 min (Padrão)",
        10 to "10 min",
        15 to "15 min",
        30 to "30 min"
    )

    val clipboardTimeoutOptions = listOf(
        10 to "10s",
        30 to "30s (Padrão)",
        60 to "60s",
        0 to "Imediato"
    )

    // Tela de autodiagnostico de notificacoes (sobreposta)
    if (showNotificationDiagnostics) {
        NotificationDiagnosticsScreen(
            onBack = { showNotificationDiagnostics = false },
            onRequestPermission = onRequestNotificationPermission,
            onTestNotification = onTestNotification
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = RaixBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RaixSurface)
                    .statusBarsPadding()
                    .border(width = 1.dp, color = RaixBorder)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = RaixActionPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        PmsgLogoBadge(
                            size = 32.dp,
                            iconSize = 18.dp,
                            shapeRadius = 8.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Configurações",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RaixActionPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "COFRE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = RaixActionPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                            Text(
                                text = "Privacidade, Bloqueio e Criptografia",
                                fontSize = 11.sp,
                                color = RaixTextSecondary
                            )
                        }
                    }

                    // Direct Instant Lock Button
                    IconButton(
                        onClick = onLockNow,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(RaixSurfaceElevated)
                            .testTag("top_lock_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Bloquear Agora",
                            tint = RaixActionPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RaixActionPrimary.copy(alpha = 0.05f),
                            Color.Transparent,
                            RaixBackground
                        )
                    )
                )
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Security Shield Header Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, RaixActionPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(RaixActionPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = RaixActionPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proteção em Cascata de 512-bit (Dual-Layer)",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = RaixTextPrimary
                        )
                        Text(
                            text = if (isHardwareBackedCrypto) {
                                "Chaves duplas de 512 bits (2x256-bit) isoladas em silício seguro (TEE/StrongBox) com super-encriptação em cascata e destruição anti-forense em SQLite e RAM."
                            } else {
                                "Criptografia em cascata de 512 bits (Dual-Layer) ativa. Zero rastro em repouso e exclusão anti-forense em memória."
                            },
                            fontSize = 11.sp,
                            color = RaixTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hardware & OS Security Posture Diagnostic Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, RaixBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = RaixActionPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auditoria de Integridade do Dispositivo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaixTextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (securityPosture.isDeviceSecure) RaixActionPrimary.copy(alpha = 0.15f)
                                    else RaixErrorContainer
                                )
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (securityPosture.isDeviceSecure) "SEGURO" else "VULNERÁVEL",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (securityPosture.isDeviceSecure) RaixActionPrimary else RaixError
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Item 1: Hardware Keystore
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hardware KeyStore (TEE / StrongBox)",
                            fontSize = 11.5.sp,
                            color = RaixTextSecondary
                        )
                        Text(
                            text = if (securityPosture.hardwareKeyStoreSupported) "Ativo (Silício Seguro)" else "Software Keystore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (securityPosture.hardwareKeyStoreSupported) RaixActionPrimary else RaixError
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Item 2: Root Detection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Integridade do SO (Root / Magisk)",
                            fontSize = 11.5.sp,
                            color = RaixTextSecondary
                        )
                        Text(
                            text = if (!securityPosture.isRooted && !securityPosture.isTestKeysBuild) "Íntegro (Sem Root)" else "Dispositivo Modificado",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!securityPosture.isRooted && !securityPosture.isTestKeysBuild) RaixActionPrimary else RaixError
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Item 3: Anti-Debugging
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Proteção Anti-Debugging / Engenharia Reversa",
                            fontSize = 11.5.sp,
                            color = RaixTextSecondary
                        )
                        Text(
                            text = if (!securityPosture.isDebuggerAttached) "Protegido" else "Depurador Anexado!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!securityPosture.isDebuggerAttached) RaixActionPrimary else RaixError
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 1: BLOQUEIO & ACESSO
            SectionTitle(title = "SEGURANÇA & AUTO-BLOQUEIO")

            // Auto-Lock Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaixSurface)
                    .border(1.dp, if (autoLockEnabled) RaixActionPrimary.copy(alpha = 0.4f) else RaixBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RaixActionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = RaixActionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Auto-Bloqueio Automático",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaixTextPrimary
                            )
                            Text(
                                text = if (autoLockEnabled) "Bloqueia após $autoLockTimeoutMinutes min sem uso." else "Desativado.",
                                fontSize = 11.sp,
                                color = RaixTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = autoLockEnabled,
                        onCheckedChange = { onToggleAutoLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RaixBackground,
                            checkedTrackColor = RaixActionPrimary,
                            uncheckedThumbColor = RaixTextSecondary,
                            uncheckedTrackColor = RaixSurfaceElevated
                        ),
                        modifier = Modifier.testTag("auto_lock_switch")
                    )
                }

                if (autoLockEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TEMPO DE INATIVIDADE PARA BLOQUEIO:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = RaixActionPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        autoLockTimeoutOptions.forEach { (mins, label) ->
                            val isSelected = autoLockTimeoutMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) RaixActionPrimary else RaixSurfaceElevated)
                                    .border(
                                        0.8.dp,
                                        if (isSelected) RaixActionPrimary else RaixBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSetAutoLockTimeout(mins) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (mins == 5) "5m (Padrão)" else "${mins}m",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                    color = if (isSelected) RaixBackground else RaixTextPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Biometric Auth Card (Fingerprint & Face)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaixSurface)
                    .border(
                        1.dp,
                        if (biometricLockEnabled) RaixActionPrimary.copy(alpha = 0.4f) else RaixBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (biometricLockEnabled) RaixActionPrimary.copy(alpha = 0.2f) else RaixSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = if (biometricLockEnabled) RaixActionPrimary else RaixTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Biometria (Digital / Facial)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = "Facial",
                                    tint = if (biometricLockEnabled) RaixPremiumGold else RaixTextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = if (biometricLockEnabled) "Desbloqueio rápido por biometria ou PIN." else "Desativado: usa apenas PIN numérico.",
                                fontSize = 11.sp,
                                color = RaixTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = biometricLockEnabled,
                        onCheckedChange = { onToggleBiometricLock(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RaixBackground,
                            checkedTrackColor = RaixActionPrimary,
                            uncheckedThumbColor = RaixTextSecondary,
                            uncheckedTrackColor = RaixSurfaceElevated
                        ),
                        modifier = Modifier.testTag("biometric_lock_switch")
                    )
                }

                if (biometricLockEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RaixBackground.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(RaixActionPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Status de Hardware: $biometricStatus",
                            fontSize = 11.sp,
                            color = RaixTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Security PIN Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaixSurface)
                    .border(0.8.dp, RaixBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RaixActionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pin,
                                contentDescription = null,
                                tint = RaixActionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "PIN de Segurança",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaixTextPrimary
                            )
                            Text(
                                text = "Código numérico de 4 dígitos",
                                fontSize = 11.sp,
                                color = RaixTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isEditingPin = !isEditingPin
                            newPinInput = ""
                            pinError = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaixSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .border(0.8.dp, RaixActionPrimary, RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = if (isEditingPin) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = null,
                            tint = RaixActionPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEditingPin) "Cancelar" else "Alterar",
                            color = RaixActionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isEditingPin,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it
                                },
                                label = { Text("Novo PIN (4 dígitos)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaixActionPrimary,
                                    unfocusedBorderColor = RaixBorder,
                                    focusedTextColor = RaixTextPrimary,
                                    unfocusedTextColor = RaixTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (newPinInput.length == 4) {
                                        onSetSecurityPin(newPinInput)
                                        isEditingPin = false
                                        pinError = null
                                    } else {
                                        pinError = "O PIN deve ter 4 dígitos"
                                    }
                                },
                                enabled = newPinInput.length == 4,
                                colors = ButtonDefaults.buttonColors(containerColor = RaixActionPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    "Salvar",
                                    color = RaixBackground,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (pinError != null) {
                            Text(
                                text = pinError ?: "",
                                color = RaixError,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duress PIN Card (Anti-Coercion Panic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaixSurface)
                    .border(
                        1.dp,
                        if (hasDuressPin) RaixError.copy(alpha = 0.5f) else RaixBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RaixErrorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = RaixError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PIN de Coerção (Duress PIN)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                if (hasDuressPin) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(RaixErrorContainer)
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "ARMADO",
                                            color = RaixError,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Abre o app vazio e tritura todos os dados silenciosamente sob ameaça.",
                                fontSize = 11.sp,
                                color = RaixTextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isEditingDuressPin = !isEditingDuressPin
                            duressPinInput = ""
                            duressPinError = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RaixSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .border(0.8.dp, RaixBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = if (isEditingDuressPin) "Cancelar" else if (hasDuressPin) "Alterar" else "Configurar",
                            color = RaixActionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isEditingDuressPin,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = duressPinInput,
                                onValueChange = {
                                    if (it.length <= 4 && it.all { c -> c.isDigit() }) duressPinInput = it
                                },
                                label = { Text("PIN de Coerção (4 dígitos)", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RaixError,
                                    unfocusedBorderColor = RaixBorder,
                                    focusedTextColor = RaixTextPrimary,
                                    unfocusedTextColor = RaixTextPrimary
                                )
                            )

                            Button(
                                onClick = {
                                    if (duressPinInput.length == 4) {
                                        if (duressPinInput == securityPin) {
                                            duressPinError = "O PIN de Coerção deve ser DIFERENTE do PIN normal!"
                                        } else {
                                            SecurePrefsHelper.setDuressPin(context, duressPinInput)
                                            hasDuressPin = true
                                            isEditingDuressPin = false
                                            duressPinError = null
                                        }
                                    } else {
                                        duressPinError = "O PIN deve ter 4 dígitos"
                                    }
                                },
                                enabled = duressPinInput.length == 4,
                                colors = ButtonDefaults.buttonColors(containerColor = RaixError),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    "Salvar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (hasDuressPin) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    SecurePrefsHelper.clearDuressPin(context)
                                    hasDuressPin = false
                                    isEditingDuressPin = false
                                }
                            ) {
                                Text("Remover PIN de Coerção", color = RaixError, fontSize = 11.sp)
                            }
                        }

                        if (duressPinError != null) {
                            Text(
                                text = duressPinError ?: "",
                                color = RaixError,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clipboard Auto-Purge Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaixSurface)
                    .border(1.dp, RaixBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(RaixActionPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = RaixActionPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Auto-Limpeza da Área de Transferência",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = RaixTextPrimary
                        )
                        Text(
                            text = "Apaga textos copiados do clipboard do sistema para evitar espionagem por outros aplicativos.",
                            fontSize = 11.sp,
                            color = RaixTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    clipboardTimeoutOptions.forEach { (secs, label) ->
                        val isSelected = clipboardClearSeconds == secs
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) RaixActionPrimary
                                    else RaixSurfaceElevated
                                )
                                .border(
                                    0.8.dp,
                                    if (isSelected) RaixActionPrimary else RaixBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    SecurePrefsHelper.setClipboardClearSeconds(context, secs)
                                    clipboardClearSeconds = secs
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                color = if (isSelected) RaixBackground else RaixTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy Curtain Card (App Switcher Concealment)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RaixSurface)
                    .border(
                        1.dp,
                        if (privacyCurtainEnabled) RaixActionPrimary.copy(alpha = 0.4f) else RaixBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RaixActionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = RaixActionPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cortina no Alternador de Aplicativos",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaixTextPrimary
                            )
                            Text(
                                text = if (privacyCurtainEnabled) "Oculta miniatura no alternador de apps recentes." else "Miniatura visível no sistema.",
                                fontSize = 11.sp,
                                color = RaixTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = privacyCurtainEnabled,
                        onCheckedChange = {
                            SecurePrefsHelper.setPrivacyCurtainEnabled(context, it)
                            privacyCurtainEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RaixBackground,
                            checkedTrackColor = RaixActionPrimary,
                            uncheckedThumbColor = RaixTextSecondary,
                            uncheckedTrackColor = RaixSurfaceElevated
                        ),
                        modifier = Modifier.testTag("privacy_curtain_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 2: PRIVACIDADE & SISTEMA
            SectionTitle(title = "PRIVACIDADE & NOTIFICAÇÕES")

            // Read Receipts & Disappearing Effect Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurface),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, if (readReceiptsEnabled) RaixActionPrimary.copy(alpha = 0.4f) else RaixBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (readReceiptsEnabled) RaixActionPrimary.copy(alpha = 0.15f) else RaixSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = if (readReceiptsEnabled) RaixActionPrimary else RaixTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Confirmação de Leitura",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Text(
                                    text = if (readReceiptsEnabled) "Exibe check duplo e efeito de desintegração ao ler." else "Status de leitura desativado.",
                                    fontSize = 11.sp,
                                    color = RaixTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = readReceiptsEnabled,
                            onCheckedChange = { onToggleReadReceipts(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RaixBackground,
                                checkedTrackColor = RaixActionPrimary,
                                uncheckedThumbColor = RaixTextSecondary,
                                uncheckedTrackColor = RaixSurfaceElevated
                            )
                        )
                    }

                    if (readReceiptsEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "DESAPARECER APÓS LEITURA (EFEITO VISUAL):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = RaixActionPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val vanishOptions = listOf(
                            0 to "Desativado",
                            5 to "5s (Rápido)",
                            10 to "10s (Padrão)",
                            30 to "30s",
                            60 to "60s"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            vanishOptions.forEach { (secs, label) ->
                                val isSelected = vanishAfterReadPresetSeconds == secs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) RaixActionPrimary else RaixSurfaceElevated)
                                        .border(
                                            0.8.dp,
                                            if (isSelected) RaixActionPrimary else RaixBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSetVanishAfterReadPresetSeconds(secs) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (secs == 0) "Off" else "${secs}s",
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                        color = if (isSelected) RaixBackground else RaixTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (vanishAfterReadPresetSeconds > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = RaixError,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mensagens lidas desintegram e evaporam em $vanishAfterReadPresetSeconds segundos com partículas.",
                                    fontSize = 10.sp,
                                    color = RaixError
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Anti-screenshot Switch (FLAG_SECURE)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurface),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RaixActionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoPhotography,
                                contentDescription = null,
                                tint = if (screenProtectionEnabled) RaixActionPrimary else RaixTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Anti-Captura (FLAG_SECURE)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = RaixTextPrimary
                            )
                            Text(
                                text = if (screenProtectionEnabled) "Ativo: bloqueia gravação e print da tela." else "Desativado para permitir prints no sistema.",
                                fontSize = 11.sp,
                                color = RaixTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = screenProtectionEnabled,
                        onCheckedChange = { onToggleScreenProtection() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RaixBackground,
                            checkedTrackColor = RaixActionPrimary,
                            uncheckedThumbColor = RaixTextSecondary,
                            uncheckedTrackColor = RaixSurfaceElevated
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-time Screenshot Detector Switch & Simulator
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurface),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, if (screenshotDetectionEnabled) RaixActionPrimary.copy(alpha = 0.4f) else RaixBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (screenshotDetectionEnabled) RaixActionPrimary.copy(alpha = 0.15f) else RaixSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = if (screenshotDetectionEnabled) RaixActionPrimary else RaixTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Detector de Screenshots",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Text(
                                    text = if (screenshotDetectionEnabled) "Alerta nos chats quando prints forem tirados (Android 14+ / MediaStore)." else "Detecção desativada.",
                                    fontSize = 11.sp,
                                    color = RaixTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = screenshotDetectionEnabled,
                            onCheckedChange = { onToggleScreenshotDetection(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RaixBackground,
                                checkedTrackColor = RaixActionPrimary,
                                uncheckedThumbColor = RaixTextSecondary,
                                uncheckedTrackColor = RaixSurfaceElevated
                            )
                        )
                    }

                    if (screenshotDetectionEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = RaixBorder.copy(alpha = 0.5f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Sensitive Content Lockdown on Screenshot
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (blockSensitiveOnScreenshot) RaixError.copy(alpha = 0.15f) else RaixSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (blockSensitiveOnScreenshot) RaixError else RaixTextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bloqueio de Conteúdo Sensível",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = RaixTextPrimary
                                    )
                                    Text(
                                        text = if (blockSensitiveOnScreenshot) "Bloqueia fotos 1x e notas secretas ao detectar print." else "Não bloqueia conteúdos.",
                                        fontSize = 11.sp,
                                        color = RaixTextSecondary
                                    )
                                }
                            }
                            Switch(
                                checked = blockSensitiveOnScreenshot,
                                onCheckedChange = { onToggleBlockSensitiveOnScreenshot(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RaixBackground,
                                    checkedTrackColor = RaixError,
                                    uncheckedThumbColor = RaixTextSecondary,
                                    uncheckedTrackColor = RaixSurfaceElevated
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Test Simulation Button
                        OutlinedButton(
                            onClick = onSimulateScreenshot,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixError.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RaixError, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simular Detecção de Print (Teste)", color = RaixError, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Shake to Clear Configuration Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurface),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, if (shakeToClearEnabled) RaixActionPrimary.copy(alpha = 0.4f) else RaixBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (shakeToClearEnabled) RaixActionPrimary.copy(alpha = 0.15f) else RaixSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = if (shakeToClearEnabled) RaixActionPrimary else RaixTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Shake to Clear (Chacoalhar)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Text(
                                    text = if (shakeToClearEnabled) "Chacoalhe para limpar instantaneamente o chat aberto." else "Detecção por acelerômetro desativada.",
                                    fontSize = 11.sp,
                                    color = RaixTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = shakeToClearEnabled,
                            onCheckedChange = { onToggleShakeToClear(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RaixBackground,
                                checkedTrackColor = RaixActionPrimary,
                                uncheckedThumbColor = RaixTextSecondary,
                                uncheckedTrackColor = RaixSurfaceElevated
                            )
                        )
                    }

                    if (shakeToClearEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SENSIBILIDADE DO MOVIMENTO:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = RaixActionPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val sensitivityOptions = listOf(
                            "HIGH" to "Alta (Leve)",
                            "NORMAL" to "Média (Padrão)",
                            "LOW" to "Baixa (Forte)"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sensitivityOptions.forEach { (key, label) ->
                                val isSelected = shakeSensitivity == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) RaixActionPrimary else RaixSurfaceElevated)
                                        .border(
                                            0.8.dp,
                                            if (isSelected) RaixActionPrimary else RaixBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSetShakeSensitivity(key) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Medium,
                                        color = if (isSelected) RaixBackground else RaixTextPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Confirmation Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pedir confirmação antes de apagar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RaixTextPrimary
                                )
                                Text(
                                    text = if (shakeRequiresConfirmation) "Exibe aviso antes de apagar o chat." else "Limpeza instantânea sem diálogo (Zero Trace).",
                                    fontSize = 10.sp,
                                    color = RaixTextSecondary
                                )
                            }
                            Switch(
                                checked = shakeRequiresConfirmation,
                                onCheckedChange = { onToggleShakeRequiresConfirmation(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = RaixBackground,
                                    checkedTrackColor = RaixActionPrimary,
                                    uncheckedThumbColor = RaixTextSecondary,
                                    uncheckedTrackColor = RaixSurfaceElevated
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Simulation Button
                        OutlinedButton(
                            onClick = onSimulateShake,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixActionPrimary.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RaixActionPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simular Chacoalhar (Teste de Limpeza)", color = RaixActionPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notification Alerts Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurface),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (notificationsEnabled) RaixActionPrimary.copy(alpha = 0.15f) else RaixSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (notificationsEnabled) RaixActionPrimary else RaixTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Permissão de Notificações",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Text(
                                    text = if (notificationsEnabled) "Autorizado no sistema Android" else "Desativadas no sistema.",
                                    fontSize = 11.sp,
                                    color = RaixTextSecondary
                                )
                            }
                        }

                        if (!notificationsEnabled) {
                            Button(
                                onClick = onRequestNotificationPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = RaixActionPrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("enable_notifications_button")
                            ) {
                                Text("Ativar", color = RaixBackground, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onTestNotification,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixActionPrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("test_notification_button")
                            ) {
                                Text("Testar", color = RaixActionPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = RaixBorder, thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Toggle: Notificar quando uma nova conversa for iniciada (Desativado por padrão)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notificar novas conversas iniciadas",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RaixTextPrimary
                            )
                            Text(
                                text = if (notifyOnNewConversation)
                                    "Avisar na chegada de nova conversa."
                                else
                                    "Silencioso: Não notificar quando uma nova conversa for iniciada.",
                                fontSize = 11.sp,
                                color = if (notifyOnNewConversation) RaixActionPrimary else RaixTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = notifyOnNewConversation,
                            onCheckedChange = onToggleNotifyOnNewConversation,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RaixActionPrimary,
                                checkedTrackColor = RaixActionPrimary.copy(alpha = 0.3f),
                                uncheckedThumbColor = RaixTextSecondary,
                                uncheckedTrackColor = RaixSurfaceElevated
                            ),
                            modifier = Modifier.testTag("notify_on_new_conversation_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = RaixBorder, thickness = 0.6.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Botao: Diagnostico de Notificacoes
                    OutlinedButton(
                        onClick = { showNotificationDiagnostics = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("open_notification_diagnostics_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixBorder),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = RaixTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Diagnostico de Notificacoes",
                            color = RaixTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 3: INFORMAÇÕES DE PROTOCOLO
            SectionTitle(title = "ARQUITETURA & EXPIRAÇÃO")

            InfoProtocolCard(
                icon = Icons.Default.AutoDelete,
                title = "Autodestruição & WorkManager Purge",
                description = "O WorkManager periódico (ciclo de 15m) e Room limpam automaticamente mensagens expiradas e mídias descartadas do banco com zero rastro."
            )

            Spacer(modifier = Modifier.height(10.dp))

            // WorkManager Periodic Purge Controller Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaixSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixBorder)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(RaixPremiumGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = RaixPremiumGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Limpeza Room (WorkManager)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RaixTextPrimary
                                )
                                Text(
                                    text = "Status: Agendado e ativo em segundo plano (cada 15m)",
                                    fontSize = 11.sp,
                                    color = RaixActionPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onTriggerWorkManagerCleanup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("trigger_workmanager_cleanup_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixPremiumGold.copy(alpha = 0.8f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = RaixPremiumGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Executar Varredura de Limpeza Agora",
                            color = RaixPremiumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            InfoProtocolCard(
                icon = Icons.Default.VpnKey,
                title = "Zero Logs & Criptografia Local",
                description = "As chaves e mensagens ficam salvas somente no dispositivo em sandbox criptografada."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: AÇÕES RÁPIDAS
            SectionTitle(title = "AÇÕES DE PRIVACIDADE")

            // Lock App Now
            OutlinedButton(
                onClick = onLockNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("lock_app_now_button"),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, RaixActionPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = RaixActionPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bloquear Aplicativo Agora",
                    fontWeight = FontWeight.Medium,
                    color = RaixActionPrimary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Panic Wipe Button
            Button(
                onClick = { showPanicConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("panic_wipe_button_settings"),
                colors = ButtonDefaults.buttonColors(containerColor = RaixError),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFF5A110D),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Incinerar Tudo Agora (Wipe Total)",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF5A110D),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // App version display
            val packageInfo = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (_: Exception) { null }
            }
            val versionText = remember(packageInfo) {
                val name = packageInfo?.versionName ?: "?"
                val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode?.toString() ?: "?"
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toString() ?: "?"
                }
                "Raix v$name (build $code)"
            }
            Text(
                text = versionText,
                modifier = Modifier.fillMaxWidth().testTag("app_version_label"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 11.sp,
                color = RaixTextSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showPanicConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPanicConfirmDialog = false },
            containerColor = RaixSurface,
            titleContentColor = RaixError,
            icon = {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = RaixError,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Protocolo de Emergência (Wipe)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Atenção: Esta ação irá apagar instantaneamente todas as mensagens, mídias, áudios e canais locais com sobrescrita criptográfica permanente.\n\nEsta operação NÃO pode ser desfeita.",
                    color = RaixTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicConfirmDialog = false
                        onPanicWipe()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RaixError)
                ) {
                    Text("VAPORIZAR TUDO AGORA", fontWeight = FontWeight.Medium, color = Color(0xFF601410))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicConfirmDialog = false }) {
                    Text("Cancelar", color = RaixTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = RaixActionPrimary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun InfoProtocolCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = RaixSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(0.8.dp, RaixBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RaixActionPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = RaixActionPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RaixTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = RaixTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
