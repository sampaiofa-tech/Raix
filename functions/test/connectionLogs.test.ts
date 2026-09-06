import { recordConnectionLog, CONNECTION_LOG_RETENTION_DAYS } from "../src/connectionLogs";
import * as admin from "firebase-admin";

describe("v1.4: connectionLogs Helper (Marco Civil da Internet Art. 15)", () => {
  let mockAdd: jest.Mock;

  beforeEach(() => {
    mockAdd = jest.fn().mockResolvedValue({ id: "log_123" });

    jest.spyOn(admin, "firestore").mockReturnValue({
      collection: (name: string) => {
        if (name === "accessLogs" || name === "connectionLogs") {
          return { add: mockAdd };
        }
        return { doc: jest.fn() };
      },
    } as any);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should pseudonymize client IP with rotating salt and store minimal metadata with 180-day TTL", async () => {
    const mockRequest: any = {
      rawRequest: {
        headers: {
          "x-forwarded-for": "177.18.29.30, 10.0.0.1",
        },
        ip: "10.0.0.1",
      },
    };

    await recordConnectionLog(mockRequest, "testCallable");

    expect(mockAdd).toHaveBeenCalledTimes(1);
    const loggedData = mockAdd.mock.calls[0][0];

    // P1.1: Sem IP bruto em repouso e sem porta
    expect(loggedData.pseudonymizedIp).toBeDefined();
    expect(loggedData.pseudonymizedIp).not.toBe("177.18.29.30");
    expect(loggedData.pseudonymizedIp.length).toBe(32); // Hex substring of HMAC-SHA256
    expect(loggedData.ip).toBeUndefined();
    expect(loggedData.porta).toBeUndefined();
    expect(loggedData.functionName).toBe("testCallable");
    expect(loggedData.expiresAt).toBeInstanceOf(Date);

    // REGRA MANDATÓRIA: Zero campos de conta, payload, chave ou mnemônico
    expect((loggedData as any).uid).toBeUndefined();
    expect((loggedData as any).fingerprint).toBeUndefined();
    expect((loggedData as any).messageId).toBeUndefined();
    expect((loggedData as any).payload).toBeUndefined();
    expect((loggedData as any).dek).toBeUndefined();
    expect((loggedData as any).mnemonic).toBeUndefined();

    // Verify TTL is approximately 180 days in future
    const diffDays = Math.round((loggedData.expiresAt.getTime() - Date.now()) / (1000 * 60 * 60 * 24));
    expect(diffDays).toBe(CONNECTION_LOG_RETENTION_DAYS);
  });

  it("should fallback to socket address when x-forwarded headers are absent and pseudonymize correctly", async () => {
    const mockRequest: any = {
      rawRequest: {
        headers: {},
        socket: {
          remoteAddress: "200.100.50.25",
        },
      },
    };

    await recordConnectionLog(mockRequest, "storeMessageKey");

    expect(mockAdd).toHaveBeenCalledTimes(1);
    const loggedData = mockAdd.mock.calls[0][0];

    expect(loggedData.pseudonymizedIp).toBeDefined();
    expect(loggedData.ip).toBeUndefined();
    expect(loggedData.porta).toBeUndefined();
    expect(loggedData.functionName).toBe("storeMessageKey");
  });

  it("should fail safe without throwing if firestore add fails", async () => {
    mockAdd.mockRejectedValueOnce(new Error("Firestore write unavailable"));

    const mockRequest: any = {
      rawRequest: {
        headers: { "x-forwarded-for": "1.2.3.4" },
      },
    };

    await expect(recordConnectionLog(mockRequest, "safeOp")).resolves.not.toThrow();
  });
});
