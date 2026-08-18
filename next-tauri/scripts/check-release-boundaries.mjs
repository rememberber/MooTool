import { access, readFile } from 'node:fs/promises'

const root = new URL('../', import.meta.url)
const packageJson = JSON.parse(await readFile(new URL('package.json', root), 'utf8'))
const tauriConfig = JSON.parse(await readFile(new URL('src-tauri/tauri.conf.json', root), 'utf8'))
const cargoToml = await readFile(new URL('src-tauri/Cargo.toml', root), 'utf8')
const workflow = await readFile(new URL('../.github/workflows/next-tauri-release.yml', root), 'utf8')
const promotionWorkflow = await readFile(new URL('../.github/workflows/next-tauri-promote-update.yml', root), 'utf8')
const productManifest = JSON.parse(await readFile(new URL('../update-manifest.json', root), 'utf8'))
const violations = []
const updaterManifestUrl = 'https://github.com/rememberber/MooTool/releases/download/next-tauri-updater/latest.json'

if (packageJson.version !== tauriConfig.version) violations.push('package.json and tauri.conf.json versions differ')
if (!new RegExp(`^version = "${escapeRegExp(packageJson.version)}"$`, 'm').test(cargoToml)) {
  violations.push('Cargo.toml package version differs from package.json')
}
if (tauriConfig.identifier !== 'com.rememberber.mootool.next.tauri') violations.push('independent Tauri bundle identifier changed')
if (tauriConfig.productName !== 'MooTool Next Tauri') violations.push('independent Tauri product name changed')
if (tauriConfig.bundle?.active !== true) violations.push('Tauri release bundling must stay enabled')
if (tauriConfig.bundle?.targets !== 'all') violations.push('Tauri release targets must remain platform-complete')
if (tauriConfig.bundle?.createUpdaterArtifacts !== true) violations.push('Tauri v2 updater artifacts must stay enabled')
if (!Array.isArray(tauriConfig.plugins?.updater?.endpoints) || tauriConfig.plugins.updater.endpoints.length !== 0) {
  violations.push('static updater endpoints must be resolved only through products.next-tauri')
}
const updaterPublicKey = tauriConfig.plugins?.updater?.pubkey
if (typeof updaterPublicKey !== 'string' || !Buffer.from(updaterPublicKey, 'base64').toString('utf8').includes('minisign public key')) {
  violations.push('Tauri updater public key is missing or invalid')
}
if (tauriConfig.plugins?.updater?.windows?.installMode !== 'passive') violations.push('Windows updater install mode must remain passive')
if (!workflow.includes('next-tauri-v*')) violations.push('release workflow must use the independent next-tauri-v* tag namespace')
if (!workflow.includes('--latest=false')) violations.push('release workflow must not claim the repository-wide Latest release')
if (!workflow.includes('TAURI_SIGNING_PRIVATE_KEY')) violations.push('release workflow must inject the updater signing key')
if (!workflow.includes('.app.tar.gz.sig') || !workflow.includes('.AppImage.sig') || !workflow.includes('.exe.sig')) {
  violations.push('release workflow must retain every primary updater signature')
}
if (!workflow.includes('Verify macOS updater, DMG, and first launch') ||
    !workflow.includes('Verify Windows updater, install, first launch, and uninstall') ||
    !workflow.includes('Verify Linux updater, packages, and first launch')) {
  violations.push('release workflow must retain platform installer and first-launch smoke checks')
}
if (/electron/i.test(workflow)) violations.push('Tauri release workflow must not invoke Electron build or release steps')
if (!promotionWorkflow.includes("types:\n      - published")) violations.push('updater promotion must wait for a published release')
if (!promotionWorkflow.includes('next-tauri-updater') || !promotionWorkflow.includes('update-registry')) {
  violations.push('promotion workflow must publish the isolated channel and root registry node')
}
if (!promotionWorkflow.includes('--latest=false')) violations.push('updater channel release must not claim repository Latest')
const tauriProduct = productManifest.products?.['next-tauri']
if (tauriProduct?.displayName !== 'MooTool Next Tauri') violations.push('root manifest next-tauri product identity is invalid')
if (tauriProduct?.updaterManifestUrl !== updaterManifestUrl) violations.push('root manifest next-tauri updater channel is invalid')

try {
  await access(new URL(`release-notes/${packageJson.version}.md`, root))
} catch {
  violations.push(`release-notes/${packageJson.version}.md is missing`)
}

if (violations.length) {
  console.error(`Tauri release boundary check failed:\n${violations.map((item) => `- ${item}`).join('\n')}`)
  process.exitCode = 1
} else {
  console.log(`Tauri release boundary check passed for ${packageJson.version}.`)
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
