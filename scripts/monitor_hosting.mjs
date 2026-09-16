import https from 'https';
import fs from 'fs';
import path from 'path';
import { performance } from 'perf_hooks';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

console.log("================================================================================");
console.log("🌐 INICIANDO MONITORAMENTO DE HOSPEDAGEM E LATÊNCIA DE REDE (B)");
console.log("================================================================================\n");

function probe(url, options = {}, postData = null) {
  return new Promise((resolve) => {
    const parsed = new URL(url);
    const start = performance.now();
    const reqOpts = {
      hostname: parsed.hostname,
      port: parsed.port || 443,
      path: parsed.pathname + parsed.search,
      method: options.method || 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) RaixHostingWatchdog/1.6',
        ...(options.headers || {})
      },
      timeout: options.timeout || 15000
    };

    const req = https.request(reqOpts, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        const latencyMs = Math.round(performance.now() - start);
        resolve({
          url,
          status: res.statusCode,
          latencyMs,
          headers: res.headers,
          error: null
        });
      });
    });

    req.on('timeout', () => {
      req.destroy();
      const latencyMs = Math.round(performance.now() - start);
      resolve({ url, status: null, latencyMs, headers: {}, error: 'TIMEOUT (15s)' });
    });

    req.on('error', (err) => {
      const latencyMs = Math.round(performance.now() - start);
      resolve({ url, status: null, latencyMs, headers: {}, error: err.message });
    });

    if (postData) {
      req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    }
    req.end();
  });
}

async function runHostingMonitor() {
  const targets = [
    {
      name: "GitHub Pages (Portal Oficial)",
      provider: "GitHub Pages",
      region: "Global CDN (Fastly)",
      url: "https://raixtech.com/",
      method: "GET",
      expectedStatuses: [200]
    },
    {
      name: "GitHub Pages (Termos de Uso)",
      provider: "GitHub Pages",
      region: "Global CDN (Fastly / brazilsouth)",
      url: "https://raixtech.com/termos.html",
      method: "GET",
      expectedStatuses: [200]
    },
    {
      name: "GitHub Pages (Privacidade)",
      provider: "GitHub Pages",
      region: "Global CDN (Fastly / brazilsouth)",
      url: "https://raixtech.com/privacidade.html",
      method: "GET",
      expectedStatuses: [200]
    },
    {
      name: "Cloudflare (Domínio raixtech.com.br)",
      provider: "Cloudflare",
      region: "Edge Anycast (Borda Brasil/Global)",
      url: "https://raixtech.com.br/",
      method: "GET",
      expectedStatuses: [301, 302, 200]
    },
    {
      name: "Firebase Cloud Functions (reportAbuse)",
      provider: "Google Cloud",
      region: "us-central1 (Iowa, USA)",
      url: "https://us-central1-gen-lang-client-0858445711.cloudfunctions.net/reportAbuse",
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ data: {} }),
      expectedStatuses: [200, 400, 401] // 401/400 confirma que a function está ativa e aplicando autenticação/validação Callable
    },
    {
      name: "Google Cloud Firestore (Cluster Principal)",
      provider: "Google Cloud",
      region: "nam5 (Multi-região us-central)",
      url: "https://firestore.googleapis.com/v1/projects/gen-lang-client-0858445711/databases/(default)",
      method: "GET",
      expectedStatuses: [401, 200] // 401 confirma que o endpoint está ativo e aplicando segurança
    }
  ];

  const results = [];
  let allHealthy = true;

  for (const t of targets) {
    process.stdout.write(`  Sondando ${t.name}... `);
    const res = await probe(t.url, { method: t.method, headers: t.headers }, t.body);
    const isExpected = res.status !== null && t.expectedStatuses.includes(res.status);
    const edgeInfo = res.headers['cf-ray'] ? `CF-RAY: ${res.headers['cf-ray']}` : (res.headers['server'] || 'Nativo');

    if (!isExpected) {
      allHealthy = false;
      console.log(`❌ FALHOU (HTTP ${res.status}, ${res.latencyMs}ms) ${res.error || ''}`);
    } else {
      console.log(`✅ OK (HTTP ${res.status}, ${res.latencyMs}ms) [${edgeInfo}]`);
    }

    results.push({
      name: t.name,
      provider: t.provider,
      region: t.region,
      url: t.url,
      status: res.status,
      latencyMs: res.latencyMs,
      expected: isExpected,
      edgeInfo,
      error: res.error
    });
  }

  // Gravar métricas em JSON
  const outputData = {
    timestamp: new Date().toISOString(),
    allHealthy,
    results
  };

  const outputPath = path.join(rootDir, 'hosting_metrics.json');
  fs.writeFileSync(outputPath, JSON.stringify(outputData, null, 2), 'utf8');

  // Gerar snippet HTML para o relatório
  let htmlSnippet = `
  <h3>🌐 Disponibilidade e Latência de Hospedagem</h3>
  <table style="width:100%;border-collapse:collapse;margin-bottom:20px;font-size:13px;">
    <thead>
      <tr style="background-color:#f1f5f9;color:#475569;text-align:left;">
        <th style="padding:10px 12px;border-bottom:1px solid #e2e8f0;">Serviço / Endpoint</th>
        <th style="padding:10px 12px;border-bottom:1px solid #e2e8f0;">Provedor & Região</th>
        <th style="padding:10px 12px;border-bottom:1px solid #e2e8f0;">HTTP</th>
        <th style="padding:10px 12px;border-bottom:1px solid #e2e8f0;">Latência</th>
        <th style="padding:10px 12px;border-bottom:1px solid #e2e8f0;">Status</th>
      </tr>
    </thead>
    <tbody>
  `;

  for (const r of results) {
    const badge = r.expected
      ? `<span style="color:#059669;font-weight:bold;">✅ ONLINE</span>`
      : `<span style="color:#dc2626;font-weight:bold;">❌ OFFLINE</span>`;
    const latencyColor = r.latencyMs < 1000 ? "#059669" : (r.latencyMs < 3000 ? "#d97706" : "#dc2626");

    htmlSnippet += `
      <tr>
        <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;"><strong>${r.name}</strong></td>
        <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;"><small>${r.provider} (${r.region})</small></td>
        <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;"><code>${r.status || 'ERR'}</code></td>
        <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;color:${latencyColor};font-weight:bold;">${r.latencyMs} ms</td>
        <td style="padding:10px 12px;border-bottom:1px solid #e2e8f0;">${badge}</td>
      </tr>
    `;
  }
  htmlSnippet += `</tbody></table>`;
  fs.writeFileSync(path.join(rootDir, 'hosting_table.html'), htmlSnippet, 'utf8');

  // Gerar snippet em texto puro
  let txtSnippet = "\nDISPONIBILIDADE E LATÊNCIA DE HOSPEDAGEM:\n";
  for (const r of results) {
    const statusMark = r.expected ? "[ONLINE]" : "[OFFLINE]";
    txtSnippet += `- ${r.name} (${r.provider}): HTTP ${r.status || 'ERR'} | ${r.latencyMs}ms ${statusMark}\n`;
  }
  fs.writeFileSync(path.join(rootDir, 'hosting_table.txt'), txtSnippet, 'utf8');

  console.log("\n================================================================================");
  console.log(`📊 MONITORAMENTO CONCLUÍDO: ${allHealthy ? "TODOS OS ENDPOINTS ONLINE" : "FALHA EM UM OU MAIS ENDPOINTS"}`);
  console.log("================================================================================\n");

  if (!allHealthy) {
    process.exit(1);
  } else {
    process.exit(0);
  }
}

runHostingMonitor();
