const fs = require('fs');
const glob = [
  'docs/CODIGO_CONSOLIDADO.md',
  'docs/CODIGO_CONSOLIDADO_BACKEND.md',
  'docs/CODIGO_CONSOLIDADO_CLIENTE.md',
  'docs/CODIGO_CONSOLIDADO_INFRA_SEGURANCA.md',
  'docs/CODIGO_CONSOLIDADO_UI.md'
];

const patterns = [
  { name: 'GitHub Token (gho_)', regex: /gho_[A-Za-z0-9_]+/ },
  { name: 'GitHub Token (ghp_)', regex: /ghp_[A-Za-z0-9_]+/ },
  { name: 'GitHub PAT (github_pat_)', regex: /github_pat_[A-Za-z0-9_]+/ },
  { name: 'Google API Key (AIzaSy)', regex: /AIzaSy[A-Za-z0-9_-]{33}/ },
  { name: 'Password Parameter', regex: /password\s*=\s*["'][^\s"']+["']/i },
  { name: 'Refresh Token', regex: /refresh_token\s*[:=]\s*["'][^\s"']+["']/i },
  { name: 'Client Email Credential', regex: /client_email\s*[:=]\s*["'][^\s"']+["']/i },
  { name: 'Private Key Header', regex: /BEGIN (RSA |EC )?PRIVATE KEY/ }
];

let leaks = 0;
for (const f of glob) {
  if (!fs.existsSync(f)) {
    console.error(`File missing: ${f}`);
    leaks++;
    continue;
  }
  const lines = fs.readFileSync(f, 'utf8').split('\n');
  lines.forEach((l, idx) => {
    patterns.forEach(p => {
      if (p.regex.test(l)) {
        console.error(`Leak in ${f}:${idx + 1} -> ${p.name} : ${l.trim()}`);
        leaks++;
      }
    });
  });
}

if (leaks === 0) {
  console.log('ALL CONSOLIDATED FILES ARE 100% VERIFIED CLEAN OF CREDENTIALS AND TOKENS!');
} else {
  console.error(`TOTAL LEAKS DETECTED: ${leaks}`);
  process.exit(1);
}
