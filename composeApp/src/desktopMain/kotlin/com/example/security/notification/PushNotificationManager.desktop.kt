package com.example.security.notification

import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon

actual object PushNotificationManager {

    private var trayIcon: TrayIcon? = null

    actual fun getPushToken(): String? {
        // Windows Desktop does not use remote cloud push services (operates 100% offline without WNS)
        return null
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        val os = System.getProperty("os.name")?.lowercase() ?: ""
        if (os.contains("win")) {
            val shown = showWindowsWinRtToast(title, body)
            if (shown) return
        }

        // Fallback to AWT SystemTray
        showSystemTrayNotification(title, body)
    }

    actual fun hasPermission(): Boolean {
        // Desktop platforms allow notifications by default
        return true
    }

    /**
     * Dispatches native Windows toast notifications using Windows.UI.Notifications (WinRT)
     * without any dependency on Microsoft WNS cloud servers.
     */
    private fun showWindowsWinRtToast(title: String, body: String): Boolean {
        return try {
            val escapedTitle = escapePowerShell(title)
            val escapedBody = escapePowerShell(body)
            val script = """
                [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > ${'$'}null
                ${'$'}template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)
                ${'$'}textNodes = ${'$'}template.GetElementsByTagName('text')
                ${'$'}textNodes.Item(0).AppendChild(${'$'}template.CreateTextNode('$escapedTitle')) > ${'$'}null
                ${'$'}textNodes.Item(1).AppendChild(${'$'}template.CreateTextNode('$escapedBody')) > ${'$'}null
                ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}template)
                [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Raix').Show(${'$'}toast)
            """.trimIndent()

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
            // Safe fallback
        }
    }

    private fun escapePowerShell(str: String): String {
        return str.replace("'", "''").replace("`", "``").replace("$", "`$")
    }
}
