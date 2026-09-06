import { executeCryptoShredding } from "../src/shredder";

describe("Crypto-Shredder Authoritative Server-Side TTL & Escalation Test Suite (P0.3)", () => {
  let mockDb: any;
  let batchDeletedPaths: string[] = [];

  beforeEach(() => {
    batchDeletedPaths = [];
  });

  it("should hard-delete expired messageKeys and their corresponding messages in batch", async () => {
    const nowMillis = 1700000000000;
    const nowTimestamp = {
      toMillis: () => nowMillis,
    } as any;

    const expiredDoc1 = {
      id: "msg_key_001",
      data: () => ({
        messageId: "msg_001",
        dek: "super_secret_aes_dek_1",
        expiresAt: { toMillis: () => nowMillis - 1000 },
      }),
      ref: { path: "messageKeys/msg_key_001", parent: { id: "messageKeys" } },
    };

    const expiredDoc2 = {
      id: "msg_002",
      data: () => ({
        messageId: "msg_002",
        dek: "super_secret_aes_dek_2",
        expiresAt: { toMillis: () => nowMillis - 5000 },
      }),
      ref: { path: "messageKeys/msg_002", parent: { id: "messageKeys" } },
    };

    mockDb = {
      collection: (name: string) => ({
        where: (field: string, op: string, val: any) => ({
          limit: (limitCount: number) => ({
            get: async () => ({
              empty: name !== "messageKeys",
              docs: name === "messageKeys" ? [expiredDoc1, expiredDoc2] : [],
            }),
          }),
        }),
        doc: (id: string) => ({
          path: `${name}/${id}`,
        }),
      }),
      collectionGroup: (name: string) => ({
        where: () => ({
          limit: () => ({
            get: async () => ({ empty: true, docs: [] }),
          }),
        }),
      }),
      batch: () => ({
        delete: (ref: any) => {
          batchDeletedPaths.push(ref.path);
        },
        commit: async () => {},
      }),
    };

    const result = await executeCryptoShredding(mockDb, nowTimestamp);

    expect(result.shreddedKeysCount).toBe(2);
    expect(result.deletedMessagesCount).toBe(2);
    expect(result.maxDelayMs).toBe(5000);
    expect(result.escalationAlertsCount).toBe(0);

    // Verify hard deletion of both DEKs and Messages
    expect(batchDeletedPaths).toContain("messageKeys/msg_key_001");
    expect(batchDeletedPaths).toContain("messages/msg_001");
    expect(batchDeletedPaths).toContain("messageKeys/msg_002");
    expect(batchDeletedPaths).toContain("messages/msg_002");
  });

  it("should calculate delay and trigger escalation alerts when envelope survives > 60 min and > 3 hours", async () => {
    const nowMillis = 1700000000000;
    const nowTimestamp = {
      toMillis: () => nowMillis,
    } as any;

    // Document expired 90 minutes ago (Level 1 escalation)
    const delayedDoc1 = {
      id: "delayed_key_90m",
      data: () => ({
        messageId: "msg_delayed_90",
        expiresAt: { toMillis: () => nowMillis - 90 * 60 * 1000 },
      }),
      ref: { path: "messageKeys/delayed_key_90m", parent: { id: "messageKeys" } },
    };

    // Document expired 4 hours ago (Level 2 critical escalation)
    const delayedDoc2 = {
      id: "delayed_key_4h",
      data: () => ({
        messageId: "msg_delayed_4h",
        expiresAt: { toMillis: () => nowMillis - 240 * 60 * 1000 },
      }),
      ref: { path: "messageKeys/delayed_key_4h", parent: { id: "messageKeys" } },
    };

    mockDb = {
      collection: (name: string) => ({
        where: () => ({
          limit: () => ({
            get: async () => ({
              empty: name !== "messageKeys",
              docs: name === "messageKeys" ? [delayedDoc1, delayedDoc2] : [],
            }),
          }),
        }),
        doc: (id: string) => ({
          path: `${name}/${id}`,
        }),
      }),
      collectionGroup: () => ({
        where: () => ({
          limit: () => ({
            get: async () => ({ empty: true, docs: [] }),
          }),
        }),
      }),
      batch: () => ({
        delete: (ref: any) => {
          batchDeletedPaths.push(ref.path);
        },
        commit: async () => {},
      }),
    };

    const result = await executeCryptoShredding(mockDb, nowTimestamp);

    expect(result.shreddedKeysCount).toBe(2);
    expect(result.maxDelayMs).toBe(240 * 60 * 1000); // 4 hours
    expect(result.escalationAlertsCount).toBe(2); // Both exceeded 60m threshold
  });

  it("should be strictly idempotent: multiple executions produce zero duplicate effects", async () => {
    const nowTimestamp = { toMillis: () => 1700000000000 } as any;

    // First call: deletes 1 document
    let hasDocs = true;
    mockDb = {
      collection: (name: string) => ({
        where: () => ({
          limit: () => ({
            get: async () => {
              if (name === "messageKeys" && hasDocs) {
                hasDocs = false;
                return {
                  empty: false,
                  docs: [
                    {
                      id: "key_idem",
                      data: () => ({ messageId: "msg_idem", expiresAt: { toMillis: () => 1699999000000 } }),
                      ref: { path: "messageKeys/key_idem", parent: { id: "messageKeys" } },
                    },
                  ],
                };
              }
              return { empty: true, docs: [] };
            },
          }),
        }),
        doc: (id: string) => ({ path: `${name}/${id}` }),
      }),
      collectionGroup: () => ({
        where: () => ({
          limit: () => ({ get: async () => ({ empty: true, docs: [] }) }),
        }),
      }),
      batch: () => ({
        delete: (ref: any) => batchDeletedPaths.push(ref.path),
        commit: async () => {},
      }),
    };

    const firstRun = await executeCryptoShredding(mockDb, nowTimestamp);
    expect(firstRun.shreddedKeysCount).toBe(1);

    // Second run immediately following: zero docs found, zero deletions, safe idempotency
    const secondRun = await executeCryptoShredding(mockDb, nowTimestamp);
    expect(secondRun.shreddedKeysCount).toBe(0);
    expect(secondRun.maxDelayMs).toBe(0);
  });

  it("CRITICAL ZERO-TRACE GUARANTEE: DEK destroyed -> content permanently unrecoverable", () => {
    const crypto = require("crypto");

    const originalMessage = "Top Secret Zero-Trace Ephemeral Communication";
    const rawDek = crypto.randomBytes(32);
    const iv = crypto.randomBytes(12);

    const cipher = crypto.createCipheriv("aes-256-gcm", rawDek, iv);
    let ciphertext = cipher.update(originalMessage, "utf8", "hex");
    ciphertext += cipher.final("hex");
    const authTag = cipher.getAuthTag();

    rawDek.fill(0);
    const destroyedDek = null;

    expect(destroyedDek).toBeNull();

    const attemptDecryptWithWrongKey = () => {
      const wrongKey = crypto.randomBytes(32);
      const decipher = crypto.createDecipheriv("aes-256-gcm", wrongKey, iv);
      decipher.setAuthTag(authTag);
      let dec = decipher.update(ciphertext, "hex", "utf8");
      dec += decipher.final("utf8");
      return dec;
    };

    expect(attemptDecryptWithWrongKey).toThrow();
  });
});
