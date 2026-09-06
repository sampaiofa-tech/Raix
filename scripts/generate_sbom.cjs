/**
 * SBOM (Software Bill of Materials) Generator for Raix (P0.4 - Supply Chain Integrity)
 * Compliant with CycloneDX v1.5 JSON specification.
 */
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const REPO_ROOT = path.resolve(__dirname, "..");
const SBOM_DIR = path.resolve(REPO_ROOT, "docs", "sbom");

if (!fs.existsSync(SBOM_DIR)) {
  fs.mkdirSync(SBOM_DIR, { recursive: true });
}

function generateCycloneDxHeader(componentName, version, description) {
  const serialNumber = "urn:uuid:" + crypto.randomUUID();
  return {
    bomFormat: "CycloneDX",
    specVersion: "1.5",
    serialNumber,
    version: 1,
    metadata: {
      timestamp: new Date().toISOString(),
      tools: [
        {
          vendor: "Raix Security Engineering",
          name: "raix-sbom-generator",
          version: "1.0.0",
        },
      ],
      component: {
        type: "application",
        name: componentName,
        version: version,
        description: description,
        licenses: [{ license: { id: "AGPL-3.0-or-later" } }],
      },
    },
    components: [],
  };
}

// 1. Generate Functions SBOM
function buildFunctionsSbom() {
  const pkgJsonPath = path.resolve(REPO_ROOT, "functions", "package.json");
  const pkgLockPath = path.resolve(REPO_ROOT, "functions", "package-lock.json");

  const pkgJson = JSON.parse(fs.readFileSync(pkgJsonPath, "utf8"));
  let pkgLock = null;
  if (fs.existsSync(pkgLockPath)) {
    pkgLock = JSON.parse(fs.readFileSync(pkgLockPath, "utf8"));
  }

  const sbom = generateCycloneDxHeader(
    "raix-cloud-functions",
    pkgJson.version || "1.4.0",
    "Raix Cloud Functions Backend & Security Services"
  );

  const deps = { ...pkgJson.dependencies, ...pkgJson.devDependencies };

  for (const [name, versionSpec] of Object.entries(deps)) {
    const lockInfo = pkgLock && pkgLock.packages && pkgLock.packages[`node_modules/${name}`];
    const resolvedVersion = (lockInfo && lockInfo.version) || versionSpec.replace(/^[\^~]/, "");
    const integrity = lockInfo && lockInfo.integrity;

    const hashes = [];
    if (integrity && integrity.startsWith("sha512-")) {
      hashes.push({
        alg: "SHA-512",
        content: integrity.replace("sha512-", ""),
      });
    }

    sbom.components.push({
      type: "library",
      name: name,
      version: resolvedVersion,
      purl: `pkg:npm/${name}@${resolvedVersion}`,
      scope: pkgJson.dependencies[name] ? "required" : "optional",
      hashes: hashes.length > 0 ? hashes : undefined,
    });
  }

  const outputPath = path.resolve(SBOM_DIR, "sbom-functions.json");
  fs.writeFileSync(outputPath, JSON.stringify(sbom, null, 2), "utf8");
  console.log(`[SBOM] Wrote functions SBOM with ${sbom.components.length} components to ${outputPath}`);
}

// 2. Generate ComposeApp (KMP) SBOM
function buildComposeAppSbom() {
  const tomlPath = path.resolve(REPO_ROOT, "gradle", "libs.versions.toml");
  const tomlContent = fs.readFileSync(tomlPath, "utf8");

  const sbom = generateCycloneDxHeader(
    "raix-compose-client",
    "1.4.0",
    "Raix Kotlin Multiplatform Secure Client (Android, Desktop, Wasm)"
  );

  // Simple TOML parsing for [libraries]
  const libraries = [];
  const lines = tomlContent.split("\n");
  let inLibraries = false;
  let inVersions = false;
  const versions = {};

  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed === "[versions]") {
      inVersions = true;
      inLibraries = false;
      continue;
    } else if (trimmed === "[libraries]") {
      inLibraries = true;
      inVersions = false;
      continue;
    } else if (trimmed.startsWith("[")) {
      inLibraries = false;
      inVersions = false;
      continue;
    }

    if (inVersions && trimmed.includes("=")) {
      const parts = trimmed.split("=");
      const key = parts[0].trim();
      const val = parts[1].trim().replace(/['"]/g, "");
      versions[key] = val;
    }

    if (inLibraries && trimmed.includes("=")) {
      const parts = trimmed.split("=");
      const libKey = parts[0].trim();
      const val = parts.slice(1).join("=").trim();

      const groupMatch = val.match(/group\s*=\s*["']([^"']+)["']/);
      const nameMatch = val.match(/name\s*=\s*["']([^"']+)["']/);
      const versionRefMatch = val.match(/version\.ref\s*=\s*["']([^"']+)["']/);
      const versionDirectMatch = val.match(/version\s*=\s*["']([^"']+)["']/);

      if (groupMatch && nameMatch) {
        const group = groupMatch[1];
        const name = nameMatch[1];
        let ver = "unknown";
        if (versionRefMatch && versions[versionRefMatch[1]]) {
          ver = versions[versionRefMatch[1]];
        } else if (versionDirectMatch) {
          ver = versionDirectMatch[1];
        }

        libraries.push({ group, name, version: ver });
      }
    }
  }

  for (const lib of libraries) {
    const component = {
      type: "library",
      group: lib.group,
      name: lib.name,
      version: lib.version,
      purl: `pkg:maven/${lib.group}/${lib.name}@${lib.version}`,
    };

    if (lib.group === "org.bouncycastle" && lib.version === "1.79") {
      component.hashes = [
        {
          alg: "SHA-256",
          content: "0d81ecc3124536b539bce9aa3fe9621b7f84c9cee371b635a5b31c78b79ab1da",
        },
      ];
    }

    sbom.components.push(component);
  }

  const outputPath = path.resolve(SBOM_DIR, "sbom-composeApp.json");
  fs.writeFileSync(outputPath, JSON.stringify(sbom, null, 2), "utf8");
  console.log(`[SBOM] Wrote composeApp SBOM with ${sbom.components.length} components to ${outputPath}`);
}

buildFunctionsSbom();
buildComposeAppSbom();
