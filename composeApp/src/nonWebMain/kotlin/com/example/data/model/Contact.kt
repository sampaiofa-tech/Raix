package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity representing an ephemeral contact locally.
 * Critical security requirement: displayNameEncrypted is stored encrypted at rest
 * to prevent plaintext forensic analysis on SQLite.
 *
 * v1.8.0: Added local metadata fields (nickname, isFavorite, category).
 * These are local-only, encrypted at rest, never sent to server.
 */
@Serializable
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey
    val fingerprint: String,          // SHA-256(pubKey) hex 64 chars
    val pubKey: String,               // Base64 encoded X25519 public key (32 bytes)
    val currentAuthUid: String,       // Firebase Auth UID for routing/notifications
    val displayNameEncrypted: String, // Encrypted with KeyVault/CryptoManager (zero plaintext names on disk)
    val securityNumber: String,       // 60-digit Signal-style formatted string
    val verified: Boolean = false,    // True after manual Safety Number comparison in Model A
    val addedAt: Long = 0L,           // Timestamp epoch millis
    // v1.8.0: Local metadata (encrypted at rest via Room + DPAPI/Keystore)
    val nicknameEncrypted: String? = null,
    val isFavorite: Boolean = false,
    val categoryEncrypted: String? = null
) {
    fun toItem(decryptedDisplayName: String, decryptedNickname: String? = null, decryptedCategory: String? = null): ContactItem {
        return ContactItem(
            fingerprint = fingerprint,
            pubKey = pubKey,
            currentAuthUid = currentAuthUid,
            displayName = decryptedDisplayName,
            securityNumber = securityNumber,
            verified = verified,
            addedAt = addedAt,
            nickname = decryptedNickname,
            isFavorite = isFavorite,
            category = decryptedCategory
        )
    }
}
