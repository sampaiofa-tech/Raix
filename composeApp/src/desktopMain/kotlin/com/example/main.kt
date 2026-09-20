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
    
    var trayIcon: java.awt.TrayIcon? = null
    var systemTray: java.awt.SystemTray? = null
    
    LaunchedEffect(Unit) {
        if (java.awt.SystemTray.isSupported()) {
            systemTray = java.awt.SystemTray.getSystemTray()
            val resource = object {}.javaClass.getResource("/icon.png")
            val image = if (resource != null) {
                java.awt.Toolkit.getDefaultToolkit().createImage(resource)
            } else {
                val fallback = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                val g = fallback.createGraphics()
                g.color = java.awt.Color.BLUE
                g.fillRect(0, 0, 16, 16)
                g.dispose()
                fallback
            }
            trayIcon = java.awt.TrayIcon(image, "Raix").apply {
                isImageAutoSize = true
                addActionListener {
                    isWindowVisible = true
                }
                addMouseListener(object : java.awt.event.MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent) {
                        if (e.button == java.awt.event.MouseEvent.BUTTON1) {
                            isWindowVisible = true
                        }
                    }
                })
                
                val popup = java.awt.PopupMenu()
                val openItem = java.awt.MenuItem("Abrir Raix")
                openItem.addActionListener {
                    isWindowVisible = true
                }
                val exitItem = java.awt.MenuItem("Sair")
                exitItem.addActionListener {
                    exitApplication()
                }
                popup.add(openItem)
                popup.add(exitItem)
                popupMenu = popup
            }
            systemTray?.add(trayIcon)
            com.example.security.notification.PushNotificationManager.sharedTrayIcon = trayIcon
        }
    }

    LaunchedEffect(Unit) {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            val appData = System.getenv("APPDATA") ?: return@LaunchedEffect
            val signalFile = java.io.File(appData, "Pmsg/toast_signal.txt")
            var lastModified = 0L
            while (true) {
                if (signalFile.exists() && signalFile.lastModified() != lastModified) {
                    lastModified = signalFile.lastModified()
                    isWindowVisible = true
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Window(
        onCloseRequest = { isWindowVisible = false },
        title = windowTitle,
        state = windowState,
        icon = appIcon,
        alwaysOnTop = true,
        visible = isWindowVisible
    ) {
        App()
    }
}

