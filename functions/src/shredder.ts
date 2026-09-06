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
  const batch = db.batch();
  let hasDeletions = false;
  let maxDelayMs = 0;
  let escalationAlertsCount = 0;

  // 1. Mensagens expiradas: Hard-delete DEK em messageKeys + Hard-delete ciphertext em messages
  const expiredKeysQuery = db
    .collection("messageKeys")
    .where("expiresAt", "<=", currentTime)
    .limit(500);

  const snapshot = await expiredKeysQuery.get();
  let messageCount = 0;

  if (!snapshot.empty) {
    for (const doc of snapshot.docs) {
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
      batch.delete(doc.ref);

      // 2. Hard-delete matching ciphertext message document (if exists)
      const messageRef = db.collection("messages").doc(messageId);
      batch.delete(messageRef);

      messageCount++;
    }
    hasDeletions = true;
  }

  // 2. Envelopes efêmeros em caixas de entrada por identidade (P0.2/P0.3): identities/{identityHash}/inbox/{envelopeId}
  let inboxCount = 0;
  const expiredInboxQuery = db
    .collectionGroup("inbox")
    .where("expiresAt", "<=", currentTime)
    .limit(500);

  const inboxSnapshot = await expiredInboxQuery.get();
  if (!inboxSnapshot.empty) {
    for (const doc of inboxSnapshot.docs) {
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

      batch.delete(doc.ref);
      inboxCount++;
    }
    hasDeletions = true;
  }

  // 3. Logs de conexão expirados (Marco Civil Art. 15 - Retenção de 180 dias):
  // Expurgo ativo das coleções connectionLogs e accessLogs
  let logsCount = 0;

  const expiredConnLogsQuery = db
    .collection("connectionLogs")
    .where("expiresAt", "<=", currentTime)
    .limit(500);

  const connLogsSnapshot = await expiredConnLogsQuery.get();
  if (!connLogsSnapshot.empty) {
    for (const doc of connLogsSnapshot.docs) {
      batch.delete(doc.ref);
      logsCount++;
    }
    hasDeletions = true;
  }

  const expiredAccessLogsQuery = db
    .collection("accessLogs")
    .where("expiresAt", "<=", currentTime)
    .limit(500);

  const accessLogsSnapshot = await expiredAccessLogsQuery.get();
  if (!accessLogsSnapshot.empty) {
    for (const doc of accessLogsSnapshot.docs) {
      batch.delete(doc.ref);
      logsCount++;
    }
    hasDeletions = true;
  }

  if (hasDeletions) {
    await batch.commit();
  }

  if (messageCount === 0 && inboxCount === 0 && logsCount === 0) {
    logger.info("Crypto-Shredder: No expired message keys, inbox envelopes or connection logs found.");
  } else {
    logger.info(
      `Crypto-Shredder: Successfully shredded ${messageCount} keys/messages, ${inboxCount} inbox envelopes and ${logsCount} connection logs. Max delay: ${maxDelayMs}ms.`
    );
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
