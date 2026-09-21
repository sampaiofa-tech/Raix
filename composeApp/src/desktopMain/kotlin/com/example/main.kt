package com.example

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.Notification
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
    
    val trayState = rememberTrayState()
    
    Tray(
        state = trayState,
        icon = appIcon,
        tooltip = "Raix",
        onAction = {
            isWindowVisible = true
            windowState.isMinimized = false
        },
        menu = {
            Item("Abrir Raix", onClick = {
                isWindowVisible = true
                windowState.isMinimized = false
            })
            Item("Sair", onClick = ::exitApplication)
        }
    )

    LaunchedEffect(trayState) {
        com.example.security.notification.PushNotificationManager.sharedTrayState = trayState
    }

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

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(8000)
        println("[DIAGNOSTICO] TESTE: Minimizando a janela...")
        windowState.isMinimized = true
        
        kotlinx.coroutines.delay(8000)
        println("[DIAGNOSTICO] TESTE: Fechando a janela (enviando para a bandeja)...")
        isWindowVisible = false
        
        kotlinx.coroutines.delay(4000)
        println("[DIAGNOSTICO] TESTE: Enviando notificacao de teste...")
        com.example.security.notification.PushNotificationManager.showLocalNotification(
            "Teste de Bandeja",
            "Esta é uma mensagem de teste na bandeja",
            "teste-id-123"
        )
        
        kotlinx.coroutines.delay(6000)
        println("[DIAGNOSTICO] TESTE: Teste finalizado.")
    }

    Window(
        onCloseRequest = { isWindowVisible = false },
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

