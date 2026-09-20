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
    
    val trayState = rememberTrayState()
    
    // Register the trayState so PushNotificationManager can use it
    LaunchedEffect(trayState) {
        PushNotificationManager.composeTrayState = trayState
    }

    Tray(
        icon = appIcon,
        tooltip = "Raix",
        state = trayState,
        onAction = { isWindowVisible = true },
        menu = {
            Item("Abrir Raix", onClick = { isWindowVisible = true })
            Item("Sair", onClick = ::exitApplication)
        }
    )

    Window(
        onCloseRequest = { isWindowVisible = false },
        title = windowTitle,
        state = windowState,
        icon = appIcon,
        alwaysOnTop = true
    ) {
        LaunchedEffect(isWindowVisible) {
            window.isVisible = isWindowVisible
        }
        App()
    }
}

