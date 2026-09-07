import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..', '..');
const indexesPath = path.join(projectRoot, 'firestore.indexes.json');
const functionsSrcDir = path.join(__dirname, '..', 'src');

console.log('[Index-as-Code CI Check] Starting verification of Firestore queries against firestore.indexes.json...');

if (!fs.existsSync(indexesPath)) {
  console.error(`[Index-as-Code CI Check] FATAL: firestore.indexes.json not found at ${indexesPath}`);
  process.exit(1);
}

const indexesConfig = JSON.parse(fs.readFileSync(indexesPath, 'utf8'));
const fieldOverrides = indexesConfig.fieldOverrides || [];
const compositeIndexes = indexesConfig.indexes || [];

// Helper to check if a collectionGroup + field has a COLLECTION_GROUP index override
function hasCollectionGroupIndex(collectionGroup, fieldPath) {
  // Check fieldOverrides
  const override = fieldOverrides.find(
    (fo) => fo.collectionGroup === collectionGroup && fo.fieldPath === fieldPath
  );
  if (override && Array.isArray(override.indexes)) {
    const hasCgScope = override.indexes.some((idx) => idx.queryScope === 'COLLECTION_GROUP');
    if (hasCgScope) return true;
  }

  // Check composite indexes
  const hasComposite = compositeIndexes.some(
    (ci) =>
      ci.collectionGroup === collectionGroup &&
      ci.queryScope === 'COLLECTION_GROUP' &&
      Array.isArray(ci.fields) &&
      ci.fields.some((f) => f.fieldPath === fieldPath)
  );
  return hasComposite;
}

// Recursively find all .ts files in functions/src
function getTsFiles(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...getTsFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith('.ts') && !entry.name.endsWith('.d.ts')) {
      files.push(fullPath);
    }
  }
  return files;
}

const tsFiles = getTsFiles(functionsSrcDir);
const missingIndexes = [];
let auditedQueriesCount = 0;

for (const filePath of tsFiles) {
  const content = fs.readFileSync(filePath, 'utf8');
  const relativePath = path.relative(projectRoot, filePath);
  const lines = content.split('\n');

  // Match collectionGroup calls: .collectionGroup("inbox") or .collectionGroup('inbox')
  const cgRegex = /\.collectionGroup\(\s*["']([^"']+)["']\s*\)/g;
  let match;

  while ((match = cgRegex.exec(content)) !== null) {
    auditedQueriesCount++;
    const collectionGroup = match[1];
    const matchIndex = match.index;

    // Find line number
    const lineNumber = content.substring(0, matchIndex).split('\n').length;

    // Look ahead in subsequent characters (up to 300 chars or next statement) for .where("field", ...) or .orderBy("field", ...)
    const contextAhead = content.substring(matchIndex, matchIndex + 500);

    const whereMatches = [...contextAhead.matchAll(/\.where\(\s*["']([^"']+)["']/g)];
    const orderByMatches = [...contextAhead.matchAll(/\.orderBy\(\s*["']([^"']+)["']/g)];

    const referencedFields = new Set([
      ...whereMatches.map((m) => m[1]),
      ...orderByMatches.map((m) => m[1]),
    ]);

    for (const fieldPath of referencedFields) {
      if (!hasCollectionGroupIndex(collectionGroup, fieldPath)) {
        missingIndexes.push({
          file: relativePath,
          line: lineNumber,
          collectionGroup,
          fieldPath,
          queryScope: 'COLLECTION_GROUP',
        });
      }
    }
  }
}

console.log(`[Index-as-Code CI Check] Audited ${auditedQueriesCount} collectionGroup query expressions across ${tsFiles.length} source files.`);

if (missingIndexes.length > 0) {
  console.error('\n❌ [Index-as-Code CI Check] FAILURE: Missing Firestore index definitions in firestore.indexes.json!\n');
  for (const item of missingIndexes) {
    console.error(
      `  - ${item.file}:${item.line} -> Requires COLLECTION_GROUP index for collectionGroup "${item.collectionGroup}" on field "${item.fieldPath}"`
    );
  }
  console.error('\nSuggested fix: add the following to fieldOverrides in firestore.indexes.json:');
  const suggestions = missingIndexes.map((item) => ({
    collectionGroup: item.collectionGroup,
    fieldPath: item.fieldPath,
    indexes: [
      { order: 'ASCENDING', queryScope: 'COLLECTION_GROUP' },
      { order: 'DESCENDING', queryScope: 'COLLECTION_GROUP' },
    ],
  }));
  console.error(JSON.stringify(suggestions, null, 2));
  process.exit(1);
}

console.log('✅ [Index-as-Code CI Check] SUCCESS: All Firestore queries are fully covered by firestore.indexes.json.');
