import {
  initializeTestEnvironment,
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
} from "@firebase/rules-unit-testing";
import * as fs from "fs";
import * as path from "path";

describe("P0.2 — Firestore Security Rules Adversarial Verification Suite", () => {
  let testEnv: RulesTestEnvironment;

  // Hashes de identidade simulando chaves públicas híbridas derivadas (64 chars hex)
  const ALICE_IDENTITY_HASH = "a11ce00000000000000000000000000000000000000000000000000000000001";
  const BOB_IDENTITY_HASH = "b0b0000000000000000000000000000000000000000000000000000000000002";
  const EVE_IDENTITY_HASH = "eee0000000000000000000000000000000000000000000000000000000000003";

  beforeAll(async () => {
    const rulesPath = path.resolve(__dirname, "../../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");

    testEnv = await initializeTestEnvironment({
      projectId: "pmsg-adversarial-rules-test",
      firestore: {
        rules,
        host: "127.0.0.1",
        port: 8080,
      },
    });
  });

  afterAll(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    if (testEnv) {
      await testEnv.clearFirestore();
    }
  });

  // =========================================================================
  // CASO 1: Identidade A -> lê dados de B -> DENIED
  // =========================================================================
  describe("Caso 1: Identidade A tenta ler dados de B -> DENIED", () => {
    it("MUST DENY Alice from reading Bob's private inbox envelope under identities/{bobHash}/inbox/{envelopeId}", async () => {
      // Setup: Bob registra sua identidade e recebe um envelope em sua inbox
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("identities").doc(BOB_IDENTITY_HASH).set({
          currentAuthUid: "bob_uid",
          pubKey: "bob_hybrid_pubkey",
          suite: "pmsg-routing-v2",
        });

        await db
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_bob_001")
          .set({
            ciphertext: "super_secret_payload_for_bob",
            iv: "random_iv",
            expiresAt: new Date(Date.now() + 3600000), // 1h future
          });
      });

      // Alice (adversária/não-proprietária) tenta ler o envelope de Bob
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const readPromise = aliceDb
        .collection("identities")
        .doc(BOB_IDENTITY_HASH)
        .collection("inbox")
        .doc("env_bob_001")
        .get();

      await assertFails(readPromise);

      // Verificação de contraprova: Bob (legítimo dono) lê com sucesso
      const bobDb = testEnv.authenticatedContext("bob_uid").firestore();
      const bobReadPromise = bobDb
        .collection("identities")
        .doc(BOB_IDENTITY_HASH)
        .collection("inbox")
        .doc("env_bob_001")
        .get();

      await assertSucceeds(bobReadPromise);
    });

    it("MUST DENY Alice from reading legacy messages addressed only to Bob", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("messages").doc("legacy_msg_bob").set({
          ciphertext: "legacy_ciphertext",
          iv: "iv",
          senderId: "charlie_uid",
          recipientId: "bob_uid",
          expiresAt: new Date(Date.now() + 3600000),
        });
      });

      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      await assertFails(aliceDb.collection("messages").doc("legacy_msg_bob").get());
    });
  });

  // =========================================================================
  // CASO 2: Identidade A -> escreve/lê DEK de B -> DENIED
  // =========================================================================
  describe("Caso 2: Identidade A tenta escrever ou ler DEK de B -> DENIED (Isolamento Total)", () => {
    it("MUST DENY any client (Alice, Bob or Eve) from writing directly to messageKeys (DEK)", async () => {
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const writePromise = aliceDb.collection("messageKeys").doc("msg_dek_001").set({
        messageId: "msg_dek_001",
        dek: "injected_dek_key",
        recipientId: "bob_uid",
        expiresAt: new Date(Date.now() + 3600000),
      });

      await assertFails(writePromise);
    });

    it("MUST DENY any client from reading DEK from messageKeys collection", async () => {
      // Backend grava DEK via Admin SDK
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("messageKeys").doc("msg_dek_002").set({
          messageId: "msg_dek_002",
          dek: "server_managed_ephemeral_dek_256",
          expiresAt: new Date(Date.now() + 3600000),
        });
      });

      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const bobDb = testEnv.authenticatedContext("bob_uid").firestore();

      await assertFails(aliceDb.collection("messageKeys").doc("msg_dek_002").get());
      await assertFails(bobDb.collection("messageKeys").doc("msg_dek_002").get());
    });
  });

  // =========================================================================
  // CASO 3: Não autenticado -> lê qualquer coisa -> DENIED
  // =========================================================================
  describe("Caso 3: Usuário não autenticado tenta ler qualquer rota -> DENIED", () => {
    beforeEach(async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("identities").doc(ALICE_IDENTITY_HASH).set({
          currentAuthUid: "alice_uid",
          pubKey: "pubKey",
        });
        await db
          .collection("identities")
          .doc(ALICE_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_01")
          .set({
            ciphertext: "data",
            expiresAt: new Date(Date.now() + 3600000),
          });
        await db.collection("messages").doc("msg_01").set({
          ciphertext: "data",
          senderId: "alice_uid",
          recipientId: "bob_uid",
          expiresAt: new Date(Date.now() + 3600000),
        });
        await db.collection("accessLogs").doc("log_01").set({ ip: "127.0.0.1" });
        await db.collection("invites").doc("inv_01").set({ creator: "alice_uid" });
      });
    });

    it("MUST DENY unauthenticated client from reading identities root", async () => {
      const anonDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(anonDb.collection("identities").doc(ALICE_IDENTITY_HASH).get());
    });

    it("MUST DENY unauthenticated client from reading inbox envelopes", async () => {
      const anonDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(
        anonDb.collection("identities").doc(ALICE_IDENTITY_HASH).collection("inbox").doc("env_01").get()
      );
    });

    it("MUST DENY unauthenticated client from reading messages, messageKeys, invites or logs", async () => {
      const anonDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(anonDb.collection("messages").doc("msg_01").get());
      await assertFails(anonDb.collection("messageKeys").doc("msg_01").get());
      await assertFails(anonDb.collection("accessLogs").doc("log_01").get());
      await assertFails(anonDb.collection("invites").doc("inv_01").get());
    });
  });

  // =========================================================================
  // CASO 4: Não autenticado / Autenticado -> enumera coleção -> DENIED
  // =========================================================================
  describe("Caso 4: Enumeração e varredura de coleções -> DENIED", () => {
    it("MUST DENY unauthenticated client from listing/querying identities", async () => {
      const anonDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(anonDb.collection("identities").get());
    });

    it("MUST DENY authenticated client from enumerating identities directory (prevent user scraping)", async () => {
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      await assertFails(aliceDb.collection("identities").get());
    });

    it("MUST DENY unauthenticated client from listing messages or messageKeys", async () => {
      const anonDb = testEnv.unauthenticatedContext().firestore();
      await assertFails(anonDb.collection("messages").get());
      await assertFails(anonDb.collection("messageKeys").get());
    });
  });

  // =========================================================================
  // CASO 5: Usuário tenta forjar identityHash de outro -> DENIED
  // =========================================================================
  describe("Caso 5: Usuário tenta forjar ou sequestrar identityHash de outro -> DENIED", () => {
    it("MUST DENY Eve from claiming Alice's existing identity document", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("identities").doc(ALICE_IDENTITY_HASH).set({
          currentAuthUid: "alice_uid",
          pubKey: "alice_hybrid_pubkey",
        });
      });

      // Eve tenta sobrescrever a identidade de Alice com seu próprio UID
      const eveDb = testEnv.authenticatedContext("eve_uid").firestore();
      await assertFails(
        eveDb.collection("identities").doc(ALICE_IDENTITY_HASH).set({
          currentAuthUid: "eve_uid",
          pubKey: "eve_hybrid_pubkey",
        })
      );
    });

    it("MUST DENY Eve from creating an identity where currentAuthUid does not match her auth.uid (impersonation)", async () => {
      const eveDb = testEnv.authenticatedContext("eve_uid").firestore();
      await assertFails(
        eveDb.collection("identities").doc(EVE_IDENTITY_HASH).set({
          currentAuthUid: "alice_uid", // Falsificação de UID
          pubKey: "eve_pubkey",
        })
      );
    });

    it("MUST DENY direct update or delete on identities collection (requires Cloud Function with FIPS 204 proof)", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("identities").doc(ALICE_IDENTITY_HASH).set({
          currentAuthUid: "alice_uid",
          pubKey: "alice_pubkey",
        });
      });

      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      // Alice tentando alterar diretamente via client SDK -> DENIED
      await assertFails(
        aliceDb.collection("identities").doc(ALICE_IDENTITY_HASH).update({
          currentAuthUid: "alice_new_device",
        })
      );
      // Alice tentando deletar diretamente via client SDK -> DENIED
      await assertFails(aliceDb.collection("identities").doc(ALICE_IDENTITY_HASH).delete());
    });
  });

  // =========================================================================
  // CASO 6: Delete: A apaga envelope que não é seu -> DENIED
  // =========================================================================
  describe("Caso 6: Delete adversarial: A apaga envelope que não é seu -> DENIED", () => {
    it("MUST DENY Eve from deleting an inbox envelope belonging to Bob", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore();
        await db.collection("identities").doc(BOB_IDENTITY_HASH).set({
          currentAuthUid: "bob_uid",
          pubKey: "bob_pubkey",
        });

        await db
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("envelope_bob_secret")
          .set({
            ciphertext: "ciphertext",
            expiresAt: new Date(Date.now() + 3600000),
          });
      });

      // Eve tenta deletar o envelope de Bob
      const eveDb = testEnv.authenticatedContext("eve_uid").firestore();
      await assertFails(
        eveDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("envelope_bob_secret")
          .delete()
      );

      // Bob (destinatário legítimo) consegue deletar (Vanish-After-Read)
      const bobDb = testEnv.authenticatedContext("bob_uid").firestore();
      await assertSucceeds(
        bobDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("envelope_bob_secret")
          .delete()
      );
    });

    it("MUST DENY Eve from deleting a legacy message between Alice and Bob", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("messages").doc("msg_between_alice_bob").set({
          ciphertext: "content",
          senderId: "alice_uid",
          recipientId: "bob_uid",
          expiresAt: new Date(Date.now() + 3600000),
        });
      });

      const eveDb = testEnv.authenticatedContext("eve_uid").firestore();
      await assertFails(eveDb.collection("messages").doc("msg_between_alice_bob").delete());
    });
  });

  // =========================================================================
  // CASO 7: Anti-Correlação de Identidades (Emenda B.3) & Imutabilidade de Envelopes
  // =========================================================================
  describe("Caso 7: Emenda B.3 — Anti-Correlação de Identidades e Imutabilidade", () => {
    it("MUST SUCCEED when an authenticated sender creates an envelope in Bob's inbox with future expiresAt", async () => {
      // Bob identity exists
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("identities").doc(BOB_IDENTITY_HASH).set({
          currentAuthUid: "bob_uid",
          pubKey: "bob_pubkey",
        });
      });

      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const futureDate = new Date(Date.now() + 86400000); // 24h

      // Envelope depositado na inbox do destinatário sem guardar ambos os identityHash juntos (Emenda B.3)
      const createPromise = aliceDb
        .collection("identities")
        .doc(BOB_IDENTITY_HASH)
        .collection("inbox")
        .doc("env_pqc_001")
        .set({
          ciphertext: "opaque_pqc_hybrid_envelope_bytes",
          iv: "iv_bytes",
          ephemeralSessionToken: "session_token_123",
          expiresAt: futureDate,
        });

      await assertSucceeds(createPromise);
    });

    it("MUST FAIL when creating an envelope with expired or missing expiresAt", async () => {
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const pastDate = new Date(Date.now() - 10000); // Já expirado

      await assertFails(
        aliceDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_pqc_expired")
          .set({
            ciphertext: "data",
            expiresAt: pastDate,
          })
      );

      await assertFails(
        aliceDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_pqc_no_ttl")
          .set({
            ciphertext: "data",
          })
      );
    });

    it("MUST FAIL when creating an envelope with expiresAt exceeding 24 hours (P0.3 strict upper bound)", async () => {
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const tooFarDate = new Date(Date.now() + 25 * 60 * 60 * 1000); // 25 hours ahead (> 24h)

      await assertFails(
        aliceDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_pqc_too_long")
          .set({
            ciphertext: "data",
            expiresAt: tooFarDate,
          })
      );
    });

    it("MUST FAIL when anyone attempts to modify an existing envelope (strict immutability)", async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        await context.firestore().collection("identities").doc(BOB_IDENTITY_HASH).set({
          currentAuthUid: "bob_uid",
          pubKey: "bob_pubkey",
        });
        await context
          .firestore()
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_immutable")
          .set({
            ciphertext: "original_ciphertext",
            expiresAt: new Date(Date.now() + 3600000),
          });
      });

      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const bobDb = testEnv.authenticatedContext("bob_uid").firestore();

      await assertFails(
        aliceDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_immutable")
          .update({ ciphertext: "tampered" })
      );

      await assertFails(
        bobDb
          .collection("identities")
          .doc(BOB_IDENTITY_HASH)
          .collection("inbox")
          .doc("env_immutable")
          .update({ ciphertext: "tampered" })
      );
    });
  });

  // =========================================================================
  // CASO 7: Coleções Protegidas de Backend (devicePushTokens & securityAnomalies)
  // =========================================================================
  describe("Caso 7: Proteção Estrita de Coleções de Backend -> DENIED para Clients", () => {
    it("MUST DENY direct read and write on devicePushTokens by any client (authenticated or unauthenticated)", async () => {
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const unauthDb = testEnv.unauthenticatedContext().firestore();

      // Escrita direta por cliente autenticado -> DENIED
      await assertFails(
        aliceDb.collection("devicePushTokens").doc("alice_uid").set({
          token: "fcm_token_sample",
        })
      );

      // Leitura direta por cliente autenticado -> DENIED
      await assertFails(aliceDb.collection("devicePushTokens").doc("alice_uid").get());

      // Leitura/Escrita por cliente não autenticado -> DENIED
      await assertFails(
        unauthDb.collection("devicePushTokens").doc("alice_uid").set({
          token: "fcm_token_sample",
        })
      );
      await assertFails(unauthDb.collection("devicePushTokens").doc("alice_uid").get());
    });

    it("MUST DENY direct read and write on securityAnomalies by any client (authenticated or unauthenticated)", async () => {
      const aliceDb = testEnv.authenticatedContext("alice_uid").firestore();
      const unauthDb = testEnv.unauthenticatedContext().firestore();

      // Escrita direta por cliente autenticado -> DENIED
      await assertFails(
        aliceDb.collection("securityAnomalies").doc("anomaly_123").set({
          type: "INJECTED_ANOMALY",
        })
      );

      // Leitura direta por cliente autenticado -> DENIED
      await assertFails(aliceDb.collection("securityAnomalies").doc("anomaly_123").get());

      // Leitura/Escrita por não autenticado -> DENIED
      await assertFails(unauthDb.collection("securityAnomalies").doc("anomaly_123").get());
    });
  });
});

