import { chmod, mkdir, stat } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { arch, platform } from 'node:os'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const source = join(root, 'src-tauri/native/ocr.swift')
const output = join(root, 'src-tauri/native/mootool-ocr')
if (platform() !== 'darwin') {
  console.log('Native macOS OCR helper is not required on this platform.')
  process.exit(0)
}
await mkdir(dirname(output), { recursive: true })
try {
  const built = await stat(output)
  if (built.mtimeMs > (await stat(source)).mtimeMs) {
    console.log('Native OCR helper is current.')
    process.exit(0)
  }
} catch { /* First build. */ }
const target = `${arch() === 'arm64' ? 'arm64' : 'x86_64'}-apple-macosx13.0`
const result = spawnSync('xcrun', ['swiftc', '-O', source, '-target', target, '-o', output], { stdio: 'inherit' })
if (result.error || result.status !== 0) throw result.error ?? new Error(`swiftc failed (${result.status})`)
await chmod(output, 0o755)
console.log(`Built macOS OCR helper for ${target}.`)
