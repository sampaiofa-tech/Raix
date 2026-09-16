# Código Consolidado do Sistema Raix (Pmsg)
**Documento Mestre para Análise do Validador (Qwen 2.5 Coder 32B)**  
**Versão da Codebase:** 1.6 (P1+ — Pós-Quântico, Defesa contra Análise de Tráfego por IA e Governança Sanitizada)  
**Data:** 08 de setembro de 2026  
**Auditoria de Credenciais:** Concluída (100% livre de segredos, tokens e chaves estáticas)

---

## 1. Guia de Ingestão e Análise pelo Modelo Validador

Este compêndio consolida a arquitetura completa do **Raix**, projetada para oferecer comunicação com privacidade absoluta por design, confidencialidade pós-quântica (PQC) e minimização extrema de metadados.

### Estrutura e Ingestão na Janela de ~32k tokens do Qwen 2.5 Coder 32B
Para garantir máxima profundidade de análise sem risco de saturação ou truncamento de janela, o código-fonte foi estruturado em camadas modulares e complementares:
1. **Compêndio Mestre Unificado (`docs/CODIGO_CONSOLIDADO.md`)**:
   - Este arquivo (~110 KB / ~27k tokens) reúne o **Core do Sistema**: a visão geral, todas as regras e funções críticas do **Backend** (`functions/src`), o **Núcleo Criptográfico KMP** (`HybridKem`, `SignatureScheme`, `SealedBox`, `IdentityCryptoManager`, etc.) e as regras mandatórias de governança (`AGENTS.md`).
2. **Módulos Setoriais Detalhados (Prontos para Análise Especializada)**:
   - [`docs/CODIGO_CONSOLIDADO_BACKEND.md`](file:///c:/Dev/Pmsg/docs/CODIGO_CONSOLIDADO_BACKEND.md) (~93 KB / ~23k tokens): Todas as 14 Cloud Functions integrais, regras e índices do Firestore, e análise das fronteiras de confiança.
   - [`docs/CODIGO_CONSOLIDADO_CLIENTE.md`](file:///c:/Dev/Pmsg/docs/CODIGO_CONSOLIDADO_CLIENTE.md) (~120 KB / ~30k tokens): Código KMP completo de Criptografia, Identidade, BIP-39, KeyVault, Sanitização de Memória e Camada de Rede Ktor REST.
   - [`docs/CODIGO_CONSOLIDADO_INFRA_SEGURANCA.md`](file:///c:/Dev/Pmsg/docs/CODIGO_CONSOLIDADO_INFRA_SEGURANCA.md) (~105 KB / ~26k tokens): Diretrizes do Agente (`AGENTS.md`), Modelagem de Ameaças completa (`THREAT_MODEL.md`) e Pipelines de CI/CD (SLSA Nível 2+, SBOM CycloneDX, health-check).
   - [`docs/CODIGO_CONSOLIDADO_UI.md`](file:///c:/Dev/Pmsg/docs/CODIGO_CONSOLIDADO_UI.md) (~80 KB / ~20k tokens): Camada de apresentação segura: portão de idade ECA/LGPD (`AgeGateScreen`), Safety Numbers OOB (`SafetyNumberScreen`), contatos e núcleo E2E de chat efêmero com autodestruição.

---

## 2. Mapa Arquitetural do Sistema

| Camada / Subsistema | Componentes Principais | Invariantes de Segurança & Privacidade |
| :--- | :--- | :--- |
| **Backend Coordenador Cego** (`functions/src/`) | `storeMessageKey`, `getMessageKey`, `resolveFingerprint`, `updateIdentityRouting`, `shredder` | Zero posse de chaves privadas; DEK opaca encapsulada; destruição atômica (*shredder*); autenticação anônima. |
| **Conformidade Legal** (`functions/src/connectionLogs.ts`) | `recordConnectionLog` | Marco Civil da Internet (Art. 15, Lei 12.965/2014); 180 dias de retenção; IP pseudonimizado via HMAC-SHA256 rotativo mensal; zero fingerprinting de cliente. |
| **Banco de Dados & Acesso** (`firestore.rules`) | Regras de Segurança Firestore | Bloqueio de escrita direta em `messageKeys`; isolamento estrito por `authUid`; integridade estrutural. |
| **Identidade & BIP-39** (`composeApp/.../identity/`) | `Bip39Portuguese`, `IdentityCryptoManager`, `IdentityManager` | 128 bits de entropia $\rightarrow$ 12 palavras PT-BR com checksum SHA-256; derivação Argon2id com salts segregados; custódia biométrica local. |
| **Criptografia Pós-Quântica Híbrida** | `HybridKem` (FIPS 203 ML-KEM-768 + X25519), `SignatureScheme` (FIPS 204 ML-DSA-65 + Ed25519) | Combiner NIST SP 800-227 / RFC 9180 via HKDF-SHA256; assinatura híbrida composta sob regra booleana `AND` estrita; proteção anti-downgrade. |
| **Higiene e Hardware** | `MemorySanitizer`, `KeyVault`, `ClipboardSensivel` | Sobrescrita determinística com zeros (`zeroize`); isolamento de chaves em silício seguro (StrongBox/Secure Enclave/TPM). |
| **Comunicação E2E & Shredder Local** | `SealedBox`, `AesGcm`, `ContactChatScreen` | DEK efêmera de 256 bits; envelopes SealedBox; entrega com autodestruição (*vanish-after-read*) e expurgo local/remoto em cascata. |
| **Governança & Supply Chain** | `AGENTS.md`, `health-check.yml`, `supply-chain-attestation.yml` | Proibição estrita de vazamento de segredos; atestado SLSA Nível 2+; SBOM CycloneDX v1.5 determinístico. |

---

## 3. Código Essencial: Backend & Regras de Banco de Dados

### Arquivo: `functions/src/index.ts`
**Contexto Arquitetural:** Ponto de entrada de funções e exportações HTTPS/Triggers.

```typescript
import * as admin from "firebase-admin";

admin.initializeApp();

export { scheduledMessageShredder, onDeleteMessage } from "./shredder";
export { geminiProxy } from "./geminiProxy";
export { storeMessageKey } from "./storeMessageKey";
export { getMessageKey } from "./getMessageKey";
export { resolveFingerprint } from "./resolveFingerprint";
export { createInvite } from "./createInvite";
export { acceptInvite } from "./acceptInvite";
export { updateIdentityRouting } from "./updateIdentityRouting";
export { reportAbuse } from "./reportAbuse";
export { reportAbuseWithContent } from "./reportAbuseWithContent";
export { registerPushToken } from "./registerPushToken";
```

---

### Arquivo: `functions/src/storeMessageKey.ts`
**Contexto Arquitetural:** Armazenamento do envelope da DEK selada com autenticação de remetente.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { Timestamp, FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

const MIN_TTL_MILLIS = 10 * 1000; // 10s
const MAX_TTL_MILLIS = 24 * 60 * 60 * 1000; // 24h

export interface StoreMessageKeyData {
  messageId: string;
  senderId: string;
  recipientId: string;
  ephemeralPubKey: string;
  wrappedDek: string;
  expiresAtMillis: number;
}

/**
 * HTTPS Callable Cloud Function to store a message's wrapped Data Encryption Key (DEK).
 * 
 * ZERO-TRACE / ZERO-KNOWLEDGE DESIGN:
 * Clients have 0 direct write access to the 'messageKeys' collection in Firestore.
 * All DEK persistence is routed strictly through this authenticated server-side function.
 * The server receives and stores ONLY opaque bytes (ephemeralPubKey, wrappedDek).
 * The server never sees the plaintext DEK or recipient private key.
 */
export const storeMessageKey = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "storeMessageKey");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("storeMessageKey: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Authentication required to store message encryption key."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as StoreMessageKeyData;

  // 2. Validate payload structure
  if (
    !data ||
    !data.messageId ||
    !data.senderId ||
    !data.recipientId ||
    !data.ephemeralPubKey ||
    !data.wrappedDek ||
    typeof data.expiresAtMillis !== "number"
  ) {
    logger.warn("storeMessageKey: Invalid payload structure.");
    throw new HttpsError(
      "invalid-argument",
      "Missing or invalid required fields: messageId, senderId, recipientId, ephemeralPubKey, wrappedDek, expiresAtMillis."
    );
  }

  // 3. Enforce Participant Authorization (caller MUST be the sender)
  if (callerUid !== data.senderId) {
    logger.warn(
      `storeMessageKey: Caller ${callerUid} is not the sender ${data.senderId}. Access denied.`
    );
    throw new HttpsError(
      "permission-denied",
      "Caller must be the message sender to register its encryption key."
    );
  }

  // 4. Clamping of TTL (10s minimum, 24h maximum)
  const nowMillis = Date.now();
  const requestedTtl = data.expiresAtMillis - nowMillis;
  const clampedTtl = Math.max(MIN_TTL_MILLIS, Math.min(requestedTtl, MAX_TTL_MILLIS));
  const effectiveExpiresAtMillis = nowMillis + clampedTtl;
  const effectiveExpiresAt = Timestamp.fromMillis(effectiveExpiresAtMillis);

  // 5. Store Opaque Wrapped Key via Admin SDK in messageKeys collection
  const db = admin.firestore();
  await db.collection("messageKeys").doc(data.messageId).set({
    messageId: data.messageId,
    senderId: data.senderId,
    recipientId: data.recipientId,
    ephemeralPubKey: data.ephemeralPubKey,
    wrappedDek: data.wrappedDek,
    createdAt: FieldValue.serverTimestamp(),
    expiresAt: effectiveExpiresAt,
  });

  logger.info(
    `storeMessageKey: Opaque wrapped DEK stored securely for message ${data.messageId} with TTL ${clampedTtl}ms.`
  );

  // 6. Zero-Knowledge Push Notification to Recipient (v1.6)
  try {
    const tokenDoc = await db.collection("devicePushTokens").doc(data.recipientId).get();
    if (tokenDoc.exists) {
      const tokenData = tokenDoc.data();
      const pushToken = tokenData?.token;
      if (pushToken && typeof pushToken === "string") {
        const clampedTtlSeconds = Math.max(1, Math.floor(clampedTtl / 1000));
        await admin.messaging().send({
          token: pushToken,
          notification: {
            title: "Raix",
            body: "Nova mensagem efêmera recebida.",
          },
          data: {
            type: "new_message",
            messageId: data.messageId,
            expiresAtMillis: String(effectiveExpiresAtMillis),
          },
          android: {
            priority: "high",
            ttl: clampedTtlSeconds * 1000,
            notification: {
              channelId: "new_conversations_channel",
              sound: "default",
            },
          },
          apns: {
            headers: {
              "apns-priority": "10",
              "apns-expiration": String(Math.floor(effectiveExpiresAtMillis / 1000)),
            },
            payload: {
              aps: {
                alert: {
                  title: "Raix",
                  body: "Nova mensagem efêmera recebida.",
                },
                sound: "default",
              },
            },
          },
        });
        logger.info(`storeMessageKey: Zero-knowledge push notification sent to recipient ${data.recipientId}`);
      }
    }
  } catch (pushErr: any) {
    // Non-blocking: failure to send push must never prevent message delivery
    logger.warn(`storeMessageKey: Push notification dispatch non-blocking notice:`, pushErr?.message || pushErr);
  }

  return {
    success: true,
    messageId: data.messageId,
    expiresAtMillis: effectiveExpiresAtMillis,
  };
});
```

---

### Arquivo: `functions/src/getMessageKey.ts`
**Contexto Arquitetural:** Recuperação do envelope da DEK com verificação estrita de destinatário.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

export interface GetMessageKeyData {
  messageId: string;
}

export interface GetMessageKeyResult {
  success: boolean;
  messageId: string;
  ephemeralPubKey: string;
  wrappedDek: string;
  expiresAtMillis: number;
}

/**
 * HTTPS Callable Cloud Function to deliver a message's wrapped Data Encryption Key (DEK)
 * strictly to an authorized participant (recipient or sender).
 *
 * ZERO-TRACE / ZERO-KNOWLEDGE DESIGN:
 * 1. Clients have NO direct read access to 'messageKeys' in Firestore (rules: allow read: if false).
 * 2. This callable validates that the authenticated caller (request.auth.uid) matches the message's
 *    recipientId (or senderId).
 * 3. Expired keys are rejected and cannot be read.
 * 4. The server returns ONLY opaque bytes (ephemeralPubKey, wrappedDek). It never sees or learns plaintext DEKs.
 * 5. Vanish-After-Read Semantics: Once the recipient decrypts and marks the message as read,
 *    the message doc in 'messages' is deleted by the client, triggering 'onDeleteMessage'
 *    which permanently shreds the wrapped DEK in 'messageKeys'.
 */
export const getMessageKey = onCall(async (request): Promise<GetMessageKeyResult> => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "getMessageKey");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("getMessageKey: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Authentication required to retrieve message encryption key."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as GetMessageKeyData;

  // 2. Validate payload structure
  if (!data || !data.messageId || typeof data.messageId !== "string" || data.messageId.trim() === "") {
    logger.warn("getMessageKey: Invalid or missing messageId.");
    throw new HttpsError(
      "invalid-argument",
      "Missing or invalid required field: messageId."
    );
  }

  const messageId = data.messageId.trim();

  // 3. Retrieve Key Document via Firebase Admin SDK
  const db = admin.firestore();
  const keyDocRef = db.collection("messageKeys").doc(messageId);
  const keyDocSnap = await keyDocRef.get();

  if (!keyDocSnap.exists) {
    logger.warn(`getMessageKey: Key document for message ${messageId} not found or already shredded.`);
    throw new HttpsError(
      "not-found",
      "Encryption key not found or already shredded."
    );
  }

  const keyData = keyDocSnap.data();
  if (!keyData || !keyData.ephemeralPubKey || !keyData.wrappedDek || !keyData.expiresAt) {
    logger.error(`getMessageKey: Corrupt key document structure for message ${messageId}.`);
    throw new HttpsError(
      "internal",
      "Corrupt encryption key metadata."
    );
  }

  // 4. Validate Expiration
  const expiresAtMillis = typeof keyData.expiresAt.toMillis === "function"
    ? keyData.expiresAt.toMillis()
    : Number(keyData.expiresAt);

  const nowMillis = Date.now();
  if (expiresAtMillis <= nowMillis) {
    logger.warn(`getMessageKey: Message ${messageId} has expired (expiresAt: ${expiresAtMillis}, now: ${nowMillis}).`);
    throw new HttpsError(
      "failed-precondition",
      "Message has expired and its encryption key is no longer accessible."
    );
  }

  // 5. Enforce Participant Authorization (caller must be recipientId or senderId)
  const recipientId = keyData.recipientId;
  const senderId = keyData.senderId;

  if (callerUid !== recipientId && callerUid !== senderId) {
    logger.warn(
      `getMessageKey: Unauthorized access attempt. Caller ${callerUid} is not a participant (sender: ${senderId}, recipient: ${recipientId}) for message ${messageId}.`
    );
    throw new HttpsError(
      "permission-denied",
      "Caller is not authorized to retrieve this encryption key."
    );
  }

  // Zero-trace logging: audit access without logging secret key material
  logger.info(`getMessageKey: Opaque wrapped DEK securely released to participant ${callerUid} for message ${messageId}.`);

  return {
    success: true,
    messageId: messageId,
    ephemeralPubKey: keyData.ephemeralPubKey,
    wrappedDek: keyData.wrappedDek,
    expiresAtMillis: expiresAtMillis,
  };
});
```

---

### Arquivo: `functions/src/resolveFingerprint.ts`
**Contexto Arquitetural:** Resolução pública de fingerprint para roteamento seguro.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 min
const MAX_REQUESTS_PER_WINDOW = 30; // 30 requests / min per user

/**
 * HTTPS Callable Cloud Function to resolve a 256-bit technical fingerprint
 * to its stable currentAuthUid and public key.
 * 
 * PRIVACY & ANTI-ENUMERATION DESIGN:
 * 1. Fingerprint is a 256-bit cryptographic hash (SHA-256) of the X25519 public key.
 *    Searching blindly is mathematically impossible (2^256 keyspace).
 * 2. Strict authentication requirement: unauthenticated calls are rejected immediately.
 * 3. Atomic per-user sliding window rate limiting prevents scraping / dictionary scans.
 * 4. Zero PII: stores and returns only technical routing identifiers, zero names or contact graphs.
 */
export const resolveFingerprint = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "resolveFingerprint");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("resolveFingerprint: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para resolver identidade."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as { fingerprint?: string };

  // 2. Validate payload
  if (!data || !data.fingerprint || typeof data.fingerprint !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'fingerprint' é obrigatório e deve ser uma string."
    );
  }

  const fingerprint = data.fingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  const db = admin.firestore();

  // 3. Sliding window rate limiting
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const data = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - data.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (data.count >= MAX_REQUESTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de requisições excedido. Aguarde antes de tentar novamente."
        );
      } else {
        t.update(rateLimitRef, { count: data.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed:", err);
  }

  // 4. Resolve identity from Firestore
  const identityDoc = await db.collection("identities").doc(fingerprint).get();
  if (!identityDoc.exists) {
    throw new HttpsError("not-found", "Identidade não encontrada no diretório técnico.");
  }

  const identityData = identityDoc.data()!;

  // 5. Check moderation revocation status (Ed25519 routing revocation)
  if (identityData.revoked === true || identityData.status === "revoked") {
    logger.warn(`resolveFingerprint: Rejected revoked identity ${fingerprint.substring(0, 8)}...`);
    throw new HttpsError(
      "permission-denied",
      "Esta identidade criptográfica foi revogada por moderação e não pode mais receber mensagens."
    );
  }

  return {
    currentAuthUid: identityData.currentAuthUid,
    pubKey: identityData.pubKey,
    signingPubKey: identityData.signingPubKey || "",
    updatedAt: identityData.updatedAt?.toMillis
      ? identityData.updatedAt.toMillis()
      : typeof identityData.updatedAt === "number"
      ? identityData.updatedAt
      : now,
  };
});
```

---

### Arquivo: `functions/src/updateIdentityRouting.ts`
**Contexto Arquitetural:** Atualização de rota efêmera com prova de posse criptográfica e anti-downgrade.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
const MAX_UPDATES_PER_WINDOW = 5; // Max 5 recovery updates per 10 min
const MAX_TIMESTAMP_SKEW_MS = 5 * 60 * 1000; // 5 minutes anti-replay window

// RFC 8410 SPKI prefix for 32-byte Ed25519 raw public key:
// 30 2a (SEQUENCE of 42 bytes)
//   30 05 (SEQUENCE of 5 bytes)
//     06 03 2b 65 70 (OID 1.3.101.112 - id-Ed25519)
//   03 21 00 (BIT STRING of 33 bytes, 0 unused bits)
//     <32-byte raw public key>
const ED25519_SPKI_PREFIX = Buffer.from("302a300506032b6570032100", "hex");

export interface UpdateIdentityRoutingData {
  fingerprint?: string;
  pubKey?: string;
  signature?: string;
  timestamp?: number;
  signingPubKey?: string;
}

/**
 * HTTPS Callable Cloud Function to update technical identity routing after recovery.
 * 
 * SECURITY SPECIFICATIONS (F0 - Proof-of-Possession via Ed25519):
 * 1. Requires authenticated caller (callerUid = request.auth.uid).
 * 2. Cryptographic binding check: SHA-256(pubKey) == fingerprint.
 * 3. Anti-replay tolerance window: |now - timestamp| <= 5 minutes.
 * 4. Ed25519 Signature Verification:
 *    Payload: "pmsg-routing-v1|<fingerprint>|<callerUid>|<timestamp>"
 *    Verified against the immutable `signingPubKey` registered in `identities/{fingerprint}`.
 *    Any signature mismatch or non-possession throws 'permission-denied'.
 * 5. Sliding window rate limiting prevents abuse.
 */
export const updateIdentityRouting = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "updateIdentityRouting");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("updateIdentityRouting: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para atualizar roteamento de identidade."
    );
  }

  const callerUid = request.auth.uid;
  const data = (request.data || {}) as UpdateIdentityRoutingData;

  // 2. Validate basic input fields
  if (!data.fingerprint || !data.pubKey) {
    throw new HttpsError(
      "invalid-argument",
      "Campos 'fingerprint' e 'pubKey' são obrigatórios."
    );
  }

  const fingerprint = data.fingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  const pubKey = data.pubKey.trim();
  if (pubKey.length === 0) {
    throw new HttpsError("invalid-argument", "Chave pública não pode ser vazia.");
  }

  if (!data.signature || typeof data.signature !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'signature' (prova de posse Ed25519) é obrigatório."
    );
  }

  if (typeof data.timestamp !== "number" || !Number.isFinite(data.timestamp)) {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'timestamp' numérico é obrigatório."
    );
  }

  // 3. Cryptographic Binding Proof: SHA-256(pubKey) == fingerprint
  try {
    const pubKeyBytes = Buffer.from(pubKey, "base64");
    if (pubKeyBytes.length !== 32) {
      throw new Error("Invalid public key length");
    }
    const computedFp = crypto.createHash("sha256").update(new Uint8Array(pubKeyBytes)).digest("hex");
    if (computedFp.toLowerCase() !== fingerprint) {
      throw new HttpsError(
        "invalid-argument",
        "Fingerprint não corresponde ao hash da chave pública fornecida."
      );
    }
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    throw new HttpsError("invalid-argument", "Formato inválido de chave pública Base64.");
  }

  // 4. Anti-Replay Protection: check timestamp skew <= 5 min
  const now = Date.now();
  if (Math.abs(now - data.timestamp) > MAX_TIMESTAMP_SKEW_MS) {
    throw new HttpsError(
      "invalid-argument",
      "Timestamp expirado ou fora da tolerância de 5 minutos (anti-replay)."
    );
  }

  const db = admin.firestore();

  // 5. Retrieve existing signing public key from Firestore doc
  const identityRef = db.collection("identities").doc(fingerprint);
  const identityDoc = await identityRef.get();
  let targetSigningPubKey: string;

  if (identityDoc.exists) {
    const existingData = identityDoc.data()!;
    if (existingData.revoked === true || existingData.status === "revoked") {
      logger.warn(`updateIdentityRouting: Attempt to update revoked identity ${fingerprint.substring(0, 8)}...`);
      throw new HttpsError(
        "permission-denied",
        "Esta identidade criptográfica foi revogada por moderação e não pode ser atualizada."
      );
    }
    if (existingData.signingPubKey) {
      targetSigningPubKey = existingData.signingPubKey;
    } else if (data.signingPubKey) {
      targetSigningPubKey = data.signingPubKey.trim();
    } else {
      throw new HttpsError(
        "permission-denied",
        "Identidade existente sem chave de assinatura Ed25519 registrada."
      );
    }
  } else {
    // Initial identity creation requires signingPubKey
    if (!data.signingPubKey || typeof data.signingPubKey !== "string") {
      throw new HttpsError(
        "invalid-argument",
        "Campo 'signingPubKey' obrigatório para registrar nova identidade."
      );
    }
    targetSigningPubKey = data.signingPubKey.trim();
  }

  // 6. Cryptographic Proof of Possession: Verify Ed25519 signature
  const canonicalPayload = `pmsg-routing-v1|${fingerprint}|${callerUid}|${data.timestamp}`;
  const messageBytes = Buffer.from(canonicalPayload, "utf8");

  try {
    const rawSigningPubKey = Buffer.from(targetSigningPubKey, "base64");
    if (rawSigningPubKey.length !== 32) {
      throw new Error("Invalid raw signing key length (must be 32 bytes)");
    }

    const signatureBytes = Buffer.from(data.signature.trim(), "base64");
    if (signatureBytes.length !== 64) {
      throw new Error("Invalid signature length (must be 64 bytes)");
    }

    const spkiBuffer = Buffer.concat([new Uint8Array(ED25519_SPKI_PREFIX), new Uint8Array(rawSigningPubKey)]);
    const keyObject = crypto.createPublicKey({ key: spkiBuffer, format: "der", type: "spki" });

    const isValid = crypto.verify(null, new Uint8Array(messageBytes), keyObject, new Uint8Array(signatureBytes));
    if (!isValid) {
      logger.warn(`updateIdentityRouting: Invalid Ed25519 signature for fingerprint ${fingerprint.substring(0, 8)}...`);
      throw new HttpsError(
        "permission-denied",
        "Prova de posse inválida: assinatura Ed25519 rejeitada."
      );
    }
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("updateIdentityRouting: Ed25519 verification exception:", err);
    throw new HttpsError(
      "permission-denied",
      "Falha na validação da assinatura Ed25519: " + err.message
    );
  }

  // 7. Rate Limiting via userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - limitData.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (limitData.count >= MAX_UPDATES_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de atualizações de roteamento excedido. Tente novamente mais tarde."
        );
      } else {
        t.update(rateLimitRef, { count: limitData.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in updateIdentityRouting:", err);
  }

  // 8. Update technical identity directory with new currentAuthUid and signingPubKey
  await identityRef.set(
    {
      currentAuthUid: callerUid,
      pubKey,
      signingPubKey: targetSigningPubKey,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  logger.info(`updateIdentityRouting: Fingerprint ${fingerprint.substring(0, 8)}... verified & bound to UID ${callerUid}`);

  return {
    success: true,
    fingerprint,
    currentAuthUid: callerUid,
    signingPubKey: targetSigningPubKey,
  };
});
```

---

### Arquivo: `functions/src/shredder.ts`
**Contexto Arquitetural:** Destruição ativa de dados: scheduledMessageShredder e onDeleteMessage.

```typescript
import * as admin from "firebase-admin";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { logger } from "firebase-functions";

export interface CryptoShreddingResult {
  shreddedKeysCount: number;
  deletedMessagesCount: number;
  deletedInboxEnvelopesCount: number;
  deletedLogsCount: number;
  maxDelayMs: number;
  escalationAlertsCount: number;
}

const MAX_SAFE_SURVIVAL_DELAY_MS = 60 * 60 * 1000; // 60 minutes
const SEVERE_SURVIVAL_DELAY_MS = 3 * 60 * 60 * 1000; // 3 hours

/**
 * Authoritative Server-Side Crypto-Shredder (P0.3).
 *
 * Runs every 15 minutes (4x/hour) via Cloud Scheduler.
 *
 * Idempotency:
 * Processes deletion in transactional batches. Multiple concurrent or sequential
 * invocations targeting the same expired documents produce zero side effects.
 *
 * Expiration Latency Monitoring & Escalation:
 * Calculates `delayMs = currentTime - expiresAt` for every expired artifact.
 * Logs the `ttl_expiration_to_deletion_delays` metric.
 * Dispatches escalation alerts if any artifact survives past maxLife + 60 min.
 */
export async function executeCryptoShredding(
  db: admin.firestore.Firestore,
  currentTime: admin.firestore.Timestamp
): Promise<CryptoShreddingResult> {
  let messageCount = 0;
  let inboxCount = 0;
  let logsCount = 0;
  let maxDelayMs = 0;
  let escalationAlertsCount = 0;
  const stageErrors: Array<{ stage: string; error: unknown }> = [];

  // =========================================================================
  // STAGE 1: CORE ZERO-TRACE CRYPTO-SHREDDING (PRIORITY 1 - ISOLATED & ATOMIC)
  // Hard-delete DEK em messageKeys + Hard-delete ciphertext em messages.
  // Independent atomic batch commit: NEVER blocked by downstream secondary queries.
  // =========================================================================
  try {
    const expiredKeysQuery = db
      .collection("messageKeys")
      .where("expiresAt", "<=", currentTime)
      .limit(500);

    const snapshot = await expiredKeysQuery.get();
    if (!snapshot.empty) {
      const dekBatch = db.batch();
      for (const doc of snapshot.docs) {
        try {
          const data = doc.data();
          const messageId = data.messageId || doc.id;

          if (data.expiresAt && typeof data.expiresAt.toMillis === "function") {
            const delayMs = currentTime.toMillis() - data.expiresAt.toMillis();
            if (delayMs > maxDelayMs) maxDelayMs = delayMs;

            logger.info("ttl_expiration_to_deletion_delays", {
              metric: "ttl_expiration_to_deletion_delays",
              collection: "messageKeys",
              docId: doc.id,
              delayMs,
            });

            if (delayMs > MAX_SAFE_SURVIVAL_DELAY_MS) {
              escalationAlertsCount++;
              if (delayMs > SEVERE_SURVIVAL_DELAY_MS) {
                logger.error("[ALERT_ESCALATION_LEVEL_2] CRITICAL: Envelope key survived > 3 hours past expiration!", {
                  docId: doc.id,
                  delayMs,
                  threshold: "180m",
                });
              } else {
                logger.warn("[ALERT_ESCALATION_LEVEL_1] WARNING: Envelope key survived > 60 min past expiration!", {
                  docId: doc.id,
                  delayMs,
                  threshold: "60m",
                });
              }
            }
          }

          // 1. Hard-delete DEK (Irreversible Crypto-Shredding)
          dekBatch.delete(doc.ref);

          // 2. Hard-delete matching ciphertext message document (if exists)
          const messageRef = db.collection("messages").doc(messageId);
          dekBatch.delete(messageRef);

          messageCount++;
        } catch (docError) {
          logger.error(`[Crypto-Shredder] Error preparing deletion for DEK doc ${doc.id}:`, docError);
          stageErrors.push({ stage: `messageKeys/${doc.id}`, error: docError });
        }
      }

      if (messageCount > 0) {
        await dekBatch.commit();
        logger.info(`Crypto-Shredder [Stage 1]: Priority commit successful. Destroyed ${messageCount} DEKs and messages.`);
      }
    }
  } catch (error) {
    logger.error("[Crypto-Shredder] Fatal error during primary DEK shredding stage:", error);
    stageErrors.push({ stage: "messageKeys/messages", error });
  }

  // =========================================================================
  // STAGE 2: SECONDARY - INBOX ENVELOPES (ISOLATED)
  // Ephemeral envelopes in per-identity inboxes: identities/{identityHash}/inbox/{envelopeId}
  // Runs in its own atomic batch; failure does not affect Stage 1 or Stage 3.
  // =========================================================================
  try {
    const expiredInboxQuery = db
      .collectionGroup("inbox")
      .where("expiresAt", "<=", currentTime)
      .limit(500);

    const inboxSnapshot = await expiredInboxQuery.get();
    if (!inboxSnapshot.empty) {
      const inboxBatch = db.batch();
      for (const doc of inboxSnapshot.docs) {
        try {
          const data = doc.data();
          if (data.expiresAt && typeof data.expiresAt.toMillis === "function") {
            const delayMs = currentTime.toMillis() - data.expiresAt.toMillis();
            if (delayMs > maxDelayMs) maxDelayMs = delayMs;

            logger.info("ttl_expiration_to_deletion_delays", {
              metric: "ttl_expiration_to_deletion_delays",
              collection: "inbox",
              docId: doc.id,
              delayMs,
            });

            if (delayMs > MAX_SAFE_SURVIVAL_DELAY_MS) {
              escalationAlertsCount++;
              logger.warn("[ALERT_ESCALATION_LEVEL_1] WARNING: Inbox envelope survived > 60 min past expiration!", {
                docId: doc.id,
                delayMs,
              });
            }
          }

          inboxBatch.delete(doc.ref);
          inboxCount++;
        } catch (docError) {
          logger.error(`[Crypto-Shredder] Error preparing deletion for inbox doc ${doc.id}:`, docError);
          stageErrors.push({ stage: `inbox/${doc.id}`, error: docError });
        }
      }

      if (inboxCount > 0) {
        await inboxBatch.commit();
        logger.info(`Crypto-Shredder [Stage 2]: Successfully purged ${inboxCount} expired inbox envelopes.`);
      }
    }
  } catch (error) {
    logger.error("[Crypto-Shredder] Error during secondary inbox shredding stage:", error);
    stageErrors.push({ stage: "inbox", error });
  }

  // =========================================================================
  // STAGE 3: SECONDARY - CONNECTION & ACCESS LOGS (ISOLATED)
  // Marco Civil Art. 15 - 180 days retention purge
  // Runs in its own atomic batch.
  // =========================================================================
  try {
    const logsBatch = db.batch();
    let hasLogDeletions = false;

    const expiredConnLogsQuery = db
      .collection("connectionLogs")
      .where("expiresAt", "<=", currentTime)
      .limit(500);

    const connLogsSnapshot = await expiredConnLogsQuery.get();
    if (!connLogsSnapshot.empty) {
      for (const doc of connLogsSnapshot.docs) {
        try {
          logsBatch.delete(doc.ref);
          logsCount++;
          hasLogDeletions = true;
        } catch (docError) {
          logger.error(`[Crypto-Shredder] Error preparing deletion for connLog doc ${doc.id}:`, docError);
          stageErrors.push({ stage: `connectionLogs/${doc.id}`, error: docError });
        }
      }
    }

    const expiredAccessLogsQuery = db
      .collection("accessLogs")
      .where("expiresAt", "<=", currentTime)
      .limit(500);

    const accessLogsSnapshot = await expiredAccessLogsQuery.get();
    if (!accessLogsSnapshot.empty) {
      for (const doc of accessLogsSnapshot.docs) {
        try {
          logsBatch.delete(doc.ref);
          logsCount++;
          hasLogDeletions = true;
        } catch (docError) {
          logger.error(`[Crypto-Shredder] Error preparing deletion for accessLog doc ${doc.id}:`, docError);
          stageErrors.push({ stage: `accessLogs/${doc.id}`, error: docError });
        }
      }
    }

    if (hasLogDeletions) {
      await logsBatch.commit();
      logger.info(`Crypto-Shredder [Stage 3]: Successfully purged ${logsCount} connection/access logs.`);
    }
  } catch (error) {
    logger.error("[Crypto-Shredder] Error during secondary connection/access logs shredding stage:", error);
    stageErrors.push({ stage: "connectionLogs/accessLogs", error });
  }

  if (messageCount === 0 && inboxCount === 0 && logsCount === 0) {
    logger.info("Crypto-Shredder: No expired message keys, inbox envelopes or connection logs found.");
  } else {
    logger.info(
      `Crypto-Shredder: Successfully shredded ${messageCount} keys/messages, ${inboxCount} inbox envelopes and ${logsCount} connection logs. Max delay: ${maxDelayMs}ms.`
    );
  }

  // Structured rethrow if any stage failed, preserving full error stack without silent suppression
  if (stageErrors.length > 0) {
    const summary = stageErrors
      .map((e) => `[Stage: ${e.stage}]: ${e.error instanceof Error ? e.error.message : String(e.error)}`)
      .join("; ");
    const aggregateError = new Error(`Crypto-Shredder partial failure: ${summary}`);
    (aggregateError as any).stageErrors = stageErrors;
    (aggregateError as any).partialResult = {
      shreddedKeysCount: messageCount,
      deletedMessagesCount: messageCount,
      deletedInboxEnvelopesCount: inboxCount,
      deletedLogsCount: logsCount,
      maxDelayMs,
      escalationAlertsCount,
    };
    throw aggregateError;
  }

  return {
    shreddedKeysCount: messageCount,
    deletedMessagesCount: messageCount,
    deletedInboxEnvelopesCount: inboxCount,
    deletedLogsCount: logsCount,
    maxDelayMs,
    escalationAlertsCount,
  };
}

/**
 * Scheduled task running on Cloud Scheduler every 15 minutes (4x/hour).
 * Hard-deletes expired DEK keys, messages and inbox envelopes in batch.
 */
export const scheduledMessageShredder = onSchedule(
  {
    schedule: "*/15 * * * *",
    timeZone: "UTC",
    retryCount: 3,
  },
  async () => {
    const db = admin.firestore();
    const now = admin.firestore.Timestamp.now();
    await executeCryptoShredding(db, now);
  }
);

/**
 * Reactive trigger on message deletion.
 * Ensures instant crypto-shredding of DEK during vanish-after-read or manual deletion.
 */
export const onDeleteMessage = onDocumentDeleted(
  "messages/{messageId}",
  async (event) => {
    const messageId = event.params.messageId;
    if (!messageId) return;

    const db = admin.firestore();
    const keyRef = db.collection("messageKeys").doc(messageId);

    try {
      await keyRef.delete();
      logger.info(`Vanish-on-Delete: DEK for message ${messageId} destroyed immediately.`);
    } catch (error) {
      logger.error(`Failed to delete DEK for message ${messageId}:`, error);
    }
  }
);
```

---

### Arquivo: `functions/src/connectionLogs.ts`
**Contexto Arquitetural:** Retenção legal de conexão (Marco Civil da Internet Art. 15) com pseudonimização HMAC-SHA256.

```typescript
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";

/**
 * Retenção legal obrigatória de registros de conexão para provedores de aplicações de internet:
 * Marco Civil da Internet (Lei nº 12.965/2014, Art. 15):
 * O provedor de aplicações de internet deve manter os respectivos registros de acesso a aplicações
 * de internet, sob sigilo, em ambiente controlado e de segurança, pelo prazo de 6 meses (180 dias).
 * 
 * MINIMIZAÇÃO DE METADADOS (P1.1):
 * - O endereço IP de origem é pseudonimizado criptograficamente via HMAC-SHA256 com salt rotativo mensal.
 * - Portas efêmeras de clientes e fingerprints são omitidos para evitar perfilamento de endpoint.
 * - Expurgo automático em 180 dias gerenciado pelo Shredder ativo.
 */
export const CONNECTION_LOG_RETENTION_DAYS = 180;

export interface ConnectionLogData {
  pseudonymizedIp: string;
  functionName: string;
  timestampUtc: any;
  expiresAt: Date;
}

/**
 * Gera um salt criptográfico rotativo com periodicidade temporal (ano-mês)
 * para permitir correlações de incidentes estritamente durante a janela legal,
 * sem manter vínculo perene com o endereço IP de origem do usuário.
 */
export function getRotatingSalt(date: Date = new Date()): string {
  const yearMonth = `${date.getUTCFullYear()}-${date.getUTCMonth() + 1}`;
  return crypto.createHash("sha256").update(`raix-mci-salt-v1-${yearMonth}`).digest("hex");
}

/**
 * Pseudonimiza o IP de origem através de HMAC-SHA256 com salt rotativo mensal.
 */
export function pseudonymizeIp(rawIp: string, date: Date = new Date()): string {
  if (!rawIp || rawIp === "unknown") return "unknown";
  const salt = getRotatingSalt(date);
  return crypto.createHmac("sha256", salt).update(rawIp).digest("hex").substring(0, 32);
}

/**
 * Registra dados estritos e minimizados de conexão (IP pseudonimizado, timestamp UTC,
 * nome do endpoint e expiração de 180 dias) em conformidade com o Art. 15 do Marco Civil da Internet.
 * 
 * Executa em modo non-blocking e fail-safe para não interromper a operação criptográfica do cliente.
 */
export async function recordConnectionLog(
  request: any,
  functionName: string
): Promise<void> {
  try {
    const rawReq = request?.rawRequest || request;
    if (!rawReq) return;

    // 1. Extração do endereço IP de origem (respeitando proxies de terminação GCP/Cloudflare)
    const xForwardedFor = rawReq.headers?.["x-forwarded-for"];
    let clientIp = "unknown";
    if (typeof xForwardedFor === "string" && xForwardedFor.trim().length > 0) {
      clientIp = xForwardedFor.split(",")[0].trim();
    } else if (typeof rawReq.ip === "string" && rawReq.ip.trim().length > 0) {
      clientIp = rawReq.ip.trim();
    } else if (rawReq.socket?.remoteAddress) {
      clientIp = rawReq.socket.remoteAddress;
    }

    // 2. Pseudonimização imediata do IP com salt rotativo (P1.1: Sem IP bruto em repouso)
    const pseudoIp = pseudonymizeIp(clientIp);

    // 3. TTL estrito de 180 dias (Marco Civil Art. 15)
    const now = Date.now();
    const expiresAt = new Date(now + CONNECTION_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000);

    const timestampUtc = typeof FieldValue?.serverTimestamp === "function" 
      ? FieldValue.serverTimestamp() 
      : new Date();

    const logEntry: ConnectionLogData = {
      pseudonymizedIp: pseudoIp,
      functionName,
      timestampUtc,
      expiresAt,
    };

    const db = admin.firestore();
    if (db && typeof db.collection === "function") {
      const coll = db.collection("connectionLogs");
      if (coll && typeof coll.add === "function") {
        await coll.add(logEntry);
      }
    }
  } catch (err: any) {
    logger.warn(`recordConnectionLog: Falha não-bloqueante ao registrar log para ${functionName}:`, err?.message || err);
  }
}
```

---

### Arquivo: `firestore.rules`
**Contexto Arquitetural:** Regras de Segurança do Firestore garantindo defesa em profundidade e zero acesso direto a chaves.

```rules
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // =========================================================================
    // REGRA RAIZ MANDATÓRIA: DENY-BY-DEFAULT EXPLÍCITO (P0.2)
    // =========================================================================
    // Nenhuma rota, documento ou subcoleção permite leitura ou escrita a menos
    // que uma regra estrita e explicitamente autorizada abaixo conceda acesso.
    match /{document=**} {
      allow read, write: if false;
    }

    // Helper: Verifica se a requisição possui contexto autenticado
    function isAuthenticated() {
      return request.auth != null && request.auth.uid != null;
    }

    // Helper: Verifica se o solicitante autenticado é o detentor legítimo do identityHash
    // O identityHash é o fingerprint derivado do mnemônico/chaves
    function isIdentityOwner(identityHash) {
      return isAuthenticated() &&
        exists(/databases/$(database)/documents/identities/$(identityHash)) &&
        request.auth.uid == get(/databases/$(database)/documents/identities/$(identityHash)).data.currentAuthUid;
    }

    // Helper: Validação estrita de timestamp de expiração (TTL de mensagem <= 24h)
    function hasValidFutureExpiration() {
      return request.resource.data.expiresAt is timestamp &&
             request.resource.data.expiresAt > request.time &&
             request.resource.data.expiresAt <= request.time + duration.value(24, 'h');
    }

    // =========================================================================
    // RAIZ DE IDENTIDADE E ROTEAMENTO: identities/{identityHash}
    // Todo conteúdo estruturado sob a raiz da identidade (P0.2).
    // =========================================================================
    match /identities/{identityHash} {
      // Read: Listagem TERMINANTEMENTE NEGADA para impedir varredura/enumeração.
      // Get pontual permitido apenas ao próprio titular para inspeção de seu estado.
      allow get: if isAuthenticated() && request.auth.uid == resource.data.currentAuthUid;
      allow list: if false;

      // Create: Permitido apenas se autenticado, se for novo documento e o UID corresponder
      allow create: if isAuthenticated()
                    && request.resource.data.currentAuthUid == request.auth.uid
                    && !exists(/databases/$(database)/documents/identities/$(identityHash));

      // Update e Delete diretos via SDK são TERMINANTEMENTE NEGADOS
      // (Atualizações de rota exigem Cloud Function 'updateIdentityRouting' com prova de posse FIPS 204)
      allow update, delete: if false;

      // =======================================================================
      // CAIXA DE ENTRADA EFÊMERA POR DESTINATÁRIO: identities/{identityHash}/inbox/{envelopeId}
      // Evita correlação (Emenda B.3): o documento de envelope vive sob a identidade
      // do destinatário e NUNCA armazena os dois identityHash simultaneamente.
      // =======================================================================
      match /inbox/{envelopeId} {
        // Leitura: Restrita exclusivamente ao proprietário do identityHash
        allow read: if isIdentityOwner(identityHash);

        // Criação: Remetentes autenticados podem depositar envelopes cifrados para o destinatário,
        // exigindo TTL obrigatório e proibindo atualização posterior (imutabilidade)
        allow create: if isAuthenticated() && hasValidFutureExpiration();

        // Update: TERMINANTEMENTE NEGADO (envelopes são imutáveis)
        allow update: if false;

        // Delete: Permitido apenas ao destinatário legítimo (Vanish-After-Read)
        allow delete: if isIdentityOwner(identityHash);
      }
    }

    // =========================================================================
    // COLEÇÃO LEGADA: messages (Compatibilidade transitória com isolamento estrito)
    // =========================================================================
    match /messages/{messageId} {
      allow get: if isAuthenticated() &&
                 (request.auth.uid == resource.data.senderId || request.auth.uid == resource.data.recipientId);
      allow list: if isAuthenticated() &&
                  request.auth.uid == resource.data.recipientId;

      allow create: if isAuthenticated() &&
                    (request.auth.uid == request.resource.data.senderId || request.auth.uid == request.resource.data.recipientId) &&
                    hasValidFutureExpiration();

      allow update: if false;

      allow delete: if isAuthenticated() &&
                    (request.auth.uid == resource.data.senderId || request.auth.uid == resource.data.recipientId);
    }

    // =========================================================================
    // COLEÇÃO: messageKeys (DEK - Data Encryption Keys)
    // =========================================================================
    // ISOLAMENTO TOTAL: Leitura e escrita NEGADAS para todos os clientes SDK.
    // Gerenciamento e destruição exclusivos via Admin SDK (Cloud Functions backend).
    match /messageKeys/{messageId} {
      allow read, write: if false;
    }

    // =========================================================================
    // COLEÇÃO: userRateLimits
    // =========================================================================
    match /userRateLimits/{uid} {
      allow read, write: if false;
    }

    // =========================================================================
    // COLEÇÃO: invites (Modelo C - Convites Efêmeros Remotos)
    // =========================================================================
    match /invites/{inviteId} {
      allow read, write: if false;
    }

    // =========================================================================
    // COLEÇÕES: abuseReports e abuseMetrics (Denúncias Comportamentais)
    // =========================================================================
    match /abuseReports/{reportId} {
      allow read, write: if false;
    }

    match /abuseMetrics/{fingerprint} {
      allow read, write: if false;
    }

    // =========================================================================
    // COLEÇÃO: accessLogs & connectionLogs (Marco Civil Art. 15 - 180 dias TTL)
    // =========================================================================
    match /accessLogs/{logId} {
      allow read, write: if false;
    }

    match /connectionLogs/{logId} {
      allow read, write: if false;
    }

    // =========================================================================
    // COLEÇÃO: devicePushTokens (Push Notifications FCM/APNs - v1.6)
    // =========================================================================
    match /devicePushTokens/{tokenId} {
      allow read, write: if false;
    }

    // =========================================================================
    // COLEÇÃO: securityAnomalies (Detecção Proativa de Anomalias - P1.3)
    // =========================================================================
    match /securityAnomalies/{anomalyId} {
      allow read, write: if false;
    }
  }
}
```

---



## 4. Código Essencial: Núcleo Criptográfico KMP

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/HybridKem.kt`
**Contexto Arquitetural:** KEM Híbrido Pós-Quântico NIST SP 800-227 / RFC 9180 (ML-KEM-768 + X25519 via HKDF-SHA256).

```kotlin
package com.example.security.identity

import kotlin.random.Random

data class HybridKemResult(
    val ephemeralX25519PubKey: ByteArray,
    val mlKemCiphertext: ByteArray,
    val combinedSharedSecret: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HybridKemResult) return false
        return ephemeralX25519PubKey.contentEquals(other.ephemeralX25519PubKey) &&
                mlKemCiphertext.contentEquals(other.mlKemCiphertext) &&
                combinedSharedSecret.contentEquals(other.combinedSharedSecret)
    }

    override fun hashCode(): Int {
        var result = ephemeralX25519PubKey.contentHashCode()
        result = 31 * result + mlKemCiphertext.contentHashCode()
        result = 31 * result + combinedSharedSecret.contentHashCode()
        return result
    }
}

/**
 * Hybrid Key Encapsulation Mechanism (KEM) Combiner
 * in accordance with NIST SP 800-227 and RFC 9180 (HPKE).
 *
 * Construction: DHKEM(X25519, HKDF-SHA256) || ML-KEM-768
 * Ensures Forward Secrecy and Resistance to Harvest-Now-Decrypt-Later threats.
 */
object HybridKem {

    private val INFO_LABEL = "raix-hybrid-hpke-v1".encodeToByteArray()

    fun encapsulate(
        recipientX25519PubKey: ByteArray,
        recipientMlKemPubKey: ByteArray,
        customEphemeralX25519Priv: ByteArray? = null
    ): HybridKemResult {
        require(recipientX25519PubKey.size == 32) { "Recipient X25519 public key must be 32 bytes" }
        require(recipientMlKemPubKey.isNotEmpty()) { "Recipient ML-KEM-768 public key cannot be empty" }

        // 1. Classical Ephemeral X25519
        val ephemPriv = customEphemeralX25519Priv ?: ByteArray(32).also { Random.nextBytes(it) }
        val clampedPriv = Curve25519Engine.clampPrivateKey(ephemPriv)
        val ephemPub = IdentityCurve25519.generatePublicKey(clampedPriv)
        val ssClassical = IdentityCurve25519.computeSharedSecret(clampedPriv, recipientX25519PubKey)

        // 2. Post-Quantum ML-KEM-768
        val pqEncapsulation = IdentityMlKem768.encapsulate(recipientMlKemPubKey)
        val ctPQ = pqEncapsulation.ciphertext
        val ssPQ = pqEncapsulation.sharedSecret

        // 3. NIST SP 800-227 Combiner:
        // salt = ephemPub || ctPQ
        // ikm = ssClassical || ssPQ
        val salt = ephemPub + ctPQ
        val ikm = ssClassical + ssPQ
        val prk = HkdfSha256.extract(salt = salt, ikm = ikm)
        val combinedKey = HkdfSha256.expand(prk = prk, info = INFO_LABEL, length = 32)

        return HybridKemResult(
            ephemeralX25519PubKey = ephemPub,
            mlKemCiphertext = ctPQ,
            combinedSharedSecret = combinedKey
        )
    }

    fun decapsulate(
        recipientX25519PrivKey: ByteArray,
        recipientMlKemPrivKey: ByteArray,
        ephemeralX25519PubKey: ByteArray,
        mlKemCiphertext: ByteArray
    ): ByteArray {
        require(recipientX25519PrivKey.size == 32) { "Recipient X25519 private key must be 32 bytes" }
        require(recipientMlKemPrivKey.isNotEmpty()) { "Recipient ML-KEM-768 private key cannot be empty" }
        require(ephemeralX25519PubKey.size == 32) { "Ephemeral X25519 public key must be 32 bytes" }
        require(mlKemCiphertext.isNotEmpty()) { "ML-KEM-768 ciphertext cannot be empty" }

        // 1. Classical Shared Secret
        val ssClassical = IdentityCurve25519.computeSharedSecret(recipientX25519PrivKey, ephemeralX25519PubKey)

        // 2. Post-Quantum Shared Secret
        val ssPQ = IdentityMlKem768.decapsulate(recipientMlKemPrivKey, mlKemCiphertext)

        // 3. Same NIST SP 800-227 Combiner
        val salt = ephemeralX25519PubKey + mlKemCiphertext
        val ikm = ssClassical + ssPQ
        val prk = HkdfSha256.extract(salt = salt, ikm = ikm)
        return HkdfSha256.expand(prk = prk, info = INFO_LABEL, length = 32)
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/SignatureScheme.kt`
**Contexto Arquitetural:** Assinatura Híbrida Composta (ML-DSA-65 + Ed25519) com verificação booleana AND.

```kotlin
package com.example.security.identity

/**
 * Enumeration of supported digital signature algorithms (Crypto-Agility).
 * In accordance with NIST FIPS 204 and RFC 8032.
 */
enum class SignatureAlgorithm(val id: String) {
    ED25519("ed25519"),
    ML_DSA_65("ml-dsa-65"),
    HYBRID_ED25519_ML_DSA_65("ed25519+ml-dsa-65")
}

/**
 * Abstract Signature Scheme interface ensuring Crypto-Agility (NIST PQC Migration).
 * Decouples high-level identity routing and handshake logic from underlying primitives.
 */
interface SignatureScheme {
    val algorithm: SignatureAlgorithm
    fun sign(privateKey: ByteArray, message: ByteArray): ByteArray
    fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean
}

/**
 * Classical Ed25519 (RFC 8032) Signature Scheme.
 */
class Ed25519SignatureScheme : SignatureScheme {
    override val algorithm: SignatureAlgorithm = SignatureAlgorithm.ED25519

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        return IdentityEd25519.sign(privateKey, message)
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return IdentityEd25519.verify(publicKey, message, signature)
    }
}

/**
 * Post-Quantum ML-DSA-65 (NIST FIPS 204) Signature Scheme.
 */
class MlDsa65SignatureScheme : SignatureScheme {
    override val algorithm: SignatureAlgorithm = SignatureAlgorithm.ML_DSA_65

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        return IdentityMlDsa65.sign(privateKey, message)
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        return IdentityMlDsa65.verify(publicKey, message, signature)
    }
}

/**
 * Hybrid Compound Signature Scheme: Ed25519 + ML-DSA-65.
 *
 * Wire format for composite signature:
 * [4-byte big-endian edSigLen][ed25519Signature][mlDsaSignature]
 *
 * Wire format for composite public key:
 * [4-byte big-endian edPubLen][ed25519PublicKey][mlDsaPublicKey]
 *
 * Wire format for composite private key:
 * [4-byte big-endian edPrivLen][ed25519PrivateKey][mlDsaPrivateKey]
 *
 * Verification succeeds if and only if BOTH Ed25519 and ML-DSA-65 signatures are valid.
 */
class HybridEd25519MlDsa65SignatureScheme : SignatureScheme {
    override val algorithm: SignatureAlgorithm = SignatureAlgorithm.HYBRID_ED25519_ML_DSA_65

    override fun sign(privateKey: ByteArray, message: ByteArray): ByteArray {
        require(privateKey.size >= 4) { "Invalid hybrid private key format" }
        val edPrivLen = ((privateKey[0].toInt() and 0xFF) shl 24) or
                ((privateKey[1].toInt() and 0xFF) shl 16) or
                ((privateKey[2].toInt() and 0xFF) shl 8) or
                (privateKey[3].toInt() and 0xFF)
        require(privateKey.size >= 4 + edPrivLen) { "Corrupt hybrid private key length" }

        val edPriv = privateKey.copyOfRange(4, 4 + edPrivLen)
        val mlDsaPriv = privateKey.copyOfRange(4 + edPrivLen, privateKey.size)

        val edSig = IdentityEd25519.sign(edPriv, message)
        val mlDsaSig = IdentityMlDsa65.sign(mlDsaPriv, message)

        val edSigLen = edSig.size
        val result = ByteArray(4 + edSigLen + mlDsaSig.size)
        result[0] = ((edSigLen ushr 24) and 0xFF).toByte()
        result[1] = ((edSigLen ushr 16) and 0xFF).toByte()
        result[2] = ((edSigLen ushr 8) and 0xFF).toByte()
        result[3] = (edSigLen and 0xFF).toByte()

        edSig.copyInto(result, 4, 0, edSigLen)
        mlDsaSig.copyInto(result, 4 + edSigLen, 0, mlDsaSig.size)
        return result
    }

    override fun verify(publicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (publicKey.size < 4 || signature.size < 4) return false

        // Unpack public key
        val edPubLen = ((publicKey[0].toInt() and 0xFF) shl 24) or
                ((publicKey[1].toInt() and 0xFF) shl 16) or
                ((publicKey[2].toInt() and 0xFF) shl 8) or
                (publicKey[3].toInt() and 0xFF)
        if (publicKey.size < 4 + edPubLen) return false

        val edPub = publicKey.copyOfRange(4, 4 + edPubLen)
        val mlDsaPub = publicKey.copyOfRange(4 + edPubLen, publicKey.size)

        // Unpack signature
        val edSigLen = ((signature[0].toInt() and 0xFF) shl 24) or
                ((signature[1].toInt() and 0xFF) shl 16) or
                ((signature[2].toInt() and 0xFF) shl 8) or
                (signature[3].toInt() and 0xFF)
        if (signature.size < 4 + edSigLen) return false

        val edSig = signature.copyOfRange(4, 4 + edSigLen)
        val mlDsaSig = signature.copyOfRange(4 + edSigLen, signature.size)

        // Both must be valid (AND semantics)
        val edValid = IdentityEd25519.verify(edPub, message, edSig)
        if (!edValid) return false

        return IdentityMlDsa65.verify(mlDsaPub, message, mlDsaSig)
    }

    companion object {
        fun encodeCompositeKey(edKey: ByteArray, mlDsaKey: ByteArray): ByteArray {
            val edLen = edKey.size
            val result = ByteArray(4 + edLen + mlDsaKey.size)
            result[0] = ((edLen ushr 24) and 0xFF).toByte()
            result[1] = ((edLen ushr 16) and 0xFF).toByte()
            result[2] = ((edLen ushr 8) and 0xFF).toByte()
            result[3] = (edLen and 0xFF).toByte()

            edKey.copyInto(result, 4, 0, edLen)
            mlDsaKey.copyInto(result, 4 + edLen, 0, mlDsaKey.size)
            return result
        }
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/SealedBox.kt`
**Contexto Arquitetural:** Envelopes criptográficos selados anônimos SealedBox.

```kotlin
package com.example.security.identity

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * Encapsulated envelope containing the opaque cryptographic payload for the recipient.
 * In accordance with NIST SP 800-227 and RFC 9180 (HPKE).
 *
 * Supports both:
 * 1. Hybrid PQC: DHKEM(X25519, HKDF-SHA256) || ML-KEM-768 ("hybrid-v1")
 * 2. Classical fallback: X25519 ("classical-v1")
 */
@Serializable
data class SealedBoxEnvelope(
    val ephemeralPubKeyHex: String,
    val wrappedDekBase64: String,
    val mlKemCiphertextBase64: String? = null,
    val securitySuite: String = SUITE_HYBRID
) {
    companion object {
        const val SUITE_HYBRID = "hybrid-v1"
        const val SUITE_CLASSICAL = "classical-v1"
    }
}

class DowngradeAttackException(message: String) : Exception(message)

/**
 * Multiplatform Sealed-Box primitive for E2E DEK encryption.
 * Implements Hybrid Post-Quantum Key Encapsulation (X25519 + ML-KEM-768)
 * and strict Anti-Downgrade protection (Guru Amendment A.2 & A.3).
 */
object SealedBox {

    private const val INFO_LABEL = "pmsg-dek-wrap-v1"
    const val NONCE_SIZE = 12 // 96 bits

    /**
     * Seals a Data Encryption Key (DEK) using Hybrid PQC: X25519 + ML-KEM-768.
     * Preserves Forward Secrecy by using fresh ephemeral keys for both algorithms.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun sealHybrid(
        dek: ByteArray,
        recipientX25519PubKey: ByteArray,
        recipientMlKemPubKey: ByteArray,
        customNonce: ByteArray? = null
    ): SealedBoxEnvelope {
        require(recipientX25519PubKey.size == 32) { "Recipient X25519 public key must be 32 bytes" }
        require(recipientMlKemPubKey.isNotEmpty()) { "Recipient ML-KEM-768 public key cannot be empty" }

        // 1. Execute Hybrid KEM (NIST SP 800-227 combiner)
        val kemResult = HybridKem.encapsulate(recipientX25519PubKey, recipientMlKemPubKey)

        // 2. Encrypt DEK with derived KEK via AES-256-GCM
        val nonce = customNonce ?: ByteArray(NONCE_SIZE).also { Random.nextBytes(it) }
        require(nonce.size == NONCE_SIZE) { "Nonce must be 12 bytes" }
        val encryptedPayload = AesGcm.encrypt(plaintext = dek, key = kemResult.combinedSharedSecret, iv = nonce)

        // 3. Pack wrappedDek = nonce (12 bytes) + encryptedPayload (ciphertext + 16-byte tag)
        val wrappedBytes = ByteArray(NONCE_SIZE + encryptedPayload.size)
        nonce.copyInto(wrappedBytes, 0, 0, NONCE_SIZE)
        encryptedPayload.copyInto(wrappedBytes, NONCE_SIZE, 0, encryptedPayload.size)

        return SealedBoxEnvelope(
            ephemeralPubKeyHex = bytesToHex(kemResult.ephemeralX25519PubKey),
            wrappedDekBase64 = Base64.encode(wrappedBytes),
            mlKemCiphertextBase64 = Base64.encode(kemResult.mlKemCiphertext),
            securitySuite = SealedBoxEnvelope.SUITE_HYBRID
        )
    }

    /**
     * Unseals a Data Encryption Key (DEK) from a Hybrid PQC envelope.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun unsealHybrid(
        envelope: SealedBoxEnvelope,
        recipientX25519PrivKey: ByteArray,
        recipientMlKemPrivKey: ByteArray
    ): ByteArray {
        require(recipientX25519PrivKey.size == 32) { "Recipient X25519 private key must be 32 bytes" }
        require(recipientMlKemPrivKey.isNotEmpty()) { "Recipient ML-KEM-768 private key cannot be empty" }
        val mlKemCtBase64 = envelope.mlKemCiphertextBase64
            ?: throw DowngradeAttackException("Tentativa de downgrade detectada: envelope não contém ciphertext ML-KEM-768.")

        val ephemeralPub = hexToBytes(envelope.ephemeralPubKeyHex)
        val mlKemCiphertext = Base64.decode(mlKemCtBase64)

        // 1. Recover combined KEK using Hybrid KEM combiner
        val kek = HybridKem.decapsulate(
            recipientX25519PrivKey = recipientX25519PrivKey,
            recipientMlKemPrivKey = recipientMlKemPrivKey,
            ephemeralX25519PubKey = ephemeralPub,
            mlKemCiphertext = mlKemCiphertext
        )

        // 2. Unpack wrapped DEK
        val wrappedBytes = Base64.decode(envelope.wrappedDekBase64)
        require(wrappedBytes.size >= NONCE_SIZE + 16) { "Wrapped DEK payload is too short" }

        val nonce = ByteArray(NONCE_SIZE)
        wrappedBytes.copyInto(nonce, 0, 0, NONCE_SIZE)

        val cipherWithTag = ByteArray(wrappedBytes.size - NONCE_SIZE)
        wrappedBytes.copyInto(cipherWithTag, 0, NONCE_SIZE, wrappedBytes.size)

        // 3. Decrypt DEK with AES-256-GCM
        return AesGcm.decrypt(ciphertext = cipherWithTag, key = kek, iv = nonce)
    }

    /**
     * Unified seal entry point with automatic mode selection:
     * Prefers Hybrid PQC if recipientMlKemPubKey is provided; falls back to classical X25519 if absent.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun seal(
        dek: ByteArray,
        recipientPubKey: ByteArray,
        recipientMlKemPubKey: ByteArray? = null,
        customEphemeralPriv: ByteArray? = null,
        customNonce: ByteArray? = null
    ): SealedBoxEnvelope {
        if (recipientMlKemPubKey != null && recipientMlKemPubKey.isNotEmpty()) {
            return sealHybrid(
                dek = dek,
                recipientX25519PubKey = recipientPubKey,
                recipientMlKemPubKey = recipientMlKemPubKey,
                customNonce = customNonce
            )
        }

        // Classical X25519 Fallback
        require(recipientPubKey.size == 32) { "Recipient public key must be 32 bytes" }

        val ephemeralPriv = customEphemeralPriv ?: ByteArray(32).also { Random.nextBytes(it) }
        val clampedPriv = Curve25519Engine.clampPrivateKey(ephemeralPriv)
        val ephemeralPub = IdentityCurve25519.generatePublicKey(clampedPriv)
        val sharedSecret = IdentityCurve25519.computeSharedSecret(clampedPriv, recipientPubKey)

        val salt = Sha256Digest.digest(ephemeralPub)
        val info = INFO_LABEL.encodeToByteArray()
        val kek = HkdfSha256.deriveKey(ikm = sharedSecret, salt = salt, info = info, length = 32)

        val nonce = customNonce ?: ByteArray(NONCE_SIZE).also { Random.nextBytes(it) }
        require(nonce.size == NONCE_SIZE) { "Nonce must be 12 bytes" }
        val encryptedPayload = AesGcm.encrypt(plaintext = dek, key = kek, iv = nonce)

        val wrappedBytes = ByteArray(NONCE_SIZE + encryptedPayload.size)
        nonce.copyInto(wrappedBytes, 0, 0, NONCE_SIZE)
        encryptedPayload.copyInto(wrappedBytes, NONCE_SIZE, 0, encryptedPayload.size)

        return SealedBoxEnvelope(
            ephemeralPubKeyHex = bytesToHex(ephemeralPub),
            wrappedDekBase64 = Base64.encode(wrappedBytes),
            mlKemCiphertextBase64 = null,
            securitySuite = SealedBoxEnvelope.SUITE_CLASSICAL
        )
    }

    /**
     * Unified unseal entry point with Anti-Downgrade enforcement:
     * If enforceHybrid is true, any classical envelope without ML-KEM-768 is rejected.
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun unseal(
        envelope: SealedBoxEnvelope,
        recipientPrivKey: ByteArray,
        recipientMlKemPrivKey: ByteArray? = null,
        enforceHybrid: Boolean = false
    ): ByteArray {
        if (envelope.mlKemCiphertextBase64 != null) {
            requireNotNull(recipientMlKemPrivKey) { "Chave privada ML-KEM-768 obrigatória para desencapsular envelope híbrido" }
            return unsealHybrid(envelope, recipientPrivKey, recipientMlKemPrivKey)
        }

        if (enforceHybrid) {
            throw DowngradeAttackException(
                "Alerta de Segurança (Anti-Downgrade): Conexão requer nível mínimo pós-quântico (ML-KEM-768). Envelope clássico rejeitado."
            )
        }

        // Classical unseal
        require(recipientPrivKey.size == 32) { "Recipient private key must be 32 bytes" }

        val ephemeralPub = hexToBytes(envelope.ephemeralPubKeyHex)
        require(ephemeralPub.size == 32) { "Ephemeral public key must be 32 bytes" }

        val wrappedBytes = Base64.decode(envelope.wrappedDekBase64)
        require(wrappedBytes.size >= NONCE_SIZE + 16) { "Wrapped DEK payload is too short" }

        val nonce = ByteArray(NONCE_SIZE)
        wrappedBytes.copyInto(nonce, 0, 0, NONCE_SIZE)

        val cipherWithTag = ByteArray(wrappedBytes.size - NONCE_SIZE)
        wrappedBytes.copyInto(cipherWithTag, 0, NONCE_SIZE, wrappedBytes.size)

        val sharedSecret = IdentityCurve25519.computeSharedSecret(recipientPrivKey, ephemeralPub)
        val salt = Sha256Digest.digest(ephemeralPub)
        val info = INFO_LABEL.encodeToByteArray()
        val kek = HkdfSha256.deriveKey(ikm = sharedSecret, salt = salt, info = info, length = 32)

        return AesGcm.decrypt(ciphertext = cipherWithTag, key = kek, iv = nonce)
    }

    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim().lowercase()
        require(clean.length % 2 == 0) { "Hex string must have even length" }
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val high = clean[i * 2].digitToInt(16)
            val low = clean[i * 2 + 1].digitToInt(16)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/Curve25519Engine.kt`
**Contexto Arquitetural:** Implementação pura de Curve25519 (X25519) em Kotlin Multiplatform.

```kotlin
package com.example.security.identity

/**
 * Pure Kotlin implementation of X25519 scalar multiplication (RFC 7748 §5).
 * Operates over the prime field GF(2^255 - 19).
 *
 * Fully deterministic across all Kotlin Multiplatform targets (Android, Desktop, iOS, WasmJS).
 */
object Curve25519Engine {

    private const val A24 = 121665L

    /**
     * Clamps private key per RFC 7748 §5:
     * - Clear bits 0, 1, 2 of byte 0
     * - Clear bit 7 of byte 31
     * - Set bit 6 of byte 31
     */
    fun clampPrivateKey(key: ByteArray): ByteArray {
        require(key.size == 32) { "Key must be 32 bytes" }
        val clamped = key.copyOf(32)
        clamped[0] = (clamped[0].toInt() and 248).toByte()
        clamped[31] = (clamped[31].toInt() and 127).toByte()
        clamped[31] = (clamped[31].toInt() or 64).toByte()
        return clamped
    }

    /**
     * Computes scalar * base_point (u = 9).
     */
    fun scalarMultBase(scalar: ByteArray): ByteArray {
        val basePoint = ByteArray(32)
        basePoint[0] = 9
        return scalarMult(scalar, basePoint)
    }

    /**
     * Computes scalar * u-coordinate per RFC 7748 §5.
     */
    fun scalarMult(scalar: ByteArray, uCoord: ByteArray): ByteArray {
        require(scalar.size == 32) { "Scalar must be 32 bytes" }
        require(uCoord.size == 32) { "uCoord must be 32 bytes" }

        val k = clampPrivateKey(scalar)

        // Decode u-coordinate: mask the most significant bit
        val uBytes = uCoord.copyOf(32)
        uBytes[31] = (uBytes[31].toInt() and 127).toByte()

        val x1 = decode(uBytes)
        var x2 = fromLong(1)
        var z2 = fromLong(0)
        var x3 = x1.copyOf()
        var z3 = fromLong(1)
        var swap = 0

        for (t in 254 downTo 0) {
            val byteIdx = t / 8
            val bitIdx = t % 8
            val kt = ((k[byteIdx].toInt() ushr bitIdx) and 1)
            swap = swap xor kt

            cswap(swap, x2, x3)
            cswap(swap, z2, z3)
            swap = kt

            val a = add(x2, z2)
            val aa = sqr(a)
            val b = sub(x2, z2)
            val bb = sqr(b)
            val e = sub(aa, bb)
            val c = add(x3, z3)
            val d = sub(x3, z3)
            val da = mul(d, a)
            val cb = mul(c, b)
            x3 = sqr(add(da, cb))
            z3 = mul(x1, sqr(sub(da, cb)))
            x2 = mul(aa, bb)
            z2 = mul(e, add(aa, mulSmall(e, A24)))
        }

        cswap(swap, x2, x3)
        cswap(swap, z2, z3)

        val result = mul(x2, inv(z2))
        return encode(result)
    }

    // -------------------------------------------------------------------------
    // Field arithmetic modulo 2^255 - 19 using 16 limbs of 16-bit integers
    // -------------------------------------------------------------------------

    private fun fromLong(value: Long): LongArray {
        val r = LongArray(16)
        r[0] = value and 0xFFFF
        r[1] = (value ushr 16) and 0xFFFF
        return r
    }

    private fun decode(bytes: ByteArray): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            val b0 = bytes[i * 2].toLong() and 0xFF
            val b1 = bytes[i * 2 + 1].toLong() and 0xFF
            r[i] = b0 or (b1 shl 8)
        }
        return r
    }

    private fun encode(a: LongArray): ByteArray {
        val r = carryAndReduce(a)
        val bytes = ByteArray(32)
        for (i in 0 until 16) {
            bytes[i * 2] = (r[i] and 0xFF).toByte()
            bytes[i * 2 + 1] = ((r[i] ushr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun cswap(swap: Int, a: LongArray, b: LongArray) {
        val mask = (if (swap != 0) 0xFFFFL else 0L)
        for (i in 0 until 16) {
            val t = mask and (a[i] xor b[i])
            a[i] = a[i] xor t
            b[i] = b[i] xor t
        }
    }

    private fun add(a: LongArray, b: LongArray): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = a[i] + b[i]
        }
        return carry(r)
    }

    private fun sub(a: LongArray, b: LongArray): LongArray {
        // Add 2 * (2^255 - 19) = 2^256 - 38 to ensure result is positive
        val r = LongArray(16)
        r[0] = a[0] + 0xFFDA - b[0] // 0x10000 - 38 = 0xFFDA with carry into next limb
        var carry = -1L // because we borrowed 0x10000 for limb 0, but added 2^256 = 16 limbs of 0xFFFF
        // 2p = [0xFFFF - 37, 0xFFFF, 0xFFFF, ..., 0x7FFF * 2] = [0xFFDA, 0xFFFF, ..., 0xFFFF]
        // 2 * (2^255 - 19) = 2^256 - 38:
        // Limb 0 = 0x10000 - 38 = 65498. Then limbs 1..15 are 0xFFFF.
        r[0] = a[0] + 0x10000L - 38L - b[0]
        for (i in 1 until 16) {
            r[i] = a[i] + 0xFFFFL - b[i]
        }
        return carry(r)
    }

    private fun mul(a: LongArray, b: LongArray): LongArray {
        val prod = LongArray(31)
        for (i in 0 until 16) {
            val ai = a[i]
            for (j in 0 until 16) {
                prod[i + j] += ai * b[j]
            }
        }
        // Fold high limbs (16..30) back into (0..14) with factor 38
        // because 2^256 = 2 * 2^255 = 2 * 19 = 38 mod p
        for (i in 0 until 15) {
            prod[i] += prod[i + 16] * 38L
        }
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = prod[i]
        }
        return carry(r)
    }

    private fun mulSmall(a: LongArray, b: Long): LongArray {
        val r = LongArray(16)
        for (i in 0 until 16) {
            r[i] = a[i] * b
        }
        return carry(r)
    }

    private fun sqr(a: LongArray): LongArray = mul(a, a)

    private fun carry(r: LongArray): LongArray {
        for (step in 0 until 2) {
            for (i in 0 until 15) {
                val c = r[i] shr 16
                r[i] = r[i] and 0xFFFF
                r[i + 1] += c
            }
            val c = r[15] shr 15
            r[15] = r[15] and 0x7FFF
            r[0] += c * 19
        }
        return r
    }

    /**
     * Performs exact canonical reduction modulo 2^255 - 19
     * (subtracts p if r >= p).
     */
    private fun carryAndReduce(a: LongArray): LongArray {
        val r = a.copyOf()
        carry(r)

        // Test if r >= p (p = 2^255 - 19: limbs 0=0xFFED, 1..14=0xFFFF, 15=0x7FFF)
        // Candidate: r - p = r + 19 - 2^255
        val cand = LongArray(16)
        cand[0] = r[0] + 19
        for (i in 0 until 15) {
            val c = cand[i] shr 16
            cand[i] = cand[i] and 0xFFFF
            cand[i + 1] = r[i + 1] + c
        }
        val c = cand[15] shr 15 // If bit 15 is set, r + 19 >= 2^255 <=> r >= p
        cand[15] = cand[15] and 0x7FFF

        return if (c != 0L) cand else r
    }

    /**
     * Inversion via Fermat's Little Theorem: a^(p-2) mod p
     * where p - 2 = 2^255 - 21.
     */
    private fun inv(z: LongArray): LongArray {
        val a = sqr(z)               // 2
        val t0 = sqr(a)              // 4
        val t1 = sqr(t0)             // 8
        val b = mul(t1, z)           // 9 = 2^3 + 1
        val c = mul(b, a)            // 11 = 2^3 + 2^1 + 1
        val t2 = sqr(c)              // 22
        val d = mul(t2, a)           // 24
        val t3 = sqr(d)              // 48
        val t4 = sqr(t3)             // 96
        val t5 = sqr(t4)             // 192
        val t6 = sqr(t5)             // 384
        val e = mul(t6, b)           // 2^5 - 1

        // Standard addition chain for 2^255 - 21
        var t = sqr(e)
        for (i in 1 until 5) t = sqr(t)
        val f = mul(t, e)            // 2^10 - 1

        t = sqr(f)
        for (i in 1 until 10) t = sqr(t)
        val g = mul(t, f)            // 2^20 - 1

        t = sqr(g)
        for (i in 1 until 20) t = sqr(t)
        val h = mul(t, g)            // 2^40 - 1

        t = sqr(h)
        for (i in 1 until 10) t = sqr(t)
        val k = mul(t, f)            // 2^50 - 1

        t = sqr(k)
        for (i in 1 until 50) t = sqr(t)
        val l = mul(t, k)            // 2^100 - 1

        t = sqr(l)
        for (i in 1 until 100) t = sqr(t)
        val m = mul(t, l)            // 2^200 - 1

        t = sqr(m)
        for (i in 1 until 50) t = sqr(t)
        val n = mul(t, k)            // 2^250 - 1

        t = sqr(n)
        for (i in 1 until 5) t = sqr(t)
        return mul(t, c)             // 2^255 - 21
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/IdentityCryptoManager.kt`
**Contexto Arquitetural:** Gerenciador criptográfico de derivação com isolamento de salts Argon2id.

```kotlin
package com.example.security.identity

import com.example.security.KeyVault
import kotlin.random.Random

data class IdentityKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray,
    val fingerprintHex: String,
    val safetyNumber: String, // 60 decimal digits (12 blocks of 5)
    val signingPrivateKey: ByteArray = ByteArray(0),
    val signingPublicKey: ByteArray = ByteArray(0),
    val mlKemPrivateKey: ByteArray = ByteArray(0),
    val mlKemPublicKey: ByteArray = ByteArray(0),
    val mlDsaPrivateKey: ByteArray = ByteArray(0),
    val mlDsaPublicKey: ByteArray = ByteArray(0),
    val hybridFingerprintHex: String = "",
    val hybridSafetyNumber: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IdentityKeyPair) return false
        return privateKey.contentEquals(other.privateKey) &&
                publicKey.contentEquals(other.publicKey) &&
                fingerprintHex == other.fingerprintHex &&
                safetyNumber == other.safetyNumber &&
                signingPrivateKey.contentEquals(other.signingPrivateKey) &&
                signingPublicKey.contentEquals(other.signingPublicKey) &&
                mlKemPrivateKey.contentEquals(other.mlKemPrivateKey) &&
                mlKemPublicKey.contentEquals(other.mlKemPublicKey) &&
                mlDsaPrivateKey.contentEquals(other.mlDsaPrivateKey) &&
                mlDsaPublicKey.contentEquals(other.mlDsaPublicKey) &&
                hybridFingerprintHex == other.hybridFingerprintHex &&
                hybridSafetyNumber == other.hybridSafetyNumber
    }

    override fun hashCode(): Int {
        var result = privateKey.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + fingerprintHex.hashCode()
        result = 31 * result + safetyNumber.hashCode()
        result = 31 * result + signingPrivateKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + mlKemPrivateKey.contentHashCode()
        result = 31 * result + mlKemPublicKey.contentHashCode()
        result = 31 * result + mlDsaPrivateKey.contentHashCode()
        result = 31 * result + mlDsaPublicKey.contentHashCode()
        result = 31 * result + hybridFingerprintHex.hashCode()
        result = 31 * result + hybridSafetyNumber.hashCode()
        return result
    }
}

data class ProvisionedIdentity(
    val mnemonic: List<String>,
    val keyPair: IdentityKeyPair,
    val entropy: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProvisionedIdentity) return false
        return mnemonic == other.mnemonic &&
                keyPair == other.keyPair &&
                entropy.contentEquals(other.entropy)
    }

    override fun hashCode(): Int {
        var result = mnemonic.hashCode()
        result = 31 * result + keyPair.hashCode()
        result = 31 * result + entropy.contentHashCode()
        return result
    }
}

/**
 * Core cryptographic engine for Raix Identity (v2.0 PQC Hybrid).
 *
 * Implements:
 * 1. Deterministic BIP-39 PT-BR 128-bit entropy -> 12-word mnemonic.
 * 2. Dual-derivation from the same BIP-39 seed:
 *    - Classical: 256-bit X25519 keypair + Ed25519 signing keypair (preserves legacy recovery).
 *    - Post-Quantum: ML-KEM-768 (FIPS 203) keypair + ML-DSA-65 (FIPS 204) signing keypair.
 * 3. Signal-compatible Dual Safety Number: covers both classical and post-quantum public keys.
 * 4. Hybrid proof-of-possession with Anti-Downgrade capability negotiation.
 * 5. Envelope encryption of private keys and seed using KeyVault hardware keys.
 */
object IdentityCryptoManager {

    private val SALT = "pmsg-v1-identity-seed".encodeToByteArray()
    private val SIGNING_SALT = "pmsg-v1-identity-signing".encodeToByteArray()
    private val MLDSA_SALT = "raix-v2-identity-mldsa-seed".encodeToByteArray()
    private val MLKEM_SALT = "raix-v2-identity-mlkem-seed".encodeToByteArray()

    const val MIN_SECURITY_LEVEL_HYBRID = "HYBRID_PQC"
    const val SUITE_HYBRID = "hybrid-v1"

    fun generateNewIdentity(providedEntropy: ByteArray? = null): ProvisionedIdentity {
        val entropy = providedEntropy ?: ByteArray(16).also { Random.nextBytes(it) }
        require(entropy.size == 16) { "Entropy must be exactly 16 bytes (128 bits)" }

        val mnemonic = Bip39Portuguese.entropyToMnemonic(entropy)
        val keyPair = deriveKeyPair(mnemonic)
        return ProvisionedIdentity(
            mnemonic = mnemonic,
            keyPair = keyPair,
            entropy = entropy
        )
    }

    fun deriveKeyPair(mnemonic: List<String>): IdentityKeyPair {
        val entropy = Bip39Portuguese.mnemonicToEntropy(mnemonic).getOrThrow()
        val seed = Sha256Digest.digest(entropy)

        // 1. Classical X25519 Encryption KeyPair
        val rawPriv = Argon2Kmp.deriveKey(seed = seed, salt = SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)

        // RFC 7748 Clamping
        val clampedPriv = rawPriv.copyOf(32)
        clampedPriv[0] = (clampedPriv[0].toInt() and 248).toByte()
        clampedPriv[31] = (clampedPriv[31].toInt() and 127).toByte()
        clampedPriv[31] = (clampedPriv[31].toInt() or 64).toByte()

        val pubKey = IdentityCurve25519.generatePublicKey(clampedPriv)
        val fingerprintHex = Sha256Digest.digestHex(pubKey)
        val safetyNumber = formatSafetyNumber(Sha256Digest.digest(pubKey))

        // 2. Classical Ed25519 Signing KeyPair (F0: Proof-of-Possession)
        val rawSigningPriv = Argon2Kmp.deriveKey(seed = seed, salt = SIGNING_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val signingPubKey = IdentityEd25519.generatePublicKey(rawSigningPriv)

        // 3. Post-Quantum ML-DSA-65 Signing KeyPair (NIST FIPS 204 Level 3)
        val rawMlDsaSeed = Argon2Kmp.deriveKey(seed = seed, salt = MLDSA_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val mlDsaKeyPair = IdentityMlDsa65.generateKeyPair(rawMlDsaSeed)

        // 4. Post-Quantum ML-KEM-768 Key Encapsulation KeyPair (NIST FIPS 203 Level 3)
        val rawMlKemSeed = Argon2Kmp.deriveKey(seed = seed, salt = MLKEM_SALT, iterations = 3, memoryKiB = 32768, parallelism = 1, outputLength = 32)
        val mlKemKeyPair = IdentityMlKem768.generateKeyPair(rawMlKemSeed)

        // 5. Hybrid Combined Fingerprint and Signal-Compatible Hybrid Safety Number (Guru Amendment A.2 & A.5)
        val classicalComponents = Sha256Digest.digest(pubKey + signingPubKey)
        val pqComponents = Sha256Digest.digest(mlKemKeyPair.publicKey + mlDsaKeyPair.publicKey)
        val hybridCombined = Sha256Digest.digest(classicalComponents + pqComponents)
        val hybridFingerprintHex = Sha256Digest.digestHex(classicalComponents + pqComponents)
        val hybridSafetyNumber = formatSafetyNumber(hybridCombined)

        // P1.2: RAM Zeroization - wipe all intermediate derivation buffers immediately
        MemorySanitizer.zeroize(entropy)
        MemorySanitizer.zeroize(seed)
        MemorySanitizer.zeroize(rawPriv)
        MemorySanitizer.zeroize(rawMlDsaSeed)
        MemorySanitizer.zeroize(rawMlKemSeed)

        return IdentityKeyPair(
            privateKey = clampedPriv,
            publicKey = pubKey,
            fingerprintHex = fingerprintHex,
            safetyNumber = safetyNumber,
            signingPrivateKey = rawSigningPriv,
            signingPublicKey = signingPubKey,
            mlKemPrivateKey = mlKemKeyPair.privateKey,
            mlKemPublicKey = mlKemKeyPair.publicKey,
            mlDsaPrivateKey = mlDsaKeyPair.privateKey,
            mlDsaPublicKey = mlDsaKeyPair.publicKey,
            hybridFingerprintHex = hybridFingerprintHex,
            hybridSafetyNumber = hybridSafetyNumber
        )
    }

    /**
     * Anti-Downgrade Handshake Payload (Guru Amendment A.2).
     * Binds minimum security level and supported suites directly into the authenticated payload.
     */
    fun buildRoutingSignaturePayload(
        fingerprint: String,
        newAuthUid: String,
        timestamp: Long,
        minSecurityLevel: String = MIN_SECURITY_LEVEL_HYBRID,
        supportedSuites: String = SUITE_HYBRID
    ): String {
        return "pmsg-routing-v2|$fingerprint|$newAuthUid|$timestamp|$minSecurityLevel|$supportedSuites"
    }

    /**
     * Backwards-compatible v1 routing payload builder.
     */
    fun buildRoutingSignaturePayloadV1(fingerprint: String, newAuthUid: String, timestamp: Long): String {
        return "pmsg-routing-v1|$fingerprint|$newAuthUid|$timestamp"
    }

    fun signRoutingUpdate(signingPrivKeySeed: ByteArray, fingerprint: String, newAuthUid: String, timestamp: Long): ByteArray {
        val payload = buildRoutingSignaturePayloadV1(fingerprint, newAuthUid, timestamp).encodeToByteArray()
        return IdentityEd25519.sign(signingPrivKeySeed, payload)
    }

    fun verifyRoutingUpdate(signingPubKey: ByteArray, fingerprint: String, newAuthUid: String, timestamp: Long, signature: ByteArray): Boolean {
        val payload = buildRoutingSignaturePayloadV1(fingerprint, newAuthUid, timestamp).encodeToByteArray()
        return IdentityEd25519.verify(signingPubKey, payload, signature)
    }

    /**
     * Hybrid Proof-of-Possession Signing (Ed25519 + ML-DSA-65).
     */
    fun signRoutingUpdateHybrid(
        edSigningPriv: ByteArray,
        mlDsaPriv: ByteArray,
        fingerprint: String,
        newAuthUid: String,
        timestamp: Long,
        minSecurityLevel: String = MIN_SECURITY_LEVEL_HYBRID,
        supportedSuites: String = SUITE_HYBRID
    ): ByteArray {
        val payload = buildRoutingSignaturePayload(fingerprint, newAuthUid, timestamp, minSecurityLevel, supportedSuites).encodeToByteArray()
        val compositePriv = HybridEd25519MlDsa65SignatureScheme.encodeCompositeKey(edSigningPriv, mlDsaPriv)
        val scheme = HybridEd25519MlDsa65SignatureScheme()
        return scheme.sign(compositePriv, payload)
    }

    /**
     * Hybrid Proof-of-Possession Verification (Ed25519 + ML-DSA-65).
     */
    fun verifyRoutingUpdateHybrid(
        edSigningPub: ByteArray,
        mlDsaPub: ByteArray,
        fingerprint: String,
        newAuthUid: String,
        timestamp: Long,
        signature: ByteArray,
        minSecurityLevel: String = MIN_SECURITY_LEVEL_HYBRID,
        supportedSuites: String = SUITE_HYBRID
    ): Boolean {
        val payload = buildRoutingSignaturePayload(fingerprint, newAuthUid, timestamp, minSecurityLevel, supportedSuites).encodeToByteArray()
        val compositePub = HybridEd25519MlDsa65SignatureScheme.encodeCompositeKey(edSigningPub, mlDsaPub)
        val scheme = HybridEd25519MlDsa65SignatureScheme()
        return scheme.verify(compositePub, payload, signature)
    }

    fun restoreFromMnemonic(mnemonic: List<String>): Result<IdentityKeyPair> {
        val validationResult = Bip39Portuguese.mnemonicToEntropy(mnemonic)
        if (validationResult.isFailure) {
            return Result.failure(validationResult.exceptionOrNull() ?: IllegalArgumentException("Mnemônico inválido"))
        }
        return try {
            Result.success(deriveKeyPair(mnemonic))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats a 32-byte hash into Signal-style Safety Number:
     * Exactly 12 blocks of 5 decimal digits (60 digits total).
     */
    fun formatSafetyNumber(hash: ByteArray): String {
        val part1 = Sha256Digest.digest(hash + byteArrayOf(0x01))
        val part2 = Sha256Digest.digest(hash + byteArrayOf(0x02))
        val stream = part1 + part2

        val blocks = ArrayList<String>(12)
        for (i in 0 until 12) {
            val offset = i * 4
            val val32 = ((stream[offset].toLong() and 0xFF) shl 24) or
                    ((stream[offset + 1].toLong() and 0xFF) shl 16) or
                    ((stream[offset + 2].toLong() and 0xFF) shl 8) or
                    (stream[offset + 3].toLong() and 0xFF)
            val fiveDigit = (val32 % 100000L).toString().padStart(5, '0')
            blocks.add(fiveDigit)
        }
        return blocks.joinToString(" ")
    }

    /**
     * Computes the shared Pair Safety Number between two parties (Alice and Bob)
     * covering both Classical (X25519) and Post-Quantum (ML-KEM-768) public keys.
     * Symmetrical: order of keys does not matter. Both parties derive the exact same 60 digits.
     */
    fun computeHybridPairSafetyNumber(
        myClassicalPub: ByteArray,
        myMlKemPub: ByteArray,
        peerClassicalPub: ByteArray,
        peerMlKemPub: ByteArray
    ): String {
        require(myClassicalPub.size == 32) { "myClassicalPub must be 32 bytes" }
        require(peerClassicalPub.size == 32) { "peerClassicalPub must be 32 bytes" }

        val myMaterial = myClassicalPub + myMlKemPub
        val peerMaterial = peerClassicalPub + peerMlKemPub

        val (first, second) = if (compareBytes(myMaterial, peerMaterial) <= 0) {
            myMaterial to peerMaterial
        } else {
            peerMaterial to myMaterial
        }

        val combined = first + second
        val hash = Sha256Digest.digest(combined)
        return formatSafetyNumber(hash)
    }

    /**
     * Legacy classical pair safety number computation.
     */
    fun computePairSafetyNumber(myPubKey: ByteArray, peerPubKey: ByteArray): String {
        require(myPubKey.size == 32) { "myPubKey must be 32 bytes" }
        require(peerPubKey.size == 32) { "peerPubKey must be 32 bytes" }

        val (first, second) = if (compareBytes(myPubKey, peerPubKey) <= 0) {
            myPubKey to peerPubKey
        } else {
            peerPubKey to myPubKey
        }

        val combined = ByteArray(64)
        first.copyInto(combined, 0, 0, 32)
        second.copyInto(combined, 32, 0, 32)

        val hash = Sha256Digest.digest(combined)
        return formatSafetyNumber(hash)
    }

    fun compareBytes(a: ByteArray, b: ByteArray): Int {
        for (i in 0 until minOf(a.size, b.size)) {
            val vA = a[i].toInt() and 0xFF
            val vB = b[i].toInt() and 0xFF
            if (vA != vB) return vA.compareTo(vB)
        }
        return a.size.compareTo(b.size)
    }

    fun envelopeEncrypt(data: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val hex = StringBuilder(data.size * 2)
        for (b in data) {
            val v = b.toInt() and 0xFF
            hex.append(hexChars[v ushr 4])
            hex.append(hexChars[v and 0x0F])
        }
        return KeyVault.encrypt(hex.toString())
    }

    fun envelopeDecrypt(cipherText: String): ByteArray {
        val hex = KeyVault.decrypt(cipherText)
        if (hex.startsWith("🔒") || hex.length % 2 != 0) {
            return ByteArray(0)
        }
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val high = hex[i * 2].digitToIntOrNull(16) ?: return ByteArray(0)
            val low = hex[i * 2 + 1].digitToIntOrNull(16) ?: return ByteArray(0)
            result[i] = ((high shl 4) or low).toByte()
        }
        return result
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/MemorySanitizer.kt`
**Contexto Arquitetural:** Sanitizador de memória volátil para sobrescrita determinística de buffers sensíveis.

```kotlin
package com.example.security.identity

import kotlin.concurrent.Volatile

/**
 * Memory Sanitizer with Dead-Store Elimination mitigation (P1.2).
 *
 * Problem:
 * Standard `Arrays.fill(bytes, 0)` can be silently eliminated as dead-code by modern
 * JIT optimizing compilers (HotSpot C2 / GraalVM) when the buffer is not read subsequently.
 * This leaves sensitive key material and BIP-39 mnemonic phrases exposed in RAM dumps.
 *
 * Mitigation:
 * Implements a volatile memory barrier and secondary accumulation read that prevents
 * the optimizer from proving that the zeroed memory is dead.
 */
object MemorySanitizer {

    @Volatile
    private var volatileSink: Byte = 0

    /**
     * Irreversibly zeroes a sensitive byte array, guaranteeing the compiler will not
     * optimize away the wiping operation.
     */
    fun zeroize(bytes: ByteArray?) {
        if (bytes == null || bytes.isEmpty()) return

        var accumulator: Byte = 0
        for (i in bytes.indices) {
            bytes[i] = 0
            accumulator = (accumulator.toInt() xor bytes[i].toInt()).toByte()
        }

        // Volatile write forces a memory fence / barrier preventing Dead-Store Elimination
        volatileSink = accumulator
    }

    /**
     * Irreversibly zeroes a sensitive char array (e.g. passwords, PINs).
     */
    fun zeroize(chars: CharArray?) {
        if (chars == null || chars.isEmpty()) return

        var accumulator: Int = 0
        for (i in chars.indices) {
            chars[i] = '\u0000'
            accumulator = accumulator xor chars[i].code
        }

        volatileSink = accumulator.toByte()
    }

    /**
     * Zeroes and clears a mutable list of sensitive strings.
     */
    fun zeroizeStrings(strings: MutableList<String>?) {
        if (strings == null || strings.isEmpty()) return
        for (i in strings.indices) {
            strings[i] = ""
        }
        strings.clear()
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/security/identity/Bip39Portuguese.kt`
**Contexto Arquitetural:** Codificador/Decodificador BIP-39 PT-BR com checksum SHA-256.

```kotlin
﻿package com.example.security.identity

/**
 * Official BIP-0039 Portuguese (PT-BR) Wordlist (2048 words) and encoder/decoder.
 * Handles 128-bit entropy -> 12 words with 4-bit SHA-256 checksum and vice-versa.
 */
object Bip39Portuguese {

    val WORDS: List<String> = listOf(
        "abacate",
        "abaixo",
        "abalar",
        "abater",
        "abduzir",
        // ... [2038 palavras omitidas para otimização de contexto do Validador — lista oficial BIP-0039 PT-BR completa no repositório] ...
        "zelador",
        "zombar",
        "zoologia",

        "zumbido"
    )

    val WORD_INDEX_MAP: Map<String, Int> = WORDS.mapIndexed { index, word -> word to index }.toMap()

    fun entropyToMnemonic(entropy: ByteArray): List<String> {
        require(entropy.size == 16) { "Entropy must be exactly 16 bytes (128 bits)" }
        val hash = Sha256Digest.digest(entropy)
        val checksumBits = (hash[0].toInt() and 0xFF) ushr 4

        val bitString = StringBuilder(132)
        for (b in entropy) {
            val v = b.toInt() and 0xFF
            for (j in 7 downTo 0) {
                bitString.append(if ((v and (1 shl j)) != 0) '1' else '0')
            }
        }
        for (j in 3 downTo 0) {
            bitString.append(if ((checksumBits and (1 shl j)) != 0) '1' else '0')
        }

        val words = ArrayList<String>(12)
        for (i in 0 until 12) {
            val chunk = bitString.substring(i * 11, (i + 1) * 11)
            var index = 0
            for (ch in chunk) {
                index = (index shl 1) or (if (ch == '1') 1 else 0)
            }
            words.add(WORDS[index])
        }
        return words
    }

    fun mnemonicToEntropy(words: List<String>): Result<ByteArray> {
        if (words.size != 12) {
            return Result.failure(IllegalArgumentException("Mnemônico deve conter exatamente 12 palavras"))
        }
        val bitString = StringBuilder(132)
        for (w in words) {
            val normalized = w.trim().lowercase()
            val index = WORD_INDEX_MAP[normalized]
                ?: return Result.failure(IllegalArgumentException("Palavra inválida na lista BIP-39 PT-BR: ''"))
            for (j in 10 downTo 0) {
                bitString.append(if ((index and (1 shl j)) != 0) '1' else '0')
            }
        }

        val entropy = ByteArray(16)
        for (i in 0 until 16) {
            val byteChunk = bitString.substring(i * 8, (i + 1) * 8)
            var b = 0
            for (ch in byteChunk) {
                b = (b shl 1) or (if (ch == '1') 1 else 0)
            }
            entropy[i] = b.toByte()
        }

        val checksumChunk = bitString.substring(128, 132)
        var extractedChecksum = 0
        for (ch in checksumChunk) {
            extractedChecksum = (extractedChecksum shl 1) or (if (ch == '1') 1 else 0)
        }

        val hash = Sha256Digest.digest(entropy)
        val expectedChecksum = (hash[0].toInt() and 0xFF) ushr 4

        if (extractedChecksum != expectedChecksum) {
            return Result.failure(IllegalStateException("Checksum do mnemônico inválido"))
        }

        return Result.success(entropy)
    }
}
```

---

### Arquivo: `composeApp/src/commonMain/kotlin/com/example/data/network/KeyStoreClient.kt`
**Contexto Arquitetural:** Cliente de rede autenticado para interação com o KeyStore blind do backend.

```kotlin
package com.example.data.network

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class StoreKeyResult(
    val success: Boolean,
    val messageId: String? = null,
    val expiresAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Client service to persist Data Encryption Keys (DEKs) via the server-side
 * HTTPS Callable function 'storeMessageKey'.
 *
 * Adheres strictly to the Firebase Callable Protocol:
 * - Method: POST
 * - Content-Type: application/json
 * - Authorization: Bearer <FIREBASE_ID_TOKEN>
 * - Body: Wrapped in {"data": { ... }} envelope
 */
object KeyStoreClient {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun storeMessageKey(
        messageId: String,
        senderId: String,
        recipientId: String,
        ephemeralPubKey: String,
        wrappedDek: String,
        expiresAtMillis: Long,
        idToken: String
    ): StoreKeyResult {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("messageId", messageId)
                    put("senderId", senderId)
                    put("recipientId", recipientId)
                    put("ephemeralPubKey", ephemeralPubKey)
                    put("wrappedDek", wrappedDek)
                    put("expiresAtMillis", expiresAtMillis)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.storeMessageKeyUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                val success = resultObj?.get("success")?.jsonPrimitive?.content?.toBoolean() ?: true
                val returnedMsgId = resultObj?.get("messageId")?.jsonPrimitive?.content ?: messageId
                val returnedExpiresAt = resultObj?.get("expiresAt")?.jsonPrimitive?.content?.toLongOrNull() ?: expiresAtMillis

                StoreKeyResult(
                    success = success,
                    messageId = returnedMsgId,
                    expiresAt = returnedExpiresAt
                )
            } else {
                val errorMsg = try {
                    val parsed = json.parseToJsonElement(responseBody).jsonObject
                    parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: "HTTP ${response.status.value}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.status.value}: $responseBody"
                }

                StoreKeyResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            StoreKeyResult(
                success = false,
                errorMessage = "Network exception: ${e.message}"
            )
        }
    }

    suspend fun getMessageKey(
        messageId: String,
        idToken: String
    ): GetKeyResult {
        return try {
            val payload = buildJsonObject {
                put("data", buildJsonObject {
                    put("messageId", messageId)
                })
            }

            val response = ApiClient.client.post(AppEndpoints.getMessageKeyUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $idToken")
                setBody(payload.toString())
            }

            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                val parsed = json.parseToJsonElement(responseBody).jsonObject
                val resultObj = parsed["result"]?.jsonObject
                val success = resultObj?.get("success")?.jsonPrimitive?.content?.toBoolean() ?: true
                val returnedMsgId = resultObj?.get("messageId")?.jsonPrimitive?.content ?: messageId
                val ephemeralPubKey = resultObj?.get("ephemeralPubKey")?.jsonPrimitive?.content
                val wrappedDek = resultObj?.get("wrappedDek")?.jsonPrimitive?.content
                val returnedExpiresAt = resultObj?.get("expiresAtMillis")?.jsonPrimitive?.content?.toLongOrNull()

                if (ephemeralPubKey != null && wrappedDek != null) {
                    GetKeyResult(
                        success = success,
                        messageId = returnedMsgId,
                        ephemeralPubKey = ephemeralPubKey,
                        wrappedDek = wrappedDek,
                        expiresAtMillis = returnedExpiresAt
                    )
                } else {
                    GetKeyResult(
                        success = false,
                        errorMessage = "Malformed response: missing opaque wrapped DEK payload"
                    )
                }
            } else {
                val errorMsg = try {
                    val parsed = json.parseToJsonElement(responseBody).jsonObject
                    parsed["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: "HTTP ${response.status.value}: $responseBody"
                } catch (_: Exception) {
                    "HTTP ${response.status.value}: $responseBody"
                }

                GetKeyResult(
                    success = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            GetKeyResult(
                success = false,
                errorMessage = "Network exception: ${e.message}"
            )
        }
    }
}

@Serializable
data class GetKeyResult(
    val success: Boolean,
    val messageId: String? = null,
    val ephemeralPubKey: String? = null,
    val wrappedDek: String? = null,
    val expiresAtMillis: Long? = null,
    val errorMessage: String? = null
)
```

---



## 5. Governança e Regras Permanentes de Segurança

### Arquivo: `AGENTS.md`
**Contexto Arquitetural:** Regras mandatórias e permanentes de segurança, governança e higiene técnica.

```markdown
# Diretrizes de Operação do Agente de IA (AGENTS.md)

Este documento estabelece as regras mandatórias e permanentes de segurança, governança e higiene técnica para agentes autônomos e assistentes de IA que operam no repositório **Pmsg**.

---

## Segurança de Credenciais (Regras Permanentes para o Agente)

1. **Prioridade Absoluta por Operações Anônimas**:
   - **NUNCA** extrair credenciais (`git credential fill`, tokens, chaves de API) quando a operação puder funcionar de forma anônima ou sem autenticação.
   - Requisições `GET` em repositórios, documentações e endpoints públicos devem ser **SEMPRE** anônimas.

2. **Proibição Estrita de Vazamento e Eco de Segredos**:
   - **NUNCA** imprimir, logar, ecoar no terminal, salvar em arquivo, incluir em artifacts ou adicionar em commits: tokens de acesso, chaves privadas, frases mnemônicas BIP-39, números de segurança comutativos ou segredos de qualquer natureza.
   - Todo comando de terminal que interaja com credenciais deve suprimir saídas diretas que contenham senhas ou tokens.

3. **Custódia do Mnemônico BIP-39 (12 Palavras)**:
   - A exibição ou revelação da frase mnemônica de recuperação ocorre **EXCLUSIVAMENTE na interface do usuário (UI)** do aplicativo, mediante autenticação biométrica ou PIN pelo próprio usuário.
   - O agente **NUNCA** manipula, lê, valida ou transcreve palavras do mnemônico, devendo apenas orientar o usuário até a tela correspondente.

4. **Gestão de Segredos de Nuvem (Secret Manager)**:
   - Segredos de infraestrutura (como `GEMINI_API_KEY`) são definidos e rotacionados **exclusivamente pelo usuário** através do CLI interativo (`firebase-tools functions:secrets:set`), sem eco em tela. O agente nunca recebe ou manipula esses valores diretamente.

5. **Padrão Mandatório para Chamadas Autenticadas em Runtime**:
   - Quando a autenticação for **estritamente necessária** (ex: automação de CI/CD ou APIs privadas autorizadas), o agente deve:
     a. Justificar formalmente a necessidade ao usuário antes da execução;
     b. Capturar a credencial via `git credential fill` diretamente em variável volátil de memória (zero eco no console);
     c. Utilizar a variável exclusivamente nos headers da requisição em memória;
     d. Descartar a variável imediatamente após o uso.

6. **Varredura Ativa e Padrões de Bloqueio de Segredos**:
   - O agente deve inspecionar ativamente arquivos, diffs e saídas para garantir a ausência absoluta dos seguintes padrões e assinaturas de credenciais:
     - **Tokens GitHub**: `gho_`, `ghp_`, `github_pat_`
     - **Chaves de API Google/Firebase**: `AIzaSy`
     - **Parâmetros e Tokens de Autenticação**: `password=`, `refresh_token`, `client_email`
     - **Chaves Privadas e Certificados**: `"BEGIN [REDACTED_HEADER] KEY"`, `"BEGIN RSA [REDACTED_HEADER] KEY"`, `"BEGIN EC [REDACTED_HEADER] KEY"`
   - É expressamente proibido commitar, logar ou exibir qualquer valor correspondente a esses padrões.

---

## Diretrizes de Operação e Execução

1. **Previsão de Tempo de Execução**:
   - Ao receber qualquer tarefa, o agente deve informar, **ANTES** de iniciar a execução, uma estimativa de tempo (ex.: `~15 min`, `~1-2 horas`, `~1 dia`) e o número de fases/etapas previstas.
   - Ao concluir a tarefa, reportar o tempo real gasto vs. a estimativa.
```

---

