import { existsSync } from 'node:fs'
import { lstat, mkdir, readdir, writeFile } from 'node:fs/promises'
import { dirname, join, relative, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const reportArgument = process.argv.find((argument) => argument.startsWith('--report='))
const freshAfterArgument = process.argv.find((argument) => argument.startsWith('--fresh-after='))
if (!reportArgument?.slice('--report='.length).trim()) {
  throw new Error('--report must point to a JSON file')
}
const reportPath = resolve(root, reportArgument.slice('--report='.length))
const freshAfter = freshAfterArgument
  ? Number(freshAfterArgument.slice('--fresh-after='.length))
  : undefined
if (freshAfter !== undefined && !Number.isFinite(freshAfter)) {
  throw new Error('--fresh-after must be a Unix epoch in milliseconds')
}

const bundleRoot = join(root, 'src-tauri', 'target', 'release', 'bundle')
const artifacts = []
for (const path of await walkArtifacts(bundleRoot)) {
  const metadata = await lstat(path)
  if (freshAfter !== undefined && metadata.mtimeMs < freshAfter) continue
  artifacts.push({
    path: relative(root, path).replaceAll('\\', '/'),
    bytes: await pathSize(path)
  })
}
if (!artifacts.length) throw new Error(`No bundle artifacts found under ${bundleRoot}`)

const executableName = process.platform === 'win32' ? 'mootool-next-tauri.exe' : 'mootool-next-tauri'
const executable = join(root, 'src-tauri', 'target', 'release', executableName)
const report = {
  schemaVersion: 1,
  recordedAt: new Date().toISOString(),
  platform: process.platform,
  architecture: process.arch,
  executable: existsSync(executable)
    ? { path: relative(root, executable).replaceAll('\\', '/'), bytes: (await lstat(executable)).size }
    : undefined,
  artifacts
}
await mkdir(dirname(reportPath), { recursive: true })
await writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
process.stdout.write(`Bundle size report: ${reportPath}\n`)

async function walkArtifacts(directory) {
  const output = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory() && entry.name.toLowerCase().endsWith('.app')) output.push(path)
    else if (entry.isDirectory()) output.push(...await walkArtifacts(path))
    else if (entry.isFile() && isBundleArtifact(entry.name)) output.push(path)
  }
  return output
}

function isBundleArtifact(name) {
  const lower = name.toLowerCase()
  return lower.endsWith('.dmg')
    || lower.endsWith('.app.tar.gz')
    || lower.endsWith('.appimage')
    || lower.endsWith('.deb')
    || lower.endsWith('.rpm')
    || lower.endsWith('.msi')
    || lower.endsWith('-setup.exe')
    || lower.endsWith('.sig')
}

async function pathSize(path) {
  const metadata = await lstat(path)
  if (!metadata.isDirectory()) return metadata.size
  let bytes = metadata.size
  for (const entry of await readdir(path)) bytes += await pathSize(join(path, entry))
  return bytes
}
