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
        try {
            composeTrayState?.sendNotification(
                Notification(
                    title = title,
                    message = body,
                    type = Notification.Type.Info
                )
            )
        } catch (_: Throwable) {}

        // (2) WinRT PowerShell Toast (as requested: ambos)
        scope.launch {
            val os = System.getProperty("os.name")?.lowercase() ?: ""
            if (os.contains("win")) {
                val shown = showWindowsWinRtToast(title, body)
                if (!shown) {
                    showSystemTrayNotification(title, body)
                }
            } else {
                showSystemTrayNotification(title, body)
            }
        }
    }

    actual fun hasPermission(): Boolean {
        return true
    }

    private fun showWindowsWinRtToast(title: String, body: String): Boolean {
        return try {
            val escapedTitle = escapePowerShell(title)
            val escapedBody = escapePowerShell(body)
            val script = "" +
                "[Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > \$null\n" +
                "\$template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)\n" +
                "\$textNodes = \$template.GetElementsByTagName('text')\n" +
                "\$textNodes.Item(0).AppendChild(\$template.CreateTextNode('$escapedTitle')) > \$null\n" +
                "\$textNodes.Item(1).AppendChild(\$template.CreateTextNode('$escapedBody')) > \$null\n" +
                "\$toast = [Windows.UI.Notifications.ToastNotification]::new(\$template)\n" +
                "[Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('{1AC14E77-02E7-4E5D-B744-2EB1AE5198B7}\\WindowsPowerShell\\v1.0\\powershell.exe').Show(\$toast)"

            val process = ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
                .redirectErrorStream(true)
                .start()
            val exited = process.waitFor()
            exited == 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun showSystemTrayNotification(title: String, body: String) {
        try {
            if (!SystemTray.isSupported()) return
            val tray = SystemTray.getSystemTray()

            if (trayIcon == null) {
                val image = Toolkit.getDefaultToolkit().createImage(
                    PushNotificationManager::class.java.getResource("/icon.png")
                ) ?: java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
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
        return str.replace("'", "''").replace("`", "``").replace("$", "$")
    }
}
