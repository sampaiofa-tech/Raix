import { onDocumentCreated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

/**
 * Cloud Function to send FCM push notifications when a new message is created.
 * Provides Zero-Knowledge background delivery: only alerts the user that an ephemeral
 * message is waiting, without sending plaintext content.
 */
export const onMessageCreated = onDocumentCreated("messages/{messageId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    return;
  }
  
  const data = snapshot.data();
  const recipientUid = data.recipientUid;
  
  if (!recipientUid) {
    logger.warn(`onMessageCreated: No recipientUid found for message ${event.params.messageId}`);
    return;
  }

  const db = admin.firestore();
  
  try {
    const tokenDoc = await db.collection("devicePushTokens").doc(recipientUid).get();
    
    if (!tokenDoc.exists) {
      logger.info(`onMessageCreated: No push token registered for recipient ${recipientUid}`);
      return;
    }
    
    const tokenData = tokenDoc.data();
    const token = tokenData?.token;
    
    if (token) {
      // Send a high-priority push notification.
      // The payload contains only the messageId and type, never the encrypted/plaintext content.
      await admin.messaging().send({
        token: token,
        data: {
          type: "new_message",
          messageId: event.params.messageId,
          senderId: data.senderId || "unknown"
        },
        android: {
          priority: "high"
        }
      });
      logger.info(`onMessageCreated: Push notification sent to ${recipientUid} for message ${event.params.messageId}`);
    }
  } catch (error) {
    logger.error(`onMessageCreated: Error sending push notification to ${recipientUid}`, error);
  }
});
