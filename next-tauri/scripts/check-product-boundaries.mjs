import { readFile, readdir } from 'node:fs/promises'
import { extname, join } from 'node:path'

const productRoot = new URL('../', import.meta.url)
const sourceRoots = ['src', 'src-tauri/src']
const sourceExtensions = new Set(['.ts', '.tsx', '.rs'])
const violations = []

for (const sourceRoot of sourceRoots) {
  for (const file of await walk(new URL(`${sourceRoot}/`, productRoot))) {
    if (!sourceExtensions.has(extname(file.pathname))) continue
    const contents = await readFile(file, 'utf8')
    for (const [label, pattern] of [
      ['Electron preload API', /\bwindow\.mootool\b/],
      ['Electron source path', /(?:\.\.\/)+(?:next\/(?:src|electron|out))\b/],
      ['Electron IPC channel', /\bipcRenderer\b|\bipcMain\b/]
    ]) {
      if (pattern.test(contents)) violations.push(`${file.pathname}: ${label}`)
    }
  }
}

const packageJson = JSON.parse(await readFile(new URL('package.json', productRoot), 'utf8'))
const dependencyNames = [
  ...Object.keys(packageJson.dependencies ?? {}),
  ...Object.keys(packageJson.devDependencies ?? {})
]
for (const name of dependencyNames) {
  if (name === 'electron' || name.startsWith('electron-')) {
    violations.push(`package.json: forbidden Electron dependency "${name}"`)
  }
}

const tauriConfig = JSON.parse(await readFile(new URL('src-tauri/tauri.conf.json', productRoot), 'utf8'))
if (tauriConfig.identifier !== 'com.rememberber.mootool.next.tauri') {
  violations.push('src-tauri/tauri.conf.json: independent application identifier changed')
}
if (tauriConfig.productName !== 'MooTool Next Tauri') {
  violations.push('src-tauri/tauri.conf.json: independent product name changed')
}

if (violations.length > 0) {
  console.error(`Product boundary check failed:\n${violations.map((item) => `- ${item}`).join('\n')}`)
  process.exitCode = 1
} else {
  console.log('Product boundary check passed: next-tauri remains independent.')
}

async function walk(directoryUrl) {
  const entries = await readdir(directoryUrl, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const entryUrl = new URL(`${entry.name}${entry.isDirectory() ? '/' : ''}`, directoryUrl)
    if (entry.isDirectory()) files.push(...await walk(entryUrl))
    else files.push(entryUrl)
  }
  return files
}
