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
