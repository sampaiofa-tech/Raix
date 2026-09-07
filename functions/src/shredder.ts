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
