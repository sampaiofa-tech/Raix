import { defineSecret } from "firebase-functions/params";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import { recordConnectionLog } from "./connectionLogs";

const accessLogEncKey = defineSecret("ACCESS_LOG_ENC_KEY");

export const submitHandshake = onCall({ secrets: [accessLogEncKey] }, async (request) => {
  await recordConnectionLog(request, "submitHandshake");

  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Auth required.");
  }

  const { token, peerPubKey, encryptedPayload } = request.data;
  if (!token || !peerPubKey || !encryptedPayload) {
    throw new HttpsError("invalid-argument", "Missing fields.");
  }

  const db = admin.firestore();
  await db.collection("handshakes").doc(token).set({
    peerPubKey,
    encryptedPayload,
    createdAt: FieldValue.serverTimestamp(),
    expiresAtMillis: Date.now() + 5 * 60 * 1000 // 5 minutes
  });

  return { success: true };
});

export const pollHandshake = onCall({ secrets: [accessLogEncKey] }, async (request) => {
  await recordConnectionLog(request, "pollHandshake");

  // No auth required for polling because Desktop might not be logged in yet.
  const { token } = request.data;
  if (!token) {
    throw new HttpsError("invalid-argument", "Missing token.");
  }

  const db = admin.firestore();
  const doc = await db.collection("handshakes").doc(token).get();

  if (!doc.exists) {
    return { status: "pending" };
  }

  const data = doc.data();
  if (!data) return { status: "pending" };

  if (Date.now() > data.expiresAtMillis) {
    return { status: "expired" };
  }

  return {
    status: "completed",
    peerPubKey: data.peerPubKey,
    encryptedPayload: data.encryptedPayload
  };
});

