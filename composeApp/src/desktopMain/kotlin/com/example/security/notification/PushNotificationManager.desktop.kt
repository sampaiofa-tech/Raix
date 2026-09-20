package com.example.security.notification

import androidx.compose.ui.window.TrayState
import androidx.compose.ui.window.Notification
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual object PushNotificationManager {

    var composeTrayState: TrayState? = null
    private var trayIcon: TrayIcon? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    actual fun getPushToken(): String? {
        return null
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        // (1) Compose native Tray notification
        var composeSuccess = false
        try {
            composeTrayState?.let {
                it.sendNotification(
                    Notification(
                        title = title,
                        message = body,
                        type = Notification.Type.Info
                    )
                )
                composeSuccess = true
            }
        } catch (_: Throwable) {}

        // (2) Fallback AWT TrayIcon
        if (!composeSuccess) {
            scope.launch {
                showSystemTrayNotification(title, body)
            }
        }
    }

    actual fun hasPermission(): Boolean {
        return true
    }

    private fun showSystemTrayNotification(title: String, body: String) {
        try {
            if (!SystemTray.isSupported()) return
            val tray = SystemTray.getSystemTray()

            if (trayIcon == null) {
                val resource = PushNotificationManager::class.java.getResource("/icon.png")
                val image = if (resource != null) {
                    Toolkit.getDefaultToolkit().createImage(resource)
                } else {
                    val fallback = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                    val g = fallback.createGraphics()
                    g.color = java.awt.Color.BLUE
                    g.fillRect(0, 0, 16, 16)
                    g.dispose()
                    fallback
                }
                trayIcon = TrayIcon(image, "Raix").apply {
                    isImageAutoSize = true
                    tray.add(this)
                }
            }

            trayIcon?.displayMessage(title, body, TrayIcon.MessageType.INFO)
        } catch (_: Throwable) {
        }
    }

    private fun escapePowerShell(str: String): String {
        return str.replace("'", "''")
            .replace("`", "``")
            .replace("$", "`$")
            .replace("\n", "`n")
            .replace("\r", "")
    }
}
