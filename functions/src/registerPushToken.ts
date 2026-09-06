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
