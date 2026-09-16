# Raix Codebase Consolidada — Módulo 1: Backend & Regras de Banco de Dados
**Destinado ao Validador:** Qwen 2.5 Coder 32B  
**Escopo:** Cloud Functions (TypeScript / Node.js 20) e Regras de Segurança Firestore  
**Status de Segurança:** Zero-Knowledge DEK, Autenticação Anônima, Retenção Legal Marco Civil (Art. 15), Shredder Duplo Ativo.

---

## 1. Visão Geral Arquitetural do Backend

O backend do Raix opera estritamente como um **coordenador cego (*blind coordinator*)** e **custodiante regulatório mínimo**:
1. **Zero Acesso ao Conteúdo ou Chaves em Claro**: O servidor nunca recebe, armazena ou manipula chaves privadas ou a Chave de Encriptação de Dados (DEK) em texto claro. Todas as chaves enviadas via `storeMessageKey` são previamente encapsuladas via envelopes selados (*SealedBox*) com a chave pública do destinatário.
2. **Controle de Acesso em Nível de Linha (RBAC/ABAC)**: Regras do Firestore (`firestore.rules`) bloqueiam qualquer escrita direta na coleção `messageKeys` a partir dos clientes. Toda persistência passa pelas Cloud Functions autenticadas via Firebase Auth.
3. **Retenção Obrigatória pelo Marco Civil da Internet (Lei 12.965/2014, Art. 15)**: O módulo `connectionLogs.ts` registra o acesso pelo prazo legal estrito de 180 dias. Para preservar a privacidade forte, o endereço IP de origem é pseudonimizado via HMAC-SHA256 com salt mensal rotativo, sem coleta de portas de cliente ou fingerprints.
4. **Destruição Ativa de Dados (Shredder)**: Implementado em duas frentes em `shredder.ts`:
   - `scheduledMessageShredder`: Executado a cada 1 minuto via Cloud Scheduler, expurgando mensagens e chaves cujo `expiresAt` expirou.
   - `onDeleteMessage`: Trigger Firestore ativado imediatamente no evento de exclusão de mensagem (ex.: pós-leitura *vanish-after-read*), destruindo atômica e irreversivelmente a DEK associada na coleção `messageKeys`.
5. **Proteção Anti-Downgrade & Anti-Abuso**: A função `updateIdentityRouting` valida assinaturas criptográficas da prova de posse antes de atualizar o UID associado a um fingerprint, impedindo sequestro de rota. As funções `reportAbuse` e `reportAbuseWithContent` aplicam rate-limiting estrito e mitigação de DoS.

---

## 2. Código-Fonte Completo do Backend

### Arquivo: `functions/src/index.ts`
**Contexto Arquitetural:** Ponto de entrada do runtime Cloud Functions v2. Exporta todos os gatilhos e HTTPS Callables.

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
**Contexto Arquitetural:** Cloud Function storeMessageKey — Armazena o envelope da DEK selada. Valida autenticação, integridade do payload e autoria do remetente (caller UID == senderId).

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
**Contexto Arquitetural:** Cloud Function getMessageKey — Recupera a DEK encapsulada. Garante que apenas o destinatário legítimo (caller UID == recipientId) possa obter o envelope da chave.

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
**Contexto Arquitetural:** Cloud Function resolveFingerprint — Resolução pública de identidade criptográfica: mapeia o fingerprint público para o authUid e chaves públicas ativas.

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
**Contexto Arquitetural:** Cloud Function updateIdentityRouting — Atualiza o roteamento de mensagens do usuário. Exige prova de posse criptográfica (assinatura digital) e validação anti-downgrade.

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

### Arquivo: `functions/src/createInvite.ts`
**Contexto Arquitetural:** Cloud Function createInvite — Gera convite temporário de pareamento seguro com expiração estrita (TTL).

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes
const MAX_INVITES_PER_WINDOW = 10; // Max 10 invites per 10 min window
const INVITE_TTL_MS = 24 * 60 * 60 * 1000; // 24 hours

/**
 * HTTPS Callable Cloud Function to generate an ephemeral remote invite (Modelo C).
 * 
 * SECURITY & PRIVACY SPECIFICATIONS:
 * 1. Requires authenticated caller (Anonymous or Firebase Auth).
 * 2. Sliding window rate limiting using Firestore collection 'userRateLimits'.
 * 3. Ephemeral single-use cryptographic token (32 bytes = 256 bits of entropy).
 * 4. 24-hour strict TTL.
 * 5. Document stored in collection 'invites' (client read/write disabled via firestore.rules).
 */
export const createInvite = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "createInvite");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("createInvite: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para criar convite."
    );
  }

  const callerUid = request.auth.uid;
  const data = request.data as { creatorFingerprint?: string; creatorPubKey?: string; creatorSigningPubKey?: string };

  // 2. Validate payload
  if (!data || !data.creatorFingerprint || !data.creatorPubKey) {
    throw new HttpsError(
      "invalid-argument",
      "Campos 'creatorFingerprint' e 'creatorPubKey' são obrigatórios."
    );
  }

  const fingerprint = data.creatorFingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  const pubKey = data.creatorPubKey.trim();
  if (pubKey.length === 0) {
    throw new HttpsError("invalid-argument", "Chave pública não pode ser vazia.");
  }

  // 3. Cryptographic Binding Check: SHA-256(pubKey) == fingerprint
  try {
    const pubKeyBytes = Buffer.from(pubKey, "base64");
    if (pubKeyBytes.length !== 32) {
      throw new Error("Invalid public key length");
    }
    const computedFp = crypto.createHash("sha256").update(new Uint8Array(pubKeyBytes)).digest("hex");
    if (computedFp.toLowerCase() !== fingerprint) {
      throw new HttpsError(
        "invalid-argument",
        "Fingerprint não corresponde ao hash SHA-256 da chave pública fornecida."
      );
    }
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    throw new HttpsError("invalid-argument", "Formato inválido de chave pública Base64.");
  }

  const db = admin.firestore();

  // 4. Rate Limiting via Firestore userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - limitData.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (limitData.count >= MAX_INVITES_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de criação de convites excedido (máximo 10 a cada 10 minutos)."
        );
      } else {
        t.update(rateLimitRef, { count: limitData.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in createInvite:", err);
  }

  // 5. Generate secure random token (256 bits)
  const token = crypto.randomBytes(32).toString("hex");
  const expiresAtMillis = now + INVITE_TTL_MS;

  const inviteRef = db.collection("invites").doc(token);
  const inviteData: Record<string, any> = {
    creatorUid: callerUid,
    creatorFingerprint: fingerprint,
    creatorPubKey: pubKey,
    createdAt: FieldValue.serverTimestamp(),
    expiresAtMillis,
    used: false,
    acceptedByUid: null,
  };
  if (data.creatorSigningPubKey && typeof data.creatorSigningPubKey === "string") {
    inviteData.creatorSigningPubKey = data.creatorSigningPubKey.trim();
    // Register identity directory doc if not already existing
    const identityRef = db.collection("identities").doc(fingerprint);
    await identityRef.set({
      currentAuthUid: callerUid,
      pubKey,
      signingPubKey: data.creatorSigningPubKey.trim(),
      updatedAt: FieldValue.serverTimestamp(),
    }, { merge: true });
  }

  await inviteRef.set(inviteData);

  logger.info(`createInvite: Invite created by ${callerUid} with 24h TTL.`);

  return {
    inviteToken: token,
    inviteLink: `pmsg://invite?i=${token}&fp=${fingerprint}`,
    expiresAtMillis,
  };
});
```

---

### Arquivo: `functions/src/acceptInvite.ts`
**Contexto Arquitetural:** Cloud Function acceptInvite — Processa a aceitação do convite e vincula contatos com verificação mútua.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute
const MAX_ACCEPTS_PER_WINDOW = 15; // Max 15 attempts / min

/**
 * HTTPS Callable Cloud Function to accept an ephemeral remote invite (Modelo C).
 * 
 * SECURITY & PRIVACY SPECIFICATIONS:
 * 1. Requires authenticated caller.
 * 2. Strict single-use: rejected if already used or expired.
 * 3. Self-accept prevention: creator cannot accept their own invite.
 * 4. Vanish-After-Accept: document is permanently deleted from Firestore inside the transaction.
 * 5. Returns only the technical routing metadata (creator's fingerprint and public key).
 */
export const acceptInvite = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "acceptInvite");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("acceptInvite: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para aceitar convite."
    );
  }

  const callerUid = request.auth.uid;
  const data = (request.data || {}) as { inviteToken?: string; token?: string; i?: string };
  const rawToken = data.inviteToken || data.token || data.i;

  // 2. Validate payload
  if (!rawToken || typeof rawToken !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'inviteToken' (ou 'i') é obrigatório e deve ser uma string."
    );
  }

  const inviteToken = rawToken.trim().toLowerCase();
  if (inviteToken.length !== 64 || !/^[0-9a-f]{64}$/.test(inviteToken)) {
    throw new HttpsError(
      "invalid-argument",
      "Token de convite inválido (deve conter 64 caracteres hexadecimais)."
    );
  }

  const db = admin.firestore();

  // 3. Rate Limiting via Firestore userRateLimits
  const rateLimitRef = db.collection("userRateLimits").doc(callerUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || { count: 0, windowStart: now };
      if (now - limitData.windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now });
      } else if (limitData.count >= MAX_ACCEPTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de tentativas de aceite excedido. Aguarde um momento."
        );
      } else {
        t.update(rateLimitRef, { count: limitData.count + 1 });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in acceptInvite:", err);
  }

  // 4. Atomic Transaction: Validate & Vanish-After-Accept
  const inviteRef = db.collection("invites").doc(inviteToken);
  let creatorFingerprint = "";
  let creatorPubKey = "";

  await db.runTransaction(async (t) => {
    const inviteDoc = await t.get(inviteRef);
    if (!inviteDoc.exists) {
      throw new HttpsError(
        "not-found",
        "Convite não encontrado ou já incinerado."
      );
    }

    const inviteData = inviteDoc.data()!;

    if (inviteData.used === true) {
      throw new HttpsError(
        "failed-precondition",
        "Convite já utilizado. O protocolo de uso único impede novo aceite."
      );
    }

    if (now > inviteData.expiresAtMillis) {
      throw new HttpsError(
        "failed-precondition",
        "Convite expirado (TTL de 24 horas excedido)."
      );
    }

    if (inviteData.creatorUid === callerUid) {
      throw new HttpsError(
        "invalid-argument",
        "Não é permitido aceitar o próprio convite."
      );
    }

    creatorFingerprint = inviteData.creatorFingerprint;
    creatorPubKey = inviteData.creatorPubKey;

    // Vanish-After-Accept: Delete document immediately
    t.delete(inviteRef);
  });

  logger.info(`acceptInvite: Token ${inviteToken.substring(0, 8)}... accepted by ${callerUid} and permanently incinerated.`);

  return {
    creatorFingerprint,
    creatorPubKey,
  };
});
```

---

### Arquivo: `functions/src/reportAbuse.ts`
**Contexto Arquitetural:** Cloud Function reportAbuse — Denúncia anônima com rate-limiting, sem inspeção de conteúdo.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes window
const MAX_REPORTS_PER_WINDOW = 5; // Maximum 5 reports per 10 minutes per user
const ABUSE_FLAG_THRESHOLD = 3; // 3 distinct reporters flag fingerprint for abuse review

export const VALID_ABUSE_TYPES = ["SPAM", "HARASSMENT", "ILLEGAL_CONTENT", "OTHER"] as const;
export type AbuseType = (typeof VALID_ABUSE_TYPES)[number];

/**
 * HTTPS Callable Cloud Function: reportAbuse (v1.3)
 *
 * Receives behavioral abuse reports without ANY message content (Zero-Knowledge).
 *
 * SPECIFICATIONS:
 * 1. Requires authenticated caller (Anonymous or Firebase Auth).
 * 2. Sliding window rate limiting using Firestore collection 'userRateLimits'.
 * 3. Strictly behavioral: REJECTS any payload containing message text, contents, or bodies.
 * 4. Stores report in 'abuseReports' collection (client read/write disabled).
 * 5. Updates server-side metrics in 'abuseMetrics/{fingerprint}' tracking distinct reporters
 *    and setting an abuse flag when multiple independent reports occur.
 */
export const reportAbuse = onCall(async (request) => {
  // 0. Marco Civil da Internet Art. 15 Connection Log
  await recordConnectionLog(request, "reportAbuse");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("reportAbuse: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para reportar abuso."
    );
  }

  const reporterUid = request.auth.uid;
  const data = request.data as Record<string, any>;

  if (!data || typeof data !== "object") {
    throw new HttpsError("invalid-argument", "Payload da requisição inválido.");
  }

  // 2. Strict ALLOW-LIST enforcement: exclusively accept reportedFingerprint, abuseType, and inviteId
  const ALLOWED_FIELDS = new Set(["reportedFingerprint", "abuseType", "inviteId"]);
  const payloadKeys = Object.keys(data);
  for (const key of payloadKeys) {
    if (!ALLOWED_FIELDS.has(key)) {
      logger.warn(`reportAbuse: Rejected unauthorized field '${key}' in payload.`);
      throw new HttpsError(
        "invalid-argument",
        `Campo não permitido: '${key}'. Denúncias aceitam exclusivamente 'reportedFingerprint', 'abuseType' e 'inviteId'.`
      );
    }
  }

  // 3. Validate required fields
  const { reportedFingerprint, abuseType, inviteId } = data;

  if (!reportedFingerprint || typeof reportedFingerprint !== "string") {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'reportedFingerprint' é obrigatório."
    );
  }

  const fingerprint = reportedFingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  if (!abuseType || typeof abuseType !== "string") {
    throw new HttpsError("invalid-argument", "Campo 'abuseType' é obrigatório.");
  }

  const normalizedAbuseType = abuseType.trim().toUpperCase() as AbuseType;
  if (!VALID_ABUSE_TYPES.includes(normalizedAbuseType)) {
    throw new HttpsError(
      "invalid-argument",
      `Tipo de abuso inválido. Valores aceitos: ${VALID_ABUSE_TYPES.join(", ")}.`
    );
  }

  let cleanInviteId: string | null = null;
  if (inviteId !== undefined && inviteId !== null) {
    if (typeof inviteId !== "string" || inviteId.trim().length === 0 || inviteId.length > 128) {
      throw new HttpsError(
        "invalid-argument",
        "Campo 'inviteId' deve ser uma string de até 128 caracteres."
      );
    }
    cleanInviteId = inviteId.trim();
  }

  const db = admin.firestore();

  // 4. Rate Limiting via Firestore userRateLimits (max 5 reports per 10 minutes)
  const rateLimitRef = db.collection("userRateLimits").doc(reporterUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || {
        count: 0,
        windowStart: now,
      };
      const windowStart = typeof limitData.windowStart === "number" ? limitData.windowStart : now;
      const currentCount = typeof limitData.count === "number" ? limitData.count : 0;

      if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now, lastReportAt: now }, { merge: true });
      } else if (currentCount >= MAX_REPORTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de denúncias excedido (máximo 5 a cada 10 minutos)."
        );
      } else {
        t.set(rateLimitRef, { count: currentCount + 1, lastReportAt: now }, { merge: true });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in reportAbuse:", err);
    throw new HttpsError("internal", "Falha ao processar limite de taxa.");
  }

  // 5. Atomic recording in abuseReports + server-side behavioral metrics in abuseMetrics
  const reportRef = db.collection("abuseReports").doc();
  const metricRef = db.collection("abuseMetrics").doc(fingerprint);

  await db.runTransaction(async (t) => {
    const metricDoc = await t.get(metricRef);
    const metricData = (metricDoc && typeof metricDoc.data === "function" && metricDoc.data()) || {};
    const existingReporters: string[] = Array.isArray(metricData.reporters)
      ? metricData.reporters
      : [];

    let updatedReporters = existingReporters;
    if (!existingReporters.includes(reporterUid)) {
      updatedReporters = [...existingReporters, reporterUid];
    }

    const reportCount = updatedReporters.length;
    const isAbuseFlagged = reportCount >= ABUSE_FLAG_THRESHOLD;

    t.set(reportRef, {
      reportId: reportRef.id,
      reportedFingerprint: fingerprint,
      reporterUid,
      abuseType: normalizedAbuseType,
      inviteId: cleanInviteId,
      createdAt: FieldValue.serverTimestamp(),
    });

    t.set(
      metricRef,
      {
        reportedFingerprint: fingerprint,
        reportCount,
        reporters: updatedReporters,
        abuseFlag: isAbuseFlagged,
        lastReportedAt: FieldValue.serverTimestamp(),
        flaggedAt: isAbuseFlagged ? (metricData.flaggedAt || FieldValue.serverTimestamp()) : null,
      },
      { merge: true }
    );
  });

  logger.info(
    `reportAbuse: Successfully registered abuse report for ${fingerprint.substring(0, 8)}... (type: ${normalizedAbuseType}) by ${reporterUid}`
  );

  return {
    success: true,
    reportId: reportRef.id,
    timestamp: Date.now(),
  };
});
```

---

### Arquivo: `functions/src/reportAbuseWithContent.ts`
**Contexto Arquitetural:** Cloud Function reportAbuseWithContent — Denúncia assistida contendo payload assinado pelo denunciante para moderação justa.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

const RATE_LIMIT_WINDOW_MS = 10 * 60 * 1000; // 10 minutes window
const MAX_REPORTS_PER_WINDOW = 5; // Maximum 5 reports per 10 minutes per user
const ABUSE_FLAG_THRESHOLD = 3; // 3 distinct reporters flag fingerprint for abuse review

export const VALID_ABUSE_TYPES = ["SPAM", "HARASSMENT", "ILLEGAL_CONTENT", "OTHER"] as const;
export type AbuseType = (typeof VALID_ABUSE_TYPES)[number];

/**
 * HTTPS Callable Cloud Function: reportAbuseWithContent (v1.4 - Parecer Jurídico C6)
 *
 * Permite ao usuário denunciante encaminhar voluntariamente um trecho de texto da mensagem ofensiva/ilícita,
 * MEDIANTE CONSENTIMENTO EXPLÍCITO E INEQUÍVOCO (LGPD Art. 7º, I / Art. 11, I).
 *
 * NOTA DE SEGURANÇA E ARQUITETURA:
 * - O mecanismo principal continua sendo o reportAbuse behavioral Zero-Knowledge.
 * - Este endpoint é estritamente opcional para casos graves onde o usuário solicita intervenção humana.
 * - Sanção de moderação: revogação Ed25519 no roteamento — NUNCA bloqueio de IP.
 */
export const reportAbuseWithContent = onCall(async (request) => {
  // 1. Registro de conexão (Marco Civil da Internet Art. 15)
  await recordConnectionLog(request, "reportAbuseWithContent");

  // 2. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("reportAbuseWithContent: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para reportar abuso com conteúdo."
    );
  }

  const reporterUid = request.auth.uid;
  const data = request.data as Record<string, any>;

  if (!data || typeof data !== "object") {
    throw new HttpsError("invalid-argument", "Payload da requisição inválido.");
  }

  // 3. Strict ALLOW-LIST enforcement
  const ALLOWED_FIELDS = new Set([
    "reportedFingerprint",
    "abuseType",
    "contentSnippet",
    "explicitConsent",
    "inviteId",
  ]);
  const payloadKeys = Object.keys(data);
  for (const key of payloadKeys) {
    if (!ALLOWED_FIELDS.has(key)) {
      logger.warn(`reportAbuseWithContent: Rejected unauthorized field '${key}' in payload.`);
      throw new HttpsError(
        "invalid-argument",
        `Campo não permitido: '${key}'. Denúncias com conteúdo aceitam exclusivamente 'reportedFingerprint', 'abuseType', 'contentSnippet', 'explicitConsent' e 'inviteId'.`
      );
    }
  }

  // 4. Validate required fields
  const { reportedFingerprint, abuseType, contentSnippet, explicitConsent, inviteId } = data;

  if (!reportedFingerprint || typeof reportedFingerprint !== "string") {
    throw new HttpsError("invalid-argument", "Campo 'reportedFingerprint' é obrigatório.");
  }

  const fingerprint = reportedFingerprint.trim().toLowerCase();
  if (fingerprint.length !== 64 || !/^[0-9a-f]{64}$/.test(fingerprint)) {
    throw new HttpsError(
      "invalid-argument",
      "Fingerprint deve conter exatamente 64 caracteres hexadecimais (256 bits)."
    );
  }

  if (!abuseType || typeof abuseType !== "string") {
    throw new HttpsError("invalid-argument", "Campo 'abuseType' é obrigatório.");
  }

  const normalizedAbuseType = abuseType.trim().toUpperCase() as AbuseType;
  if (!VALID_ABUSE_TYPES.includes(normalizedAbuseType)) {
    throw new HttpsError(
      "invalid-argument",
      `Tipo de abuso inválido. Valores aceitos: ${VALID_ABUSE_TYPES.join(", ")}.`
    );
  }

  // Validação de consentimento explícito (Mandatório C6 / LGPD)
  if (explicitConsent !== true) {
    logger.warn("reportAbuseWithContent: Rejected without explicit consent.");
    throw new HttpsError(
      "invalid-argument",
      "O consentimento explícito do usuário é mandatório para o encaminhamento voluntário de conteúdo para moderação."
    );
  }

  // Validação do snippet de conteúdo (máximo 1.000 caracteres para evitar DoS)
  if (!contentSnippet || typeof contentSnippet !== "string" || contentSnippet.trim().length === 0) {
    throw new HttpsError("invalid-argument", "Campo 'contentSnippet' é obrigatório.");
  }
  if (contentSnippet.length > 1000) {
    throw new HttpsError(
      "invalid-argument",
      "Campo 'contentSnippet' excede o limite máximo permitido de 1000 caracteres."
    );
  }
  const cleanSnippet = contentSnippet.trim();

  let cleanInviteId: string | null = null;
  if (inviteId !== undefined && inviteId !== null) {
    if (typeof inviteId !== "string" || inviteId.trim().length === 0 || inviteId.length > 128) {
      throw new HttpsError(
        "invalid-argument",
        "Campo 'inviteId' deve ser uma string de até 128 caracteres."
      );
    }
    cleanInviteId = inviteId.trim();
  }

  const db = admin.firestore();

  // 5. Rate Limiting via Firestore userRateLimits (max 5 reports per 10 minutes)
  const rateLimitRef = db.collection("userRateLimits").doc(reporterUid);
  const now = Date.now();

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(rateLimitRef);
      const limitData = (doc && typeof doc.data === "function" && doc.data()) || {
        count: 0,
        windowStart: now,
      };
      const windowStart = typeof limitData.windowStart === "number" ? limitData.windowStart : now;
      const currentCount = typeof limitData.count === "number" ? limitData.count : 0;

      if (now - windowStart > RATE_LIMIT_WINDOW_MS) {
        t.set(rateLimitRef, { count: 1, windowStart: now, lastReportAt: now }, { merge: true });
      } else if (currentCount >= MAX_REPORTS_PER_WINDOW) {
        throw new HttpsError(
          "resource-exhausted",
          "Limite de denúncias excedido (máximo 5 a cada 10 minutos)."
        );
      } else {
        t.set(rateLimitRef, { count: currentCount + 1, lastReportAt: now }, { merge: true });
      }
    });
  } catch (err: any) {
    if (err instanceof HttpsError) throw err;
    logger.error("Rate limit check failed in reportAbuseWithContent:", err);
    throw new HttpsError("internal", "Falha ao processar limite de taxa.");
  }

  // 6. Gravação atômica em abuseReports + métricas comportamentais em abuseMetrics
  const reportRef = db.collection("abuseReports").doc();
  const metricRef = db.collection("abuseMetrics").doc(fingerprint);

  await db.runTransaction(async (t) => {
    const metricDoc = await t.get(metricRef);
    const metricData = (metricDoc && typeof metricDoc.data === "function" && metricDoc.data()) || {};
    const existingReporters: string[] = Array.isArray(metricData.reporters)
      ? metricData.reporters
      : [];

    let updatedReporters = existingReporters;
    if (!existingReporters.includes(reporterUid)) {
      updatedReporters = [...existingReporters, reporterUid];
    }

    const reportCount = updatedReporters.length;
    const isAbuseFlagged = reportCount >= ABUSE_FLAG_THRESHOLD;

    t.set(reportRef, {
      reportId: reportRef.id,
      reportedFingerprint: fingerprint,
      reporterUid,
      abuseType: normalizedAbuseType,
      hasContent: true,
      contentSnippet: cleanSnippet,
      explicitConsent: true,
      inviteId: cleanInviteId,
      createdAt: FieldValue.serverTimestamp(),
    });

    t.set(
      metricRef,
      {
        reportedFingerprint: fingerprint,
        reportCount,
        reporters: updatedReporters,
        abuseFlag: isAbuseFlagged,
        hasContentReports: true,
        lastReportedAt: FieldValue.serverTimestamp(),
        flaggedAt: isAbuseFlagged ? (metricData.flaggedAt || FieldValue.serverTimestamp()) : null,
      },
      { merge: true }
    );
  });

  logger.info(
    `reportAbuseWithContent: Successfully registered verified content abuse report for ${fingerprint.substring(0, 8)}... (type: ${normalizedAbuseType}) by ${reporterUid}`
  );

  return {
    success: true,
    reportId: reportRef.id,
    timestamp: Date.now(),
  };
});
```

---

### Arquivo: `functions/src/geminiProxy.ts`
**Contexto Arquitetural:** Cloud Function geminiProxy — Proxy de IA para assistência privada com remoção de headers, ausência de log de conversas e isolamento de tráfego.

```typescript
import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions";
import * as admin from "firebase-admin";
import { recordConnectionLog } from "./connectionLogs";

const geminiApiKey = defineSecret("GEMINI_API_KEY");

const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute window
const MAX_REQUESTS_PER_WINDOW = 5; // Max 5 burner notes per minute per user

/**
 * Sliding window rate limiter based on Firestore transaction.
 * Returns true if request is allowed, false if rate limit exceeded.
 */
export async function checkRateLimit(
  uid: string,
  db?: admin.firestore.Firestore
): Promise<boolean> {
  const firestoreDb = db || admin.firestore();
  const rateLimitRef = firestoreDb.collection("userRateLimits").doc(uid);
  const now = Date.now();

  try {
    return await firestoreDb.runTransaction(async (transaction) => {
      const doc = await transaction.get(rateLimitRef);
      if (!doc.exists) {
        transaction.set(rateLimitRef, {
          windowStart: now,
          count: 1,
          lastRequestAt: now,
        });
        return true;
      }

      const data = doc.data() || {};
      const windowStart = typeof data.windowStart === "number" ? data.windowStart : now;
      const currentCount = typeof data.count === "number" ? data.count : 0;

      if (now - windowStart < RATE_LIMIT_WINDOW_MS) {
        if (currentCount >= MAX_REQUESTS_PER_WINDOW) {
          return false; // Limit exceeded
        }
        transaction.update(rateLimitRef, {
          count: currentCount + 1,
          lastRequestAt: now,
        });
        return true;
      } else {
        // Window expired, reset counter for new window
        transaction.set(rateLimitRef, {
          windowStart: now,
          count: 1,
          lastRequestAt: now,
        });
        return true;
      }
    });
  } catch (err) {
    logger.error(`geminiProxy: Rate limiter transaction error for user ${uid}`, err);
    // Fail-open or fail-closed based on safety: here allow unless repeated failures
    return true;
  }
}

/**
 * HTTPS Cloud Function: geminiProxy
 *
 * Receives ephemeral AI burner note requests from Desktop and Web clients.
 * Authenticates client session token, enforces user rate limiting,
 * attaches GEMINI_API_KEY from server-side secret store, queries Google Gemini API
 * (gemini-2.0-flash), and returns the generated note without ever leaking the API key.
 */
export const geminiProxy = onRequest(
  {
    secrets: [geminiApiKey],
    cors: true,
    invoker: "public",
  },
  async (req, res) => {
    // 0. Marco Civil da Internet Art. 15 Connection Log
    await recordConnectionLog(req, "geminiProxy");

    // 1. Enforce POST method
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method Not Allowed. Use POST." });
      return;
    }

    // 2. Validate Authorization header with real verifyIdToken check
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      logger.warn("geminiProxy: Unauthorized request (missing or invalid Bearer header).");
      res.status(401).json({ error: "Unauthorized. Valid Bearer session token required." });
      return;
    }

    const sessionToken = authHeader.substring("Bearer ".length).trim();
    if (!sessionToken) {
      res.status(401).json({ error: "Unauthorized. Empty session token." });
      return;
    }

    // Cryptographic validation of Firebase Auth ID token
    let callerUid: string;
    try {
      const decodedToken = await admin.auth().verifyIdToken(sessionToken);
      callerUid = decodedToken.uid;
    } catch (authError: any) {
      logger.warn("geminiProxy: Invalid or expired Firebase ID token.", { message: authError?.message });
      res.status(401).json({ error: "Unauthorized. Invalid or expired Firebase ID token." });
      return;
    }

    // 3. Enforce sliding window Rate Limiting (max 5 requests/min per UID)
    const allowed = await checkRateLimit(callerUid);
    if (!allowed) {
      logger.warn(`geminiProxy: Rate limit exceeded for user ${callerUid}.`);
      res.status(429).json({
        error: "Too Many Requests. Rate limit exceeded (maximum 5 requests per minute).",
      });
      return;
    }

    // 3. Parse request payload
    let body = req.body;
    if (typeof body === "string") {
      try {
        body = JSON.parse(body);
      } catch {
        res.status(400).json({ error: "Invalid JSON body." });
        return;
      }
    }

    const prompt = body?.prompt;
    if (!prompt || typeof prompt !== "string") {
      res.status(400).json({ error: "Missing or invalid 'prompt' parameter." });
      return;
    }

    const model = body?.model || "gemini-3.6-flash";

    // 4. Retrieve GEMINI_API_KEY from Secret Manager or environment (Emulator fallback)
    const apiKey = process.env.GEMINI_API_KEY || geminiApiKey.value();
    if (!apiKey) {
      logger.error("geminiProxy: GEMINI_API_KEY secret is not configured.");
      res.status(500).json({ error: "Server AI configuration unavailable." });
      return;
    }

    // 5. Call Gemini API via HTTPS REST endpoint
    try {
      const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

      const geminiPayload = {
        contents: [
          {
            parts: [
              {
                text: `Gere uma nota efêmera e concisa para mensageria segura baseada no seguinte pedido: ${prompt}`,
              },
            ],
          },
        ],
        generationConfig: {
          maxOutputTokens: 256,
          temperature: 0.7,
        },
      };

      const response = await fetch(geminiUrl, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(geminiPayload),
      });

      if (!response.ok) {
        const errText = await response.text();
        logger.error(`geminiProxy: Upstream Gemini API error (${response.status}):`, errText);
        res.status(502).json({ error: "Upstream AI service error." });
        return;
      }

      const responseData = (await response.json()) as any;
      const generatedText =
        responseData?.candidates?.[0]?.content?.parts?.[0]?.text ||
        "Nota efêmera gerada com sucesso.";

      res.status(200).json({
        note: generatedText.trim(),
        model: model,
        timestamp: Date.now(),
      });
    } catch (err: any) {
      logger.error("geminiProxy: Exception calling Gemini API:", err);
      res.status(500).json({ error: "Internal proxy error executing AI task." });
    }
  }
);
```

---

### Arquivo: `functions/src/shredder.ts`
**Contexto Arquitetural:** Cloud Functions shredder — Destruição ativa de dados: scheduledMessageShredder (rotina de expurgo agendado a cada minuto) e onDeleteMessage (trigger atômico de exclusão de chave ao apagar mensagem).

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

### Arquivo: `functions/src/registerPushToken.ts`
**Contexto Arquitetural:** Cloud Function registerPushToken — Registro cego de token de notificação push associado ao hash do destinatário.

```typescript
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import { recordConnectionLog } from "./connectionLogs";

export interface RegisterPushTokenData {
  token: string;
  platform?: "android" | "ios" | "web" | "windows";
}

/**
 * HTTPS Callable Cloud Function to register a device push notification token (FCM/APNs).
 *
 * Security:
 * - Requires authentication: token is linked to request.auth.uid.
 * - Managed in isolated server-side collection devicePushTokens (inaccessible to SDK clients).
 */
export const registerPushToken = onCall(async (request) => {
  // 0. Connection Log (Marco Civil da Internet Art. 15)
  await recordConnectionLog(request, "registerPushToken");

  // 1. Enforce Authentication
  if (!request.auth || !request.auth.uid) {
    logger.warn("registerPushToken: Unauthenticated call rejected.");
    throw new HttpsError(
      "unauthenticated",
      "Autenticação obrigatória para registrar token de notificação."
    );
  }

  const callerUid = request.auth.uid;
  const data = (request.data || {}) as RegisterPushTokenData;

  // 2. Validate token
  if (!data.token || typeof data.token !== "string" || data.token.trim().length === 0) {
    logger.warn(`registerPushToken: Invalid token provided by caller ${callerUid}`);
    throw new HttpsError("invalid-argument", "Token de notificação inválido ou ausente.");
  }

  if (data.token.length > 4096) {
    throw new HttpsError("invalid-argument", "Token excede o tamanho máximo permitido.");
  }

  const platform = data.platform || "android";
  const db = admin.firestore();

  // 3. Persist in isolated devicePushTokens collection
  await db.collection("devicePushTokens").doc(callerUid).set(
    {
      token: data.token.trim(),
      platform,
      authUid: callerUid,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  logger.info(`registerPushToken: Push token registered successfully for UID ${callerUid} (${platform})`);

  return {
    success: true,
    platform,
  };
});
```

---

### Arquivo: `functions/src/connectionLogs.ts`
**Contexto Arquitetural:** Módulo connectionLogs — Retenção legal de registros de conexão (Marco Civil da Internet Art. 15) com pseudonimização HMAC-SHA256 de IP e expurgo em 180 dias.

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

### Arquivo: `functions/src/anomalyDetector.ts`
**Contexto Arquitetural:** Módulo anomalyDetector — Detecção de anomalias volumétricas e mitigação de ataques de exaustão/DoS em tempo real.

```typescript
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

export const READ_ANOMALY_THRESHOLD = 20; // 20 reads / min per identity
export const BRUTE_FORCE_DENIAL_THRESHOLD = 5; // 5 consecutive denials / min
export const ANOMALY_WINDOW_MS = 60 * 1000; // 1 minute window

export interface AnomalyEvent {
  type: "READ_SPIKE" | "BRUTE_FORCE_DENIAL";
  targetIdentityHash: string;
  callerUid: string;
  count: number;
  threshold: number;
  timestamp: Date;
  details: string;
}

/**
 * Monitors and alerts on abnormal read spikes targeting an identity (P1.3).
 * Detects potential enumeration, bulk harvesting or scraping attacks.
 */
export async function trackIdentityRead(
  db: admin.firestore.Firestore,
  identityHash: string,
  callerUid: string,
  nowMillis: number = Date.now()
): Promise<boolean> {
  if (!identityHash) return false;

  const trackerRef = db.collection("anomalyTrackers").doc(`read_${identityHash}`);
  let isAnomaly = false;

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(trackerRef);
      const data = (doc && typeof doc.data === "function" && doc.data()) || {
        count: 0,
        windowStart: nowMillis,
      };

      if (nowMillis - data.windowStart > ANOMALY_WINDOW_MS) {
        t.set(trackerRef, { count: 1, windowStart: nowMillis, lastCaller: callerUid });
      } else {
        const newCount = data.count + 1;
        t.set(trackerRef, { count: newCount, windowStart: data.windowStart, lastCaller: callerUid }, { merge: true });

        if (newCount >= READ_ANOMALY_THRESHOLD) {
          isAnomaly = true;
        }
      }
    });

    if (isAnomaly) {
      logger.warn("[SECURITY_ANOMALY_READ_SPIKE] Anomaly detected: read frequency exceeded safe threshold", {
        type: "READ_SPIKE",
        identityHash: identityHash.substring(0, 16) + "...",
        callerUid,
        threshold: READ_ANOMALY_THRESHOLD,
      });

      // Record security anomaly event
      const anomalyRef = db.collection("securityAnomalies").doc();
      await anomalyRef.set({
        type: "READ_SPIKE",
        targetIdentityHash: identityHash,
        callerUid,
        threshold: READ_ANOMALY_THRESHOLD,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        details: `Identity experienced >= ${READ_ANOMALY_THRESHOLD} reads within 60s window.`,
      });
    }
  } catch (err: any) {
    logger.error("trackIdentityRead failed non-blockingly:", err?.message || err);
  }

  return isAnomaly;
}

/**
 * Monitors and alerts on repeated permission-denied / invalid authorization attempts (P1.3).
 * Detects brute force scans against technical identityHashes.
 */
export async function trackPermissionDenied(
  db: admin.firestore.Firestore,
  identityHash: string,
  callerUid: string,
  reason: string,
  nowMillis: number = Date.now()
): Promise<boolean> {
  const target = identityHash || "global_unknown";
  const trackerRef = db.collection("anomalyTrackers").doc(`denial_${target}_${callerUid}`);
  let isBruteForce = false;

  try {
    await db.runTransaction(async (t) => {
      const doc = await t.get(trackerRef);
      const data = (doc && typeof doc.data === "function" && doc.data()) || {
        count: 0,
        windowStart: nowMillis,
      };

      if (nowMillis - data.windowStart > ANOMALY_WINDOW_MS) {
        t.set(trackerRef, { count: 1, windowStart: nowMillis, lastReason: reason });
      } else {
        const newCount = data.count + 1;
        t.set(trackerRef, { count: newCount, windowStart: data.windowStart, lastReason: reason }, { merge: true });

        if (newCount >= BRUTE_FORCE_DENIAL_THRESHOLD) {
          isBruteForce = true;
        }
      }
    });

    if (isBruteForce) {
      logger.error("[SECURITY_ANOMALY_BRUTE_FORCE_DETECTED] Repeated permission-denied events detected", {
        type: "BRUTE_FORCE_DENIAL",
        identityHash: target.substring(0, 16) + "...",
        callerUid,
        reason,
        threshold: BRUTE_FORCE_DENIAL_THRESHOLD,
      });

      const anomalyRef = db.collection("securityAnomalies").doc();
      await anomalyRef.set({
        type: "BRUTE_FORCE_DENIAL",
        targetIdentityHash: target,
        callerUid,
        reason,
        threshold: BRUTE_FORCE_DENIAL_THRESHOLD,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        details: `Caller triggered >= ${BRUTE_FORCE_DENIAL_THRESHOLD} access denials within 60s window.`,
      });
    }
  } catch (err: any) {
    logger.error("trackPermissionDenied failed non-blockingly:", err?.message || err);
  }

  return isBruteForce;
}
```

---

### Arquivo: `firestore.rules`
**Contexto Arquitetural:** Regras de Segurança do Firestore — Defesa em profundidade: bloqueio padrão, isolamento de coleções e acesso estrito por UID autenticado.

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

### Arquivo: `firestore.indexes.json`
**Contexto Arquitetural:** Índices Compostos do Firestore — Configuração de consultas compostas para mensagens pendentes e varredura eficiente do shredder.

```json
{
  "indexes": [],
  "fieldOverrides": [
    {
      "collectionGroup": "messages",
      "fieldPath": "expiresAt",
      "ttl": true,
      "indexes": [
        {
          "order": "ASCENDING",
          "queryScope": "COLLECTION"
        },
        {
          "order": "DESCENDING",
          "queryScope": "COLLECTION"
        },
        {
          "arrayConfig": "CONTAINS",
          "queryScope": "COLLECTION"
        }
      ]
    },
    {
      "collectionGroup": "connectionLogs",
      "fieldPath": "expiresAt",
      "ttl": true,
      "indexes": [
        {
          "order": "ASCENDING",
          "queryScope": "COLLECTION"
        },
        {
          "order": "DESCENDING",
          "queryScope": "COLLECTION"
        },
        {
          "arrayConfig": "CONTAINS",
          "queryScope": "COLLECTION"
        }
      ]
    },
    {
      "collectionGroup": "accessLogs",
      "fieldPath": "expiresAt",
      "ttl": true,
      "indexes": [
        {
          "order": "ASCENDING",
          "queryScope": "COLLECTION"
        },
        {
          "order": "DESCENDING",
          "queryScope": "COLLECTION"
        },
        {
          "arrayConfig": "CONTAINS",
          "queryScope": "COLLECTION"
        }
      ]
    },
    {
      "collectionGroup": "inbox",
      "fieldPath": "expiresAt",
      "indexes": [
        {
          "order": "ASCENDING",
          "queryScope": "COLLECTION_GROUP"
        },
        {
          "order": "DESCENDING",
          "queryScope": "COLLECTION_GROUP"
        }
      ]
    }
  ]
}
```

---

