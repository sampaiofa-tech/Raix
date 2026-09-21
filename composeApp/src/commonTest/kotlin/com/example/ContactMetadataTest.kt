package com.example

import com.example.data.model.BlockedContact
import com.example.data.model.ContactItem
import com.example.data.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * v1.8.0 Functional Tests (N2)
 * Covers: rename, delete, favorite, category on local encrypted metadata.
 * Uses in-memory ContactRepository to simulate encrypted-at-rest behavior.
 */
class ContactMetadataTest {

    private fun createTestRepository(): ContactRepository {
        return object : ContactRepository {
            private val contactsFlow = MutableStateFlow<Map<String, ContactItem>>(emptyMap())
            private val blockedFlow = MutableStateFlow<Map<String, BlockedContact>>(emptyMap())
            private val purgeCountFlow = MutableStateFlow(0)

            override fun getContacts(): Flow<List<ContactItem>> =
                contactsFlow.map { it.values.toList() }

            override suspend fun getContact(fingerprint: String): ContactItem? =
                contactsFlow.value[fingerprint]

            override suspend fun saveContact(contact: ContactItem) {
                val current = contactsFlow.value.toMutableMap()
                current[contact.fingerprint] = contact
                contactsFlow.value = current
            }

            override suspend fun setVerified(fingerprint: String, verified: Boolean) {
                val current = contactsFlow.value.toMutableMap()
                val c = current[fingerprint] ?: return
                current[fingerprint] = c.copy(verified = verified)
                contactsFlow.value = current
            }

            override suspend fun updateAuthUid(fingerprint: String, newUid: String) {
                val current = contactsFlow.value.toMutableMap()
                val c = current[fingerprint] ?: return
                current[fingerprint] = c.copy(currentAuthUid = newUid)
                contactsFlow.value = current
            }

            override suspend fun renameContact(fingerprint: String, newName: String) {
                val current = contactsFlow.value.toMutableMap()
                val c = current[fingerprint] ?: return
                current[fingerprint] = c.copy(displayName = newName)
                contactsFlow.value = current
            }

            override suspend fun setFavorite(fingerprint: String, isFavorite: Boolean) {
                val current = contactsFlow.value.toMutableMap()
                val c = current[fingerprint] ?: return
                current[fingerprint] = c.copy(isFavorite = isFavorite)
                contactsFlow.value = current
            }

            override suspend fun setCategory(fingerprint: String, category: String?) {
                val current = contactsFlow.value.toMutableMap()
                val c = current[fingerprint] ?: return
                current[fingerprint] = c.copy(category = category)
                contactsFlow.value = current
            }

            override suspend fun deleteContact(fingerprint: String) {
                val current = contactsFlow.value.toMutableMap()
                current.remove(fingerprint)
                contactsFlow.value = current
            }

            override suspend fun panicWipe(): Int {
                val c = contactsFlow.value.size + blockedFlow.value.size
                contactsFlow.value = emptyMap()
                blockedFlow.value = emptyMap()
                purgeCountFlow.value = 0
                return c
            }

            override fun getBlockedContacts(): Flow<List<BlockedContact>> =
                blockedFlow.map { it.values.sortedByDescending { b -> b.blockedAt } }

            override suspend fun blockContact(fingerprint: String) {
                val current = blockedFlow.value.toMutableMap()
                current[fingerprint] = BlockedContact(fingerprint, 1000L)
                blockedFlow.value = current
            }

            override suspend fun unblockContact(fingerprint: String) {
                val current = blockedFlow.value.toMutableMap()
                current.remove(fingerprint)
                blockedFlow.value = current
            }

            override suspend fun isContactBlocked(fingerprint: String): Boolean =
                blockedFlow.value.containsKey(fingerprint)

            override suspend fun recordBlockedPurge(fingerprint: String) {
                purgeCountFlow.value += 1
            }

            override fun getBlockedPurgeCount(): Flow<Int> = purgeCountFlow
        }
    }

    private fun testContact(
        fingerprint: String = "abc123",
        displayName: String = "Alice"
    ) = ContactItem(
        fingerprint = fingerprint,
        pubKey = "pubkey_$fingerprint",
        currentAuthUid = "uid_$fingerprint",
        displayName = displayName,
        securityNumber = "1234 5678",
        verified = false,
        addedAt = System.currentTimeMillis()
    )

    // --- RENAME ---

    @Test
    fun renameContact_updatesDisplayName() = runTest {
        val repo = createTestRepository()
        val contact = testContact()
        repo.saveContact(contact)

        repo.renameContact("abc123", "Bob")

        val updated = repo.getContact("abc123")
        assertEquals("Bob", updated?.displayName)
    }

    @Test
    fun renameContact_preservesOtherFields() = runTest {
        val repo = createTestRepository()
        val contact = testContact()
        repo.saveContact(contact)

        repo.renameContact("abc123", "Carlos")

        val updated = repo.getContact("abc123")!!
        assertEquals(contact.pubKey, updated.pubKey)
        assertEquals(contact.securityNumber, updated.securityNumber)
        assertEquals(contact.fingerprint, updated.fingerprint)
    }

    @Test
    fun renameContact_nonExistentDoesNothing() = runTest {
        val repo = createTestRepository()
        repo.renameContact("nonexistent", "Ghost")
        val contacts = repo.getContacts().first()
        assertTrue(contacts.isEmpty())
    }

    // --- DELETE ---

    @Test
    fun deleteContact_removesFromList() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())
        assertEquals(1, repo.getContacts().first().size)

        repo.deleteContact("abc123")

        val contacts = repo.getContacts().first()
        assertTrue(contacts.isEmpty())
    }

    @Test
    fun deleteContact_preservesOtherContacts() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact("fp1", "Alice"))
        repo.saveContact(testContact("fp2", "Bob"))

        repo.deleteContact("fp1")

        val remaining = repo.getContacts().first()
        assertEquals(1, remaining.size)
        assertEquals("Bob", remaining[0].displayName)
    }

    @Test
    fun deleteContact_nonExistentDoesNothing() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())
        repo.deleteContact("nonexistent")
        assertEquals(1, repo.getContacts().first().size)
    }

    // --- FAVORITE ---

    @Test
    fun setFavorite_togglesOn() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())
        assertFalse(repo.getContact("abc123")!!.isFavorite)

        repo.setFavorite("abc123", true)

        assertTrue(repo.getContact("abc123")!!.isFavorite)
    }

    @Test
    fun setFavorite_togglesOff() = runTest {
        val repo = createTestRepository()
        val contact = testContact().copy(isFavorite = true)
        repo.saveContact(contact)

        repo.setFavorite("abc123", false)

        assertFalse(repo.getContact("abc123")!!.isFavorite)
    }

    @Test
    fun setFavorite_preservesOtherFields() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())

        repo.setFavorite("abc123", true)

        val updated = repo.getContact("abc123")!!
        assertEquals("Alice", updated.displayName)
        assertEquals("pubkey_abc123", updated.pubKey)
    }

    @Test
    fun setFavorite_nonExistentDoesNothing() = runTest {
        val repo = createTestRepository()
        repo.setFavorite("nonexistent", true)
        assertTrue(repo.getContacts().first().isEmpty())
    }

    // --- CATEGORY ---

    @Test
    fun setCategory_assignsCategory() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())
        assertNull(repo.getContact("abc123")!!.category)

        repo.setCategory("abc123", "Trabalho")

        assertEquals("Trabalho", repo.getContact("abc123")!!.category)
    }

    @Test
    fun setCategory_clearsCategory() = runTest {
        val repo = createTestRepository()
        val contact = testContact().copy(category = "Trabalho")
        repo.saveContact(contact)

        repo.setCategory("abc123", null)

        assertNull(repo.getContact("abc123")!!.category)
    }

    @Test
    fun setCategory_changesCategory() = runTest {
        val repo = createTestRepository()
        val contact = testContact().copy(category = "Trabalho")
        repo.saveContact(contact)

        repo.setCategory("abc123", "Pessoal")

        assertEquals("Pessoal", repo.getContact("abc123")!!.category)
    }

    @Test
    fun setCategory_preservesOtherFields() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())

        repo.setCategory("abc123", "VIP")

        val updated = repo.getContact("abc123")!!
        assertEquals("Alice", updated.displayName)
        assertFalse(updated.isFavorite)
    }

    // --- COMBINED OPERATIONS ---

    @Test
    fun combinedOperations_renameAndFavoriteAndCategory() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())

        repo.renameContact("abc123", "Ana")
        repo.setFavorite("abc123", true)
        repo.setCategory("abc123", "VIP")

        val contact = repo.getContact("abc123")!!
        assertEquals("Ana", contact.displayName)
        assertTrue(contact.isFavorite)
        assertEquals("VIP", contact.category)
    }

    @Test
    fun combinedOperations_deleteAfterMetadataChanges() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact())
        repo.setFavorite("abc123", true)
        repo.setCategory("abc123", "Trabalho")

        repo.deleteContact("abc123")

        assertNull(repo.getContact("abc123"))
        assertTrue(repo.getContacts().first().isEmpty())
    }

    @Test
    fun combinedOperations_multipleContactsIndependent() = runTest {
        val repo = createTestRepository()
        repo.saveContact(testContact("fp1", "Alice"))
        repo.saveContact(testContact("fp2", "Bob"))

        repo.setFavorite("fp1", true)
        repo.setCategory("fp2", "Trabalho")

        val alice = repo.getContact("fp1")!!
        val bob = repo.getContact("fp2")!!

        assertTrue(alice.isFavorite)
        assertNull(alice.category)
        assertFalse(bob.isFavorite)
        assertEquals("Trabalho", bob.category)
    }
}
