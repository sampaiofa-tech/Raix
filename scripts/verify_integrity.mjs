import fs from 'fs';
import path from 'path';
import crypto from 'crypto';
import { execSync } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

console.log("================================================================================");
console.log("🛡️  INICIANDO AUDITORIA DE INTEGRIDADE DE ARTEFATOS E CONFIGURAÇÃO (A.1)");
console.log("================================================================================\n");

let passed = 0;
let warnings = 0;
let errors = 0;

function assert(description, condition, details = "") {
  if (condition) {
    console.log(`  ✅ [PASS] ${description}`);
    passed++;
  } else {
    console.error(`  ❌ [FAIL] ${description} ${details}`);
    errors++;
  }
}

function warn(description, details = "") {
  console.warn(`  ⚠️ [WARN] ${description} ${details}`);
  warnings++;
}

// 1. Verificação de Hashes de Artefatos (Anti-Tamper)
console.log("--- 1. Hashes de Artefatos Oficiais (docs/release-checksums.sha256) ---");
const checksumFile = path.join(rootDir, 'docs', 'release-checksums.sha256');

if (!fs.existsSync(checksumFile)) {
  assert("Arquivo de checksums oficial existe", false, `(${checksumFile} não encontrado)`);
} else {
  assert("Arquivo de checksums oficial existe", true);
  const lines = fs.readFileSync(checksumFile, 'utf8')
    .split('\n')
    .map(l => l.trim())
    .filter(l => l && !l.startsWith('#'));

  const searchLocations = [
    path.join(rootDir, 'composeApp', 'build', 'outputs', 'apk', 'release'),
    path.join(rootDir, 'composeApp', 'build', 'outputs', 'bundle', 'release'),
    path.join(rootDir, 'composeApp', 'build', 'compose', 'binaries', 'main', 'msi'),
    path.join(rootDir, 'dist')
  ];

  let verifiedBinaries = 0;

  for (const line of lines) {
    const parts = line.split(/\s+/);
    if (parts.length < 2) continue;
    const expectedHash = parts[0].toLowerCase();
    const fileName = parts[1];

    let foundPath = null;
    for (const loc of searchLocations) {
      const candidate = path.join(loc, fileName);
      if (fs.existsSync(candidate)) {
        foundPath = candidate;
        break;
      }
    }

    if (foundPath) {
      const fileBuffer = fs.readFileSync(foundPath);
      const actualHash = crypto.createHash('sha256').update(fileBuffer).digest('hex').toLowerCase();
      const match = actualHash === expectedHash;
      assert(
        `Artefato ${fileName} íntegro (SHA-256 verificado)`,
        match,
        match ? `(${actualHash.slice(0, 16)}...)` : `Esperado: ${expectedHash}, Atual: ${actualHash}`
      );
      if (match) verifiedBinaries++;
    } else {
      console.log(`  ℹ️ [INFO] Artefato ${fileName} não presente no runner local (ignorado em checkout limpo).`);
    }
  }

  console.log(`  Total de binários físicos verificados contra o ledger: ${verifiedBinaries}`);
}

// 2. Integridade dos Metadados de Verificação Gradle (verification-metadata.xml)
console.log("\n--- 2. Integridade de Dependências Gradle (verification-metadata.xml) ---");
const verificationMetaPath = path.join(rootDir, 'gradle', 'verification-metadata.xml');

if (!fs.existsSync(verificationMetaPath)) {
  assert("verification-metadata.xml presente em gradle/", false);
} else {
  const content = fs.readFileSync(verificationMetaPath, 'utf8');
  assert("verification-metadata.xml presente em gradle/", true);
  assert(
    "Verificação de SHA-256 ativada para Bouncy Castle (bcprov-jdk18on)",
    content.includes("bcprov-jdk18on") && content.includes("sha256")
  );
  assert(
    "Verificação de SHA-256 ativada para Bouncy Castle PKIX (bcpkix-jdk18on)",
    content.includes("bcpkix-jdk18on") && content.includes("sha256")
  );
}

// 3. Integridade do SBOM CycloneDX v1.5
console.log("\n--- 3. Integridade do SBOM CycloneDX v1.5 ---");
const sbomScript = path.join(rootDir, 'scripts', 'generate_sbom.cjs');
if (fs.existsSync(sbomScript)) {
  try {
    execSync(`node "${sbomScript}"`, { cwd: rootDir, stdio: 'pipe' });
    assert("Regeneração do SBOM executada com sucesso", true);

    const sbomDiff = execSync("git diff --name-only docs/sbom/", { cwd: rootDir, encoding: 'utf8' }).trim();
    assert(
      "SBOM em docs/sbom/ perfeitamente sincronizado com o grafo de dependências",
      sbomDiff === "",
      sbomDiff ? `(Divergências em: ${sbomDiff})` : ""
    );
  } catch (err) {
    assert("Geração do SBOM sem erros", false, err.message);
  }
} else {
  warn("Script generate_sbom.cjs não encontrado.");
}

// 4. Integridade da Árvore Git (Cleanliness de Configurações Críticas)
console.log("\n--- 4. Integridade de Configurações de Projeto (Git Tree) ---");
try {
  const criticalFilesDiff = execSync("git diff --name-only HEAD -- firestore.rules firestore.indexes.json gradle.properties", { cwd: rootDir, encoding: 'utf8' }).trim();
  assert(
    "Arquivos críticos de governança (firestore.rules, firestore.indexes.json, gradle.properties) intocados",
    criticalFilesDiff === "",
    criticalFilesDiff ? `(Modificados: ${criticalFilesDiff})` : ""
  );
} catch (err) {
  warn("Não foi possível inspecionar o git diff contra HEAD:", err.message);
}

console.log("\n================================================================================");
console.log(`📊 RESULTADO DA AUDITORIA DE INTEGRIDADE: ${passed} PASS, ${errors} FAIL, ${warnings} WARN`);
console.log("================================================================================\n");

if (errors > 0) {
  process.exit(1);
} else {
  process.exit(0);
}
