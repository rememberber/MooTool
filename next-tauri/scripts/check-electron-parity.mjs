import { readFile } from 'node:fs/promises'

const root = new URL('../', import.meta.url)
const violations = []

async function mustExist(relativePath, label) {
  try {
    await readFile(new URL(relativePath, root))
  } catch {
    violations.push(`missing ${label}: ${relativePath}`)
  }
}

await mustExist('doc/electron-parity.md', 'parity catalog')
await mustExist('doc/feature-baseline.md', 'feature baseline')
await mustExist('doc/adr/012-ai-integration-scope.md', 'AI scope ADR')

const tsSettings = await readFile(new URL('src/platform/contracts/settings.ts', root), 'utf8')
const rsSettings = await readFile(new URL('src-tauri/src/contracts/settings.rs', root), 'utf8')
const tsSchema = tsSettings.match(/SETTINGS_SCHEMA_VERSION = (\d+)/)?.[1]
const rsSchema = rsSettings.match(/SETTINGS_SCHEMA_VERSION: u32 = (\d+)/)?.[1]
if (!tsSchema || !rsSchema || tsSchema !== rsSchema) {
  violations.push(`settings schema mismatch: TS=${tsSchema ?? '?'} Rust=${rsSchema ?? '?'}`)
}

if (!tsSettings.includes('navigationStyle')) violations.push('TS layout.navigationStyle missing')
if (!rsSettings.includes('navigation_style')) violations.push('Rust layout.navigation_style missing')
if (!rsSettings.includes('compact_navigation')) violations.push('Rust layout.compact_navigation missing')
if (!rsSettings.includes('show_separators')) violations.push('Rust layout.show_separators missing')

const catalog = await readFile(new URL('src/app/toolCatalog.ts', root), 'utf8')
const engineeringLabs = (catalog.match(/engineeringOnly: true/g) ?? []).length
if (engineeringLabs !== 2) {
  violations.push(`expected 2 engineering-only tools, found ${engineeringLabs}`)
}

const parityDoc = await readFile(new URL('doc/electron-parity.md', root), 'utf8')
if (!parityDoc.includes('ADR-012')) violations.push('electron-parity.md must reference ADR-012 for AI boundary')

if (violations.length > 0) {
  console.error(`Electron parity gate failed:\n${violations.map((item) => `- ${item}`).join('\n')}`)
  process.exitCode = 1
} else {
  console.log('Electron parity gate passed: baseline docs, schema v%s, layout fields, engineering labs.', tsSchema)
}
