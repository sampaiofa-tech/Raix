package com.example

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberWindowState
import com.example.security.notification.PushNotificationManager

fun main() = application {
    com.example.util.VersionMigrationManagerDesktop.checkAndWipeOnUpdate(com.example.DesktopBuildConfig.APP_VERSION)
    val isDev = System.getProperty("raix.dev") == "true" || System.getenv("RAIX_DEV") == "true"
    val windowTitle = if (isDev) "Raix [desktop-dev]" else "Raix"
    
    val windowState = rememberWindowState(size = DpSize(450.dp, 800.dp))

    val appIcon = painterResource("icon.png")
    var appWindow: java.awt.Window? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val appData = System.getenv("APPDATA") ?: return@withContext
                val signalFile = java.io.File(appData, "Pmsg/toast_signal.txt")
                var lastModified = 0L
                while (true) {
                    if (signalFile.exists() && signalFile.lastModified() != lastModified) {
                        lastModified = signalFile.lastModified()
                        val messageId = signalFile.readText().trim()
                        signalFile.delete()
                        lastModified = 0L
                        if (messageId.isNotEmpty()) {
                            PushNotificationManager.pendingClickedMessageId = messageId
                            println("[DIAGNOSTICO] main.kt: toast_signal.txt lido -> messageId=$messageId")
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            windowState.isMinimized = false
                        }
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }
        }
    }

    Window(
        onCloseRequest = { windowState.isMinimized = true },
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
            println("[DIAGNOSTICO] Estado da Janela: minimized=${windowState.isMinimized}")
        }
        
        App()
    }
}

