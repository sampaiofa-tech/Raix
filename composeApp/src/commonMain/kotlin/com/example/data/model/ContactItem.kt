package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * Domain representation of a Contact in Raix.
 * Display name is in plaintext in memory, but NEVER on disk (stored as encrypted envelope).
 *
 * v1.8.0: Added local metadata fields (nickname, isFavorite, category).
 * These operate exclusively on local encrypted data — never on the server graph.
 */
@Serializable
data class ContactItem(
    val fingerprint: String,
    val pubKey: String,
    val currentAuthUid: String,
    val displayName: String,
    val securityNumber: String,
    val verified: Boolean = false,
    val addedAt: Long = 0L,
    // v1.8.0: Local metadata (encrypted at rest, never sent to server)
    val nickname: String? = null,
    val isFavorite: Boolean = false,
    val category: String? = null
)
