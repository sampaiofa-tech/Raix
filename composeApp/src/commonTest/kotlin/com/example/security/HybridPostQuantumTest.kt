package com.example.security

import com.example.security.identity.DowngradeAttackException
import com.example.security.identity.Ed25519SignatureScheme
import com.example.security.identity.HybridEd25519MlDsa65SignatureScheme
import com.example.security.identity.HybridKem
import com.example.security.identity.IdentityCryptoManager
import com.example.security.identity.MlDsa65SignatureScheme
import com.example.security.identity.SealedBox
import com.example.security.identity.SealedBoxEnvelope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HybridPostQuantumTest {

    @Test
    fun testDualDerivationFromEntropyAndMnemonicRecovery() {
        val fixedEntropy = ByteArray(16) { (it * 13 + 7).toByte() }
        val id1 = IdentityCryptoManager.generateNewIdentity(fixedEntropy)
        val id2 = IdentityCryptoManager.generateNewIdentity(fixedEntropy)

        // 1. Classical deterministic equivalence
        assertTrue(id1.keyPair.privateKey.contentEquals(id2.keyPair.privateKey))
        assertTrue(id1.keyPair.publicKey.contentEquals(id2.keyPair.publicKey))
        assertTrue(id1.keyPair.signingPrivateKey.contentEquals(id2.keyPair.signingPrivateKey))
        assertTrue(id1.keyPair.signingPublicKey.contentEquals(id2.keyPair.signingPublicKey))

        // 2. Post-Quantum deterministic equivalence
        assertTrue(id1.keyPair.mlKemPrivateKey.contentEquals(id2.keyPair.mlKemPrivateKey))
        assertTrue(id1.keyPair.mlKemPublicKey.contentEquals(id2.keyPair.mlKemPublicKey))
        assertTrue(id1.keyPair.mlDsaPrivateKey.contentEquals(id2.keyPair.mlDsaPrivateKey))
        assertTrue(id1.keyPair.mlDsaPublicKey.contentEquals(id2.keyPair.mlDsaPublicKey))

        // 3. FIPS 203 and 204 Parameter sizes (Level 3)
        assertEquals(1184, id1.keyPair.mlKemPublicKey.size) // ML-KEM-768 public key size
        assertEquals(1952, id1.keyPair.mlDsaPublicKey.size) // ML-DSA-65 public key size

        // 4. Mnemonic recovery in clean environment
        val restored = IdentityCryptoManager.restoreFromMnemonic(id1.mnemonic).getOrThrow()
        assertTrue(id1.keyPair.privateKey.contentEquals(restored.privateKey))
        assertTrue(id1.keyPair.publicKey.contentEquals(restored.publicKey))
        assertTrue(id1.keyPair.mlKemPrivateKey.contentEquals(restored.mlKemPrivateKey))
        assertTrue(id1.keyPair.mlKemPublicKey.contentEquals(restored.mlKemPublicKey))
        assertTrue(id1.keyPair.mlDsaPrivateKey.contentEquals(restored.mlDsaPrivateKey))
        assertTrue(id1.keyPair.mlDsaPublicKey.contentEquals(restored.mlDsaPublicKey))
        assertEquals(id1.keyPair.hybridFingerprintHex, restored.hybridFingerprintHex)
        assertEquals(id1.keyPair.hybridSafetyNumber, restored.hybridSafetyNumber)
    }

    @Test
    fun testSignatureSchemeCryptoAgilityAndHybridValidation() {
        val alice = IdentityCryptoManager.generateNewIdentity()
        val message = "raix-pqc-routing-challenge-payload".encodeToByteArray()

        // 1. Classical Ed25519
        val edScheme = Ed25519SignatureScheme()
        val edSig = edScheme.sign(alice.keyPair.signingPrivateKey, message)
        assertTrue(edScheme.verify(alice.keyPair.signingPublicKey, message, edSig))

        // 2. Post-Quantum ML-DSA-65
        val mlDsaScheme = MlDsa65SignatureScheme()
        val mlDsaSig = mlDsaScheme.sign(alice.keyPair.mlDsaPrivateKey, message)
        assertTrue(mlDsaScheme.verify(alice.keyPair.mlDsaPublicKey, message, mlDsaSig))

        // 3. Compound Hybrid Scheme (Ed25519 + ML-DSA-65)
        val hybridScheme = HybridEd25519MlDsa65SignatureScheme()
        val compositePriv = HybridEd25519MlDsa65SignatureScheme.encodeCompositeKey(
            alice.keyPair.signingPrivateKey,
            alice.keyPair.mlDsaPrivateKey
        )
        val compositePub = HybridEd25519MlDsa65SignatureScheme.encodeCompositeKey(
            alice.keyPair.signingPublicKey,
            alice.keyPair.mlDsaPublicKey
        )

        val hybridSig = hybridScheme.sign(compositePriv, message)
        assertTrue(hybridScheme.verify(compositePub, message, hybridSig))

        // 4. Tamper verification: altered message must fail
        val tamperedMessage = "tampered-payload".encodeToByteArray()
        assertFalse(hybridScheme.verify(compositePub, tamperedMessage, hybridSig))

        // 5. Tamper verification: altered signature must fail
        val corruptedSig = hybridSig.copyOf()
        corruptedSig[corruptedSig.size - 1] = (corruptedSig[corruptedSig.size - 1].toInt() xor 0xFF).toByte()
        assertFalse(hybridScheme.verify(compositePub, message, corruptedSig))
    }

    @Test
    fun testHybridKemCombinerAndForwardSecrecy() {
        val bob = IdentityCryptoManager.generateNewIdentity()

        // 1. Alice encapsulates against Bob's hybrid public keys
        val kemResult = HybridKem.encapsulate(bob.keyPair.publicKey, bob.keyPair.mlKemPublicKey)
        assertEquals(32, kemResult.ephemeralX25519PubKey.size)
        assertEquals(1088, kemResult.mlKemCiphertext.size) // ML-KEM-768 ciphertext
        assertEquals(32, kemResult.combinedSharedSecret.size) // Derived KEK

        // 2. Bob decapsulates and recovers identical combined key
        val bobSecret = HybridKem.decapsulate(
            recipientX25519PrivKey = bob.keyPair.privateKey,
            recipientMlKemPrivKey = bob.keyPair.mlKemPrivateKey,
            ephemeralX25519PubKey = kemResult.ephemeralX25519PubKey,
            mlKemCiphertext = kemResult.mlKemCiphertext
        )
        assertTrue(kemResult.combinedSharedSecret.contentEquals(bobSecret))

        // 3. Forward Secrecy validation: second encapsulation produces fresh ephemeral keys and fresh ciphertext
        val secondKemResult = HybridKem.encapsulate(bob.keyPair.publicKey, bob.keyPair.mlKemPublicKey)
        assertFalse(kemResult.ephemeralX25519PubKey.contentEquals(secondKemResult.ephemeralX25519PubKey))
        assertFalse(kemResult.mlKemCiphertext.contentEquals(secondKemResult.mlKemCiphertext))
        assertFalse(kemResult.combinedSharedSecret.contentEquals(secondKemResult.combinedSharedSecret))

        val secondBobSecret = HybridKem.decapsulate(
            recipientX25519PrivKey = bob.keyPair.privateKey,
            recipientMlKemPrivKey = bob.keyPair.mlKemPrivateKey,
            ephemeralX25519PubKey = secondKemResult.ephemeralX25519PubKey,
            mlKemCiphertext = secondKemResult.mlKemCiphertext
        )
        assertTrue(secondKemResult.combinedSharedSecret.contentEquals(secondBobSecret))
    }

    @Test
    fun testSealedBoxHybridEncryptionAndDecryption() {
        val bob = IdentityCryptoManager.generateNewIdentity()
        val originalDek = "aes_256_gcm_dek_payload_super_secret_32b".encodeToByteArray().copyOf(32)

        // Seal with Hybrid PQC
        val envelope = SealedBox.seal(
            dek = originalDek,
            recipientPubKey = bob.keyPair.publicKey,
            recipientMlKemPubKey = bob.keyPair.mlKemPublicKey
        )

        assertEquals(SealedBoxEnvelope.SUITE_HYBRID, envelope.securitySuite)
        assertTrue(envelope.mlKemCiphertextBase64 != null)

        // Unseal with Bob's hybrid private keys
        val recoveredDek = SealedBox.unseal(
            envelope = envelope,
            recipientPrivKey = bob.keyPair.privateKey,
            recipientMlKemPrivKey = bob.keyPair.mlKemPrivateKey,
            enforceHybrid = true
        )

        assertTrue(originalDek.contentEquals(recoveredDek))
    }

    @Test
    fun testAntiDowngradeProtectionRejectsCoercedClassicalEnvelope() {
        val bob = IdentityCryptoManager.generateNewIdentity()
        val originalDek = ByteArray(32) { (it + 1).toByte() }

        // Attacker creates a coerced classical-only envelope (forcing X25519 only)
        val coercedEnvelope = SealedBox.seal(
            dek = originalDek,
            recipientPubKey = bob.keyPair.publicKey,
            recipientMlKemPubKey = null // omitting ML-KEM
        )
        assertEquals(SealedBoxEnvelope.SUITE_CLASSICAL, coercedEnvelope.securitySuite)
        assertEquals(null, coercedEnvelope.mlKemCiphertextBase64)

        // When Bob's client enforces hybrid security, it MUST REJECT the downgraded envelope
        assertFailsWith<DowngradeAttackException> {
            SealedBox.unseal(
                envelope = coercedEnvelope,
                recipientPrivKey = bob.keyPair.privateKey,
                recipientMlKemPrivKey = bob.keyPair.mlKemPrivateKey,
                enforceHybrid = true
            )
        }

        // Without hybrid enforcement (legacy mode), classical unseal works
        val legacyRecovered = SealedBox.unseal(
            envelope = coercedEnvelope,
            recipientPrivKey = bob.keyPair.privateKey,
            recipientMlKemPrivKey = null,
            enforceHybrid = false
        )
        assertTrue(originalDek.contentEquals(legacyRecovered))
    }

    @Test
    fun testHybridSafetyNumbersFormatAndSymmetry() {
        val alice = IdentityCryptoManager.generateNewIdentity()
        val bob = IdentityCryptoManager.generateNewIdentity()

        // 1. Single identity hybrid safety number is 60 digits (12 blocks of 5)
        val aliceSafety = alice.keyPair.hybridSafetyNumber
        assertEquals(71, aliceSafety.length) // 60 digits + 11 spaces
        val blocks = aliceSafety.split(" ")
        assertEquals(12, blocks.size)
        assertTrue(blocks.all { it.length == 5 && it.all { ch -> ch.isDigit() } })

        // 2. Hybrid Pair Safety Number Symmetry
        val pairAliceBob = IdentityCryptoManager.computeHybridPairSafetyNumber(
            myClassicalPub = alice.keyPair.publicKey,
            myMlKemPub = alice.keyPair.mlKemPublicKey,
            peerClassicalPub = bob.keyPair.publicKey,
            peerMlKemPub = bob.keyPair.mlKemPublicKey
        )

        val pairBobAlice = IdentityCryptoManager.computeHybridPairSafetyNumber(
            myClassicalPub = bob.keyPair.publicKey,
            myMlKemPub = bob.keyPair.mlKemPublicKey,
            peerClassicalPub = alice.keyPair.publicKey,
            peerMlKemPub = alice.keyPair.mlKemPublicKey
        )

        assertEquals(pairAliceBob, pairBobAlice)
        assertEquals(71, pairAliceBob.length)

        // 3. Different peer produces different safety number
        val charlie = IdentityCryptoManager.generateNewIdentity()
        val pairAliceCharlie = IdentityCryptoManager.computeHybridPairSafetyNumber(
            myClassicalPub = alice.keyPair.publicKey,
            myMlKemPub = alice.keyPair.mlKemPublicKey,
            peerClassicalPub = charlie.keyPair.publicKey,
            peerMlKemPub = charlie.keyPair.mlKemPublicKey
        )
        assertNotEquals(pairAliceBob, pairAliceCharlie)
    }

    @Test
    fun testHybridProofOfPossessionRoutingHandshake() {
        val alice = IdentityCryptoManager.generateNewIdentity()
        val timestamp = 1725400000000L
        val newAuthUid = "firebase_uid_alice_pqc_device"

        val signature = IdentityCryptoManager.signRoutingUpdateHybrid(
            edSigningPriv = alice.keyPair.signingPrivateKey,
            mlDsaPriv = alice.keyPair.mlDsaPrivateKey,
            fingerprint = alice.keyPair.fingerprintHex,
            newAuthUid = newAuthUid,
            timestamp = timestamp
        )

        val isValid = IdentityCryptoManager.verifyRoutingUpdateHybrid(
            edSigningPub = alice.keyPair.signingPublicKey,
            mlDsaPub = alice.keyPair.mlDsaPublicKey,
            fingerprint = alice.keyPair.fingerprintHex,
            newAuthUid = newAuthUid,
            timestamp = timestamp,
            signature = signature
        )
        assertTrue(isValid)

        // Tampering with payload or security level breaks verification
        val isTamperedValid = IdentityCryptoManager.verifyRoutingUpdateHybrid(
            edSigningPub = alice.keyPair.signingPublicKey,
            mlDsaPub = alice.keyPair.mlDsaPublicKey,
            fingerprint = alice.keyPair.fingerprintHex,
            newAuthUid = "attacker_uid",
            timestamp = timestamp,
            signature = signature
        )
        assertFalse(isTamperedValid)
    }
}
