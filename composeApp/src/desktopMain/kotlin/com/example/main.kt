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
    var isWindowVisible by remember { mutableStateOf(true) }
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
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isWindowVisible = true
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
        icon = appIcon,
        alwaysOnTop = true,
        visible = isWindowVisible
    ) {
        appWindow = this.window
        
        LaunchedEffect(isWindowVisible, windowState.isMinimized) {
            if (isWindowVisible && !windowState.isMinimized) {
                appWindow?.toFront()
                appWindow?.requestFocus()
            }
            println("[DIAGNOSTICO] Estado da Janela: visible=$isWindowVisible, minimized=${windowState.isMinimized}")
        }
        
        App()
    }
}

