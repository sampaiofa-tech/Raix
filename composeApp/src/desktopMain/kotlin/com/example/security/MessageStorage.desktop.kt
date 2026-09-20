package com.example.security

import com.example.ui.screens.EphemeralUiMessage
import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.serialization.json.Json
import java.io.File
import kotlinx.serialization.builtins.ListSerializer

actual object MessageStorage {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun getStorageFile(contactFingerprint: String): File {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg").apply { if (!exists()) mkdirs() }
        val safeName = contactFingerprint.replace(Regex("[^a-zA-Z0-9]"), "_")
        return File(dir, "msg_$safeName.dpapi")
    }

    actual fun loadMessages(contactFingerprint: String): List<EphemeralUiMessage> {
        val file = getStorageFile(contactFingerprint)
        if (!file.exists()) return emptyList()
        return try {
            val rawJson = if (Platform.isWindows()) {
                val encrypted = file.readBytes()
                val decrypted = Crypt32Util.cryptUnprotectData(encrypted)
                String(decrypted, Charsets.UTF_8)
            } else {
                file.readText()
            }
            json.decodeFromString(ListSerializer(EphemeralUiMessage.serializer()), rawJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual fun saveMessages(contactFingerprint: String, messages: List<EphemeralUiMessage>) {
        val file = getStorageFile(contactFingerprint)
        if (messages.isEmpty()) {
            if (file.exists()) file.delete()
            return
        }
        try {
            val rawJson = json.encodeToString(ListSerializer(EphemeralUiMessage.serializer()), messages)
            if (Platform.isWindows()) {
                val encrypted = Crypt32Util.cryptProtectData(rawJson.toByteArray(Charsets.UTF_8))
                file.writeBytes(encrypted)
            } else {
                file.writeText(rawJson)
            }
        } catch (_: Exception) {
            // Fallback
        }
    }

    actual fun clearMessages(contactFingerprint: String) {
        val file = getStorageFile(contactFingerprint)
        if (file.exists()) {
            try {
                file.delete()
            } catch (_: Exception) {}
        }
    }
}
