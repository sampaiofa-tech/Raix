import {
  trackIdentityRead,
  trackPermissionDenied,
  READ_ANOMALY_THRESHOLD,
  BRUTE_FORCE_DENIAL_THRESHOLD,
} from "../src/anomalyDetector";

describe("P1.3: Anomaly Detection & Brute Force Defense Suite", () => {
  let memoryStore: Record<string, any>;
  let mockDb: any;

  beforeEach(() => {
    memoryStore = {};

    mockDb = {
      collection: (colName: string) => ({
        doc: (docId?: string) => {
          const id = docId || `auto_${Math.random()}`;
          const key = `${colName}/${id}`;
          return {
            id,
            set: async (data: any, options?: any) => {
              if (options?.merge && memoryStore[key]) {
                memoryStore[key] = { ...memoryStore[key], ...data };
              } else {
                memoryStore[key] = data;
              }
            },
          };
        },
      }),
      runTransaction: async (updateFunction: (transaction: any) => Promise<any>) => {
        const mockTransaction = {
          get: async (ref: any) => {
            const key = ref.id.startsWith("read_")
              ? `anomalyTrackers/${ref.id}`
              : `anomalyTrackers/${ref.id}`;
            const data = memoryStore[key];
            return {
              exists: !!data,
              data: () => data,
            };
          },
          set: (ref: any, data: any, options?: any) => {
            const key = `anomalyTrackers/${ref.id}`;
            if (options?.merge && memoryStore[key]) {
              memoryStore[key] = { ...memoryStore[key], ...data };
            } else {
              memoryStore[key] = data;
            }
          },
        };
        return updateFunction(mockTransaction);
      },
    };
  });

  it("should trigger READ_SPIKE security anomaly when read frequency reaches threshold", async () => {
    const identityHash = "a1b2c3d4e5f60000000000000000000000000000000000000000000000000001";
    const callerUid = "user_victim_reader";
    const now = 1700000000000;

    // Simulate reads below threshold
    for (let i = 1; i < READ_ANOMALY_THRESHOLD; i++) {
      const isAnomaly = await trackIdentityRead(mockDb, identityHash, callerUid, now);
      expect(isAnomaly).toBe(false);
    }

    // Next read hits threshold -> triggers anomaly
    const isAnomaly = await trackIdentityRead(mockDb, identityHash, callerUid, now);
    expect(isAnomaly).toBe(true);

    // Verify security anomaly logged in store
    const anomalyKeys = Object.keys(memoryStore).filter((k) => k.startsWith("securityAnomalies/"));
    expect(anomalyKeys.length).toBe(1);
    expect(memoryStore[anomalyKeys[0]].type).toBe("READ_SPIKE");
  });

  it("should trigger BRUTE_FORCE_DENIAL when repeated permission denials occur", async () => {
    const targetIdentity = "attacker_target_identity_hash_00000000000000000000000000000000002";
    const callerUid = "attacker_eve";
    const now = 1700000000000;

    // Simulate denials below threshold
    for (let i = 1; i < BRUTE_FORCE_DENIAL_THRESHOLD; i++) {
      const isBruteForce = await trackPermissionDenied(
        mockDb,
        targetIdentity,
        callerUid,
        "permission-denied: wrong identity",
        now
      );
      expect(isBruteForce).toBe(false);
    }

    // 5th denial triggers brute force alert
    const isBruteForce = await trackPermissionDenied(
      mockDb,
      targetIdentity,
      callerUid,
      "permission-denied: wrong identity",
      now
    );
    expect(isBruteForce).toBe(true);

    const anomalyKeys = Object.keys(memoryStore).filter((k) => k.startsWith("securityAnomalies/"));
    expect(anomalyKeys.length).toBe(1);
    expect(memoryStore[anomalyKeys[0]].type).toBe("BRUTE_FORCE_DENIAL");
  });
});
