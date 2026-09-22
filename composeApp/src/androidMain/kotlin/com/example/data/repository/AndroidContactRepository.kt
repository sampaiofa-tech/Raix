package com.example.data.repository

import com.example.data.local.BlockedContactDao
import com.example.data.local.ContactDao
import com.example.data.model.BlockedContact
import com.example.data.model.BlockedContactEntity
import com.example.data.model.Contact
import com.example.data.model.ContactItem
import com.example.security.identity.AddressBookCrypto
import com.example.security.identity.IdentityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AndroidContactRepository(
    private val contactDao: ContactDao,
    private val blockedContactDao: BlockedContactDao? = null
) : ContactRepository {

    private val blockedPurgeCountFlow = MutableStateFlow(0)

    private fun decryptField(encrypted: String?, key: ByteArray): String? {
        if (encrypted == null) return null
        return try {
            AddressBookCrypto.decrypt(encrypted, key)
        } catch (_: Throwable) {
            null
        }
    }

    private fun encryptField(value: String?, key: ByteArray): String? {
        if (value == null) return null
        return try {
            AddressBookCrypto.encrypt(value, key)
        } catch (_: Throwable) {
            null
        }
    }

    override fun getContacts(): Flow<List<ContactItem>> {
        return contactDao.getAllContacts().map { list ->
            list.map { contact ->
                val key = try { IdentityManager.getAddressBookKey() } catch (_: Throwable) { null }
                val decryptedName = if (key != null) {
                    try {
                        AddressBookCrypto.decrypt(contact.displayNameEncrypted, key)
                    } catch (_: Throwable) { "Contato Desconhecido" }
                } else "Contato Desconhecido"

                val decryptedNickname = if (key != null) decryptField(contact.nicknameEncrypted, key) else null
                val decryptedCategory = if (key != null) decryptField(contact.categoryEncrypted, key) else null

                contact.toItem(
                    decryptedDisplayName = if (decryptedName.isNotBlank()) decryptedName else "Contato Desconhecido",
                    decryptedNickname = decryptedNickname,
                    decryptedCategory = decryptedCategory
                )
            }
        }
    }

    override suspend fun getContact(fingerprint: String): ContactItem? = withContext(Dispatchers.IO) {
        val contact = contactDao.getContactByFingerprint(fingerprint) ?: return@withContext null
        val key = try { IdentityManager.getAddressBookKey() } catch (_: Throwable) { null }
        val decryptedName = if (key != null) {
            try {
                AddressBookCrypto.decrypt(contact.displayNameEncrypted, key)
            } catch (_: Throwable) { "Contato Desconhecido" }
        } else "Contato Desconhecido"

        val decryptedNickname = if (key != null) decryptField(contact.nicknameEncrypted, key) else null
        val decryptedCategory = if (key != null) decryptField(contact.categoryEncrypted, key) else null

        contact.toItem(
            decryptedDisplayName = if (decryptedName.isNotBlank()) decryptedName else "Contato Desconhecido",
            decryptedNickname = decryptedNickname,
            decryptedCategory = decryptedCategory
        )
    }

    override suspend fun saveContact(contact: ContactItem) = withContext(Dispatchers.IO) {
        val key = IdentityManager.getAddressBookKey() ?: return@withContext
        val encryptedName = AddressBookCrypto.encrypt(contact.displayName, key)
        val entity = Contact(
            fingerprint = contact.fingerprint,
            pubKey = contact.pubKey,
            currentAuthUid = contact.currentAuthUid,
            displayNameEncrypted = encryptedName,
            securityNumber = contact.securityNumber,
            verified = contact.verified,
            addedAt = if (contact.addedAt > 0) contact.addedAt else System.currentTimeMillis(),
            nicknameEncrypted = encryptField(contact.nickname, key),
            isFavorite = contact.isFavorite,
            categoryEncrypted = encryptField(contact.category, key)
        )
        contactDao.insertContact(entity)
    }

    override suspend fun setVerified(fingerprint: String, verified: Boolean) = withContext(Dispatchers.IO) {
        contactDao.updateVerified(fingerprint, verified)
    }

    override suspend fun updateAuthUid(fingerprint: String, newUid: String) = withContext(Dispatchers.IO) {
        contactDao.updateAuthUid(fingerprint, newUid)
    }

    override suspend fun deleteContact(fingerprint: String) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(fingerprint)
    }

    override suspend fun renameContact(fingerprint: String, newName: String) = withContext(Dispatchers.IO) {
        val contact = getContact(fingerprint) ?: return@withContext
        saveContact(contact.copy(displayName = newName))
    }

    override suspend fun setFavorite(fingerprint: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        contactDao.updateFavorite(fingerprint, isFavorite)
    }

    override suspend fun setCategory(fingerprint: String, category: String?) = withContext(Dispatchers.IO) {
        val key = IdentityManager.getAddressBookKey() ?: return@withContext
        val encrypted = encryptField(category, key)
        contactDao.updateCategory(fingerprint, encrypted)
    }

    override suspend fun panicWipe(): Int = withContext(Dispatchers.IO) {
        val count = contactDao.panicWipeAllContacts()
        blockedContactDao?.panicWipeAllBlockedContacts()
        blockedPurgeCountFlow.value = 0
        count
    }

    override fun getBlockedContacts(): Flow<List<BlockedContact>> {
        return blockedContactDao?.getAllBlockedContacts()?.map { list ->
            list.map { it.toDomain() }
        } ?: flowOf(emptyList())
    }

    override suspend fun blockContact(fingerprint: String) = withContext(Dispatchers.IO) {
        blockedContactDao?.insertBlockedContact(
            BlockedContactEntity(
                fingerprint = fingerprint,
                blockedAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    override suspend fun unblockContact(fingerprint: String) = withContext(Dispatchers.IO) {
        blockedContactDao?.deleteBlockedContact(fingerprint)
        Unit
    }

    override suspend fun isContactBlocked(fingerprint: String): Boolean = withContext(Dispatchers.IO) {
        blockedContactDao?.isBlocked(fingerprint) ?: false
    }

    override suspend fun recordBlockedPurge(fingerprint: String) {
        blockedPurgeCountFlow.value += 1
    }

    override fun getBlockedPurgeCount(): Flow<Int> = blockedPurgeCountFlow
}
