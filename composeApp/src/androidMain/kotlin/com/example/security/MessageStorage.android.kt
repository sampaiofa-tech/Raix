package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.screens.EphemeralUiMessage
import com.example.util.AndroidContextHolder
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

actual object MessageStorage {
    private const val PREFS_NAME = "pmsg_ephemeral_msgs"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun getPrefs(): SharedPreferences? {
        return AndroidContextHolder.appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual fun loadMessages(contactFingerprint: String): List<EphemeralUiMessage> {
        val prefs = getPrefs() ?: return emptyList()
        val rawJson = prefs.getString("msg_$contactFingerprint", null) ?: return emptyList()
        return try {
            json.decodeFromString(ListSerializer(EphemeralUiMessage.serializer()), rawJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    actual fun saveMessages(contactFingerprint: String, messages: List<EphemeralUiMessage>) {
        val prefs = getPrefs() ?: return
        if (messages.isEmpty()) {
            prefs.edit().remove("msg_$contactFingerprint").apply()
            return
        }
        try {
            val rawJson = json.encodeToString(ListSerializer(EphemeralUiMessage.serializer()), messages)
            prefs.edit().putString("msg_$contactFingerprint", rawJson).apply()
        } catch (_: Exception) {}
    }

    actual fun clearMessages(contactFingerprint: String) {
        val prefs = getPrefs() ?: return
        prefs.edit().remove("msg_$contactFingerprint").apply()
    }
}
