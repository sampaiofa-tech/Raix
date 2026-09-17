const { execSync } = require("child_process");

let diffText = "";
try {
  diffText += execSync("git diff HEAD", { encoding: "utf8" }) + "\n";
  diffText += execSync("git diff --cached", { encoding: "utf8" }) + "\n";
  diffText += execSync("git log -p -5", { encoding: "utf8" }) + "\n";
} catch (e) {
  // Fallback se git falhar
  console.warn("Aviso ao extrair diffs do git:", e.message);
}

const patterns = [
  { name: "GitHub Token (gho_)", regex: /gho_[A-Za-z0-9_]+/ },
  { name: "GitHub Token (ghp_)", regex: /ghp_[A-Za-z0-9_]+/ },
  { name: "GitHub PAT (github_pat_)", regex: /github_pat_[A-Za-z0-9_]+/ },
  { name: "Google API Key (AIzaSy)", regex: /AIzaSy[A-Za-z0-9_-]{33}/ },
  { name: "Password Parameter", regex: /password\s*=\s*["'][^\s"']+["']/i },
  { name: "Refresh Token", regex: /refresh_token\s*[:=]\s*["'][^\s"']+["']/i },
  { name: "Client Email Credential", regex: /client_email\s*[:=]\s*["'][^\s"']+["']/i },
  { name: "Private Key Header", regex: /BEGIN (RSA |EC )?PRIVATE KEY/ }
];

const lines = diffText.split("\n");
let currentFile = "unknown";
let leaksFound = 0;

for (const line of lines) {
  if (line.startsWith("+++ b/")) {
    currentFile = line.substring(6);
  }
  
  if (line.startsWith("+") && !line.startsWith("+++")) {
    // Ignora falsos positivos (regras em docs, linter, e scripts de consolidação)
    if (
      currentFile.endsWith(".md") || 
      currentFile.endsWith("verify_secrets.cjs") || 
      currentFile.endsWith("generate_consolidated.py")
    ) {
      continue;
    }

    for (const p of patterns) {
      if (p.regex.test(line)) {
        console.error(`ALERTA: Padrão proibido detectado no diff: ${p.name} em arquivo: ${currentFile}`);
        leaksFound++;
      }
    }
  }
}

if (leaksFound > 0) {
  console.error(`FALHA NA VARREDURA: ${leaksFound} ocorrências proibidas encontradas.`);
  process.exit(1);
} else {
  console.log("SUCESSO: Varredura de segredos limpa! Zero assinaturas de segredos encontradas nas modificações.");
}
