package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.security.notification.PushNotificationManager
import kotlinx.coroutines.delay

fun main() = application {
    com.example.util.VersionMigrationManagerDesktop.checkAndWipeOnUpdate(com.example.DesktopBuildConfig.APP_VERSION)
    val isDev = System.getProperty("raix.dev") == "true" || System.getenv("RAIX_DEV") == "true"
    val windowTitle = if (isDev) "Raix [desktop-dev]" else "Raix"

    val windowState = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    val appIcon = painterResource("icon.png")
    var appWindow: java.awt.Window? by remember { mutableStateOf(null) }

    // ── Main window: X = encerrar o app ──
    Window(
        onCloseRequest = { exitApplication() },
        title = windowTitle,
        state = windowState,
        icon = appIcon
    ) {
        appWindow = this.window

        LaunchedEffect(windowState.isMinimized) {
            if (!windowState.isMinimized) {
                appWindow?.toFront()
                appWindow?.requestFocus()
            }
            println("[LIFECYCLE] Estado da Janela: minimized=${windowState.isMinimized}")
        }

        App()
    }

    // ── Mini-janela de notificação (canto superior direito) ──
    val notification = PushNotificationManager.pendingNotification
    if (notification != null) {
        val notifState = rememberWindowState(
            size = DpSize(320.dp, 90.dp),
            position = WindowPosition.PlatformDefault
        )

        Window(
            onCloseRequest = { PushNotificationManager.pendingNotification = null },
            state = notifState,
            title = "Raix",
            undecorated = true,
            alwaysOnTop = true,
            focusable = false,
            resizable = false
        ) {
            // Posicionar no canto superior direito da tela
            LaunchedEffect(Unit) {
                try {
                    val gc = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .defaultScreenDevice.defaultConfiguration
                    val insets = java.awt.Toolkit.getDefaultToolkit().getScreenInsets(gc)
                    val bounds = gc.bounds
                    window.setLocation(
                        bounds.x + bounds.width - window.width - 16 - insets.right,
                        bounds.y + insets.top + 16
                    )
                } catch (e: Exception) {
                    println("[NOTIF] Falha ao posicionar mini-janela: ${e.message}")
                }
            }

            // Auto-dispensa após 5 segundos
            LaunchedEffect(notification) {
                delay(5_000L)
                PushNotificationManager.pendingNotification = null
            }

            // Conteúdo da mini-janela — paleta existente
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val fingerprint = notification.contactFingerprint
                        if (!fingerprint.isNullOrEmpty()) {
                            PushNotificationManager.pendingClickedMessageId = fingerprint
                        }
                        PushNotificationManager.pendingNotification = null
                        windowState.isMinimized = false
                        appWindow?.toFront()
                        appWindow?.requestFocus()
                    },
                color = Color(0xFF1E2432),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicador verde (ícone)
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF00E676), CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "RAIX",
                            color = Color(0xFF00E676),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Nova mensagem criptografada",
                            color = Color(0xFFF5F7FA),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
