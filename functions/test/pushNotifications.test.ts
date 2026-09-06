import { registerPushToken } from "../src/registerPushToken";
import { storeMessageKey } from "../src/storeMessageKey";

const mockSet = jest.fn().mockResolvedValue({});
const mockGet = jest.fn();
const mockDoc = jest.fn((id: string) => ({
  set: mockSet,
  get: mockGet,
}));
const mockCollection = jest.fn((colName: string) => ({
  doc: mockDoc,
  add: jest.fn().mockResolvedValue({ id: "mock_add_id" }),
}));

const mockSend = jest.fn().mockResolvedValue("mock_message_id_123");

jest.mock("firebase-admin", () => {
  const actual = jest.requireActual("firebase-admin");
  return {
    ...actual,
    initializeApp: jest.fn(),
    firestore: Object.assign(
      jest.fn(() => ({
        collection: mockCollection,
      })),
      {
        FieldValue: {
          serverTimestamp: jest.fn(() => "MOCK_SERVER_TIMESTAMP"),
        },
        Timestamp: {
          fromMillis: jest.fn((ms) => ({ toMillis: () => ms })),
        },
      }
    ),
    messaging: jest.fn(() => ({
      send: mockSend,
    })),
  };
});

describe("Push Notifications (v1.6) - Unit Tests", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("registerPushToken", () => {
    it("should reject unauthenticated calls", async () => {
      const request = {
        auth: null,
        data: { token: "fcm_test_token_123" },
      };

      await expect((registerPushToken as any).run(request)).rejects.toThrow(
        "Autenticação obrigatória"
      );
    });

    it("should reject empty or invalid token", async () => {
      const request = {
        auth: { uid: "user_alice" },
        data: { token: "   " },
      };

      await expect((registerPushToken as any).run(request)).rejects.toThrow(
        "Token de notificação inválido ou ausente."
      );
    });

    it("should store valid token in devicePushTokens collection", async () => {
      const request = {
        auth: { uid: "user_alice" },
        data: { token: "fcm_alice_device_token_abc", platform: "android" },
      };

      const result = await (registerPushToken as any).run(request);

      expect(result).toEqual({ success: true, platform: "android" });
      expect(mockCollection).toHaveBeenCalledWith("devicePushTokens");
      expect(mockDoc).toHaveBeenCalledWith("user_alice");
      expect(mockSet).toHaveBeenCalledWith(
        expect.objectContaining({
          token: "fcm_alice_device_token_abc",
          platform: "android",
          authUid: "user_alice",
        }),
        { merge: true }
      );
    });
  });

  describe("storeMessageKey with Zero-Knowledge Push Notification", () => {
    it("should dispatch push notification if recipient has registered push token", async () => {
      // Mock recipient has a push token in Firestore
      mockGet.mockResolvedValueOnce({
        exists: true,
        data: () => ({
          token: "fcm_bob_device_token_xyz",
          platform: "android",
        }),
      });

      const request = {
        auth: { uid: "user_alice" },
        data: {
          messageId: "msg_push_01",
          senderId: "user_alice",
          recipientId: "user_bob",
          ephemeralPubKey: "ephem_pub_123",
          wrappedDek: "wrapped_dek_b64",
          expiresAtMillis: Date.now() + 3600000,
        },
      };

      const result = await (storeMessageKey as any).run(request);

      expect(result.success).toBe(true);
      expect(mockSend).toHaveBeenCalledWith(
        expect.objectContaining({
          token: "fcm_bob_device_token_xyz",
          notification: {
            title: "Raix",
            body: "Nova mensagem efêmera recebida.",
          },
          data: expect.objectContaining({
            type: "new_message",
            messageId: "msg_push_01",
          }),
        })
      );
    });

    it("should succeed gracefully if recipient has no registered push token", async () => {
      mockGet.mockResolvedValueOnce({
        exists: false,
      });

      const request = {
        auth: { uid: "user_alice" },
        data: {
          messageId: "msg_push_02",
          senderId: "user_alice",
          recipientId: "user_charlie_offline",
          ephemeralPubKey: "ephem_pub_456",
          wrappedDek: "wrapped_dek_b64",
          expiresAtMillis: Date.now() + 3600000,
        },
      };

      const result = await (storeMessageKey as any).run(request);

      expect(result.success).toBe(true);
      expect(mockSend).not.toHaveBeenCalled();
    });

    it("should succeed even if push delivery throws an error (fail-safe non-blocking)", async () => {
      mockGet.mockResolvedValueOnce({
        exists: true,
        data: () => ({ token: "expired_or_invalid_token" }),
      });
      mockSend.mockRejectedValueOnce(new Error("FCM service unavailable"));

      const request = {
        auth: { uid: "user_alice" },
        data: {
          messageId: "msg_push_03",
          senderId: "user_alice",
          recipientId: "user_bob",
          ephemeralPubKey: "ephem_pub_789",
          wrappedDek: "wrapped_dek_b64",
          expiresAtMillis: Date.now() + 3600000,
        },
      };

      const result = await (storeMessageKey as any).run(request);

      expect(result.success).toBe(true);
    });
  });
});
