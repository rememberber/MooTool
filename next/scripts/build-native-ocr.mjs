import { mkdir, mkdtemp, stat, chmod, rm } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { tmpdir } from 'node:os'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const output = join(root, 'out/native/mootool-ocr')
await mkdir(dirname(output), { recursive: true })
if (process.platform !== 'darwin') {
  console.log('Native macOS OCR helper is not required on this platform.')
  process.exit(0)
}
const source = join(root, 'electron/native/ocr.swift')
try {
  const built = await stat(output)
  if (built.mtimeMs > (await stat(source)).mtimeMs && built.mtimeMs > (await stat(fileURLToPath(import.meta.url))).mtimeMs) {
    console.log('Native OCR helper is current.')
    process.exit(0)
  }
} catch { /* First build. */ }

const temporary = await mkdtemp(join(tmpdir(), 'mootool-build-ocr-'))
function run(command, args) {
  const result = spawnSync(command, args, { stdio: 'inherit', shell: false })
  if (result.error || result.status !== 0) throw result.error ?? new Error(`${command} failed (${result.status})`)
}
try {
  const binaries = []
  for (const arch of ['arm64', 'x86_64']) {
    const binary = join(temporary, `mootool-ocr-${arch}`)
    run('xcrun', ['swiftc', '-O', source, '-target', `${arch}-apple-macosx13.0`, '-module-cache-path', join(temporary, 'modules'), '-o', binary])
    binaries.push(binary)
  }
  run('xcrun', ['lipo', '-create', ...binaries, '-output', output])
  await chmod(output, 0o755)
  console.log('Built universal macOS OCR helper (arm64 + x86_64).')
} finally { await rm(temporary, { recursive: true, force: true }) }
