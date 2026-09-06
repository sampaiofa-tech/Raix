import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import * as logger from "firebase-functions/logger";
import * as crypto from "crypto";

/**
 * Retenção legal obrigatória de registros de conexão para provedores de aplicações de internet:
 * Marco Civil da Internet (Lei nº 12.965/2014, Art. 15):
 * O provedor de aplicações de internet deve manter os respectivos registros de acesso a aplicações
 * de internet, sob sigilo, em ambiente controlado e de segurança, pelo prazo de 6 meses (180 dias).
 * 
 * MINIMIZAÇÃO DE METADADOS (P1.1):
 * - O endereço IP de origem é pseudonimizado criptograficamente via HMAC-SHA256 com salt rotativo mensal.
 * - Portas efêmeras de clientes e fingerprints são omitidos para evitar perfilamento de endpoint.
 * - Expurgo automático em 180 dias gerenciado pelo Shredder ativo.
 */
export const CONNECTION_LOG_RETENTION_DAYS = 180;

export interface ConnectionLogData {
  pseudonymizedIp: string;
  functionName: string;
  timestampUtc: any;
  expiresAt: Date;
}

/**
 * Gera um salt criptográfico rotativo com periodicidade temporal (ano-mês)
 * para permitir correlações de incidentes estritamente durante a janela legal,
 * sem manter vínculo perene com o endereço IP de origem do usuário.
 */
export function getRotatingSalt(date: Date = new Date()): string {
  const yearMonth = `${date.getUTCFullYear()}-${date.getUTCMonth() + 1}`;
  return crypto.createHash("sha256").update(`raix-mci-salt-v1-${yearMonth}`).digest("hex");
}

/**
 * Pseudonimiza o IP de origem através de HMAC-SHA256 com salt rotativo mensal.
 */
export function pseudonymizeIp(rawIp: string, date: Date = new Date()): string {
  if (!rawIp || rawIp === "unknown") return "unknown";
  const salt = getRotatingSalt(date);
  return crypto.createHmac("sha256", salt).update(rawIp).digest("hex").substring(0, 32);
}

/**
 * Registra dados estritos e minimizados de conexão (IP pseudonimizado, timestamp UTC,
 * nome do endpoint e expiração de 180 dias) em conformidade com o Art. 15 do Marco Civil da Internet.
 * 
 * Executa em modo non-blocking e fail-safe para não interromper a operação criptográfica do cliente.
 */
export async function recordConnectionLog(
  request: any,
  functionName: string
): Promise<void> {
  try {
    const rawReq = request?.rawRequest || request;
    if (!rawReq) return;

    // 1. Extração do endereço IP de origem (respeitando proxies de terminação GCP/Cloudflare)
    const xForwardedFor = rawReq.headers?.["x-forwarded-for"];
    let clientIp = "unknown";
    if (typeof xForwardedFor === "string" && xForwardedFor.trim().length > 0) {
      clientIp = xForwardedFor.split(",")[0].trim();
    } else if (typeof rawReq.ip === "string" && rawReq.ip.trim().length > 0) {
      clientIp = rawReq.ip.trim();
    } else if (rawReq.socket?.remoteAddress) {
      clientIp = rawReq.socket.remoteAddress;
    }

    // 2. Pseudonimização imediata do IP com salt rotativo (P1.1: Sem IP bruto em repouso)
    const pseudoIp = pseudonymizeIp(clientIp);

    // 3. TTL estrito de 180 dias (Marco Civil Art. 15)
    const now = Date.now();
    const expiresAt = new Date(now + CONNECTION_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000);

    const timestampUtc = typeof FieldValue?.serverTimestamp === "function" 
      ? FieldValue.serverTimestamp() 
      : new Date();

    const logEntry: ConnectionLogData = {
      pseudonymizedIp: pseudoIp,
      functionName,
      timestampUtc,
      expiresAt,
    };

    const db = admin.firestore();
    if (db && typeof db.collection === "function") {
      const coll = db.collection("connectionLogs");
      if (coll && typeof coll.add === "function") {
        await coll.add(logEntry);
      }
    }
  } catch (err: any) {
    logger.warn(`recordConnectionLog: Falha não-bloqueante ao registrar log para ${functionName}:`, err?.message || err);
  }
}

