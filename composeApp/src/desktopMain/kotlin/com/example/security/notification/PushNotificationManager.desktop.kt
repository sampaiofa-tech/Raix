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

    var sharedTrayIcon: TrayIcon? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    actual fun getPushToken(): String? {
        return null
    }

    actual fun showLocalNotification(title: String, body: String, messageId: String?) {
        println("[PushNotificationManager] showLocalNotification chamado. title: $title, body: $body")
        scope.launch {
            showSystemTrayNotification(title, body, messageId)
        }
    }

    actual fun hasPermission(): Boolean {
        return true
    }

    private val shownNotifications = mutableSetOf<String>()

    actual fun getClickedNotificationMessageId(): String? {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (!isWindows) return null

        try {
            val appData = System.getenv("APPDATA") ?: return null
            val signalFile = java.io.File(appData, "Pmsg/toast_signal.txt")
            if (signalFile.exists()) {
                val messageId = signalFile.readText().trim()
                signalFile.delete()
                if (messageId.isNotEmpty()) {
                    return messageId
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun showSystemTrayNotification(title: String, body: String, messageId: String?) {
        println("[PushNotificationManager] showSystemTrayNotification invocado. title=$title")
        
        // Dedup: só exibe se o messageId for nulo ou ainda não foi exibido.
        if (messageId != null && !shownNotifications.add(messageId)) {
            println("[PushNotificationManager] Notificação ignorada (já exibida): $messageId")
            return
        }
        
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows) {
            try {
                val safeTitle = escapePowerShell(title)
                val safeBody = escapePowerShell(body)
                
                val psCommand = """
                    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null;
                    ${'$'}template = [Windows.UI.Notifications.ToastTemplateType]::ToastText02;
                    ${'$'}xml = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent(${'$'}template);
                    ${'$'}texts = ${'$'}xml.GetElementsByTagName('text');
                    ${'$'}texts.Item(0).AppendChild(${'$'}xml.CreateTextNode('$safeTitle')) | Out-Null;
                    ${'$'}texts.Item(1).AppendChild(${'$'}xml.CreateTextNode('$safeBody')) | Out-Null;
                    ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}xml);
                    ${'$'}notifier = [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('Raix');
                    ${'$'}notifier.Show(${'$'}toast);
                """.trimIndent().replace('\n', ' ')
                
                val process = ProcessBuilder("powershell", "-ExecutionPolicy", "Bypass", "-Command", psCommand).start()
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Retorna early no Windows para não duplicar com o TrayIcon, que não funciona direito.
            return
        }

        // Fallback: AWT TrayIcon (Para Linux/Mac se houver)
        try {
            if (!SystemTray.isSupported()) {
                println("[PushNotificationManager] SystemTray não é suportado!")
                return
            }
            val tray = SystemTray.getSystemTray()

            val iconToUse = sharedTrayIcon ?: run {
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
                TrayIcon(image, "Raix").apply {
                    isImageAutoSize = true
                    tray.add(this)
                }
            }

            iconToUse.displayMessage(title, body, TrayIcon.MessageType.INFO)
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
