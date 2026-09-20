package com.example.security

import com.example.ui.screens.EphemeralUiMessage

expect object MessageStorage {
    fun loadMessages(contactFingerprint: String): List<EphemeralUiMessage>
    fun saveMessages(contactFingerprint: String, messages: List<EphemeralUiMessage>)
    fun clearMessages(contactFingerprint: String)
}
