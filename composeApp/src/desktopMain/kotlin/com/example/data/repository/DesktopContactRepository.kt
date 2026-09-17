package com.example.data.repository

import com.example.data.model.BlockedContact
import com.example.data.model.ContactItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import com.example.security.identity.IdentityManager
import com.example.security.identity.AddressBookCrypto

class DesktopContactRepository : ContactRepository {

    private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(emptyMap())
    private val blockedFlow = MutableStateFlow<Map<String, BlockedContact>>(emptyMap())
    private val blockedPurgeCountFlow = MutableStateFlow(0)

    private val baseDir: File by lazy {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val dir = File(appData, "Pmsg")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private val storageFile: File by lazy {
        File(baseDir, "contacts.json")
    }

    private val blockedFile: File by lazy {
        File(baseDir, "blocked_contacts.json")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    init {
        loadFromDisk()
        loadBlockedFromDisk()
    }

    private fun loadFromDisk() {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                if (content.isNotBlank()) {
                    val key = IdentityManager.getAddressBookKey() ?: return
                    val decrypted = AddressBookCrypto.decrypt(content, key)
                    if (decrypted.isNotBlank()) {
                        val list = json.decodeFromString<List<ContactItem>>(decrypted)
                        
                        // TTL Enforcement (48 hours)
                        val now = System.currentTimeMillis()
                        val ttlMillis = 48L * 60 * 60 * 1000L
                        val validList = list.filter { (now - it.addedAt) < ttlMillis }
                        
                        contactsFlow.value = validList.associateBy { it.fingerprint }
                        if (validList.size != list.size) {
                            saveToDisk() // Persist TTL evictions
                        }
                        return
                    }
                }
            }
            contactsFlow.value = emptyMap()
            saveToDisk()
        } catch (_: Throwable) {}
    }

    private fun saveToDisk() {
        try {
            val key = IdentityManager.getAddressBookKey() ?: return
            val list = contactsFlow.value.values.toList()
            val text = json.encodeToString(list)
            val encrypted = AddressBookCrypto.encrypt(text, key)
            storageFile.writeText(encrypted)
        } catch (_: Throwable) {}
    }

    private fun loadBlockedFromDisk() {
        try {
            if (blockedFile.exists()) {
                val content = blockedFile.readText()
                if (content.isNotBlank()) {
                    val key = IdentityManager.getAddressBookKey() ?: return
                    val decrypted = AddressBookCrypto.decrypt(content, key)
                    if (decrypted.isNotBlank()) {
                        val list = json.decodeFromString<List<BlockedContact>>(decrypted)
                        blockedFlow.value = list.associateBy { it.fingerprint }
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun saveBlockedToDisk() {
        try {
            val key = IdentityManager.getAddressBookKey() ?: return
            val list = blockedFlow.value.values.toList()
            val text = json.encodeToString(list)
            val encrypted = AddressBookCrypto.encrypt(text, key)
            blockedFile.writeText(encrypted)
        } catch (_: Throwable) {}
    }

    override fun getContacts(): Flow<List<ContactItem>> {
        if (contactsFlow.value.isEmpty()) {
            loadFromDisk()
        }
        return contactsFlow.map { it.values.sortedByDescending { c -> c.addedAt } }
    }

    override suspend fun getContact(fingerprint: String): ContactItem? = withContext(Dispatchers.IO) {
        contactsFlow.value[fingerprint]
    }

    override suspend fun saveContact(contact: ContactItem) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        val toSave = if (contact.addedAt > 0) contact else contact.copy(addedAt = System.currentTimeMillis())
        current[contact.fingerprint] = toSave
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun setVerified(fingerprint: String, verified: Boolean) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        val c = current[fingerprint] ?: return@withContext
        current[fingerprint] = c.copy(verified = verified)
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun updateAuthUid(fingerprint: String, newUid: String) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        val c = current[fingerprint] ?: return@withContext
        current[fingerprint] = c.copy(currentAuthUid = newUid)
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun deleteContact(fingerprint: String) = withContext(Dispatchers.IO) {
        val current = contactsFlow.value.toMutableMap()
        current.remove(fingerprint)
        contactsFlow.value = current
        saveToDisk()
    }

    override suspend fun panicWipe(): Int = withContext(Dispatchers.IO) {
        val count = contactsFlow.value.size
        contactsFlow.value = emptyMap()
        blockedFlow.value = emptyMap()
        blockedPurgeCountFlow.value = 0
        try {
            if (storageFile.exists()) {
                // Anti-forensic zeroization before delete
                storageFile.writeBytes(ByteArray(storageFile.length().toInt().coerceAtLeast(1)))
                storageFile.delete()
            }
            if (blockedFile.exists()) {
                blockedFile.writeBytes(ByteArray(blockedFile.length().toInt().coerceAtLeast(1)))
                blockedFile.delete()
            }
        } catch (_: Throwable) {}
        count
    }

    override fun getBlockedContacts(): Flow<List<BlockedContact>> {
        return blockedFlow.map { it.values.sortedByDescending { b -> b.blockedAt } }
    }

    override suspend fun blockContact(fingerprint: String) = withContext(Dispatchers.IO) {
        val current = blockedFlow.value.toMutableMap()
        current[fingerprint] = BlockedContact(fingerprint = fingerprint, blockedAt = System.currentTimeMillis())
        blockedFlow.value = current
        saveBlockedToDisk()
    }

    override suspend fun unblockContact(fingerprint: String) = withContext(Dispatchers.IO) {
        val current = blockedFlow.value.toMutableMap()
        current.remove(fingerprint)
        blockedFlow.value = current
        saveBlockedToDisk()
    }

    override suspend fun isContactBlocked(fingerprint: String): Boolean = withContext(Dispatchers.IO) {
        blockedFlow.value.containsKey(fingerprint)
    }

    override suspend fun recordBlockedPurge(fingerprint: String) {
        blockedPurgeCountFlow.value += 1
    }

    override fun getBlockedPurgeCount(): Flow<Int> = blockedPurgeCountFlow
}
