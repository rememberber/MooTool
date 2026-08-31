import { readdir, readFile, stat } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { basename, join, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const packageJson = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'))
const version = packageJson.version
const tag = `next-tauri-v${version}`
const startedAt = Date.now()

run('npm', ['run', 'check:release'])
run('npx', ['vitest', 'run', 'scripts/prepare-release-assets.test.ts'])

const bundleTargets = process.platform === 'darwin'
  ? ['app', 'dmg']
  : process.platform === 'win32'
    ? ['nsis']
    : ['appimage', 'deb']
run('npx', [
  'tauri',
  'build',
  '--ci',
  '--no-sign',
  '--bundles',
  bundleTargets.join(','),
  '--config',
  'src-tauri/tauri.rehearsal.conf.json'
])
run('node', ['scripts/native-smoke.mjs', '--skip-build'])

const bundleRoot = join(root, 'src-tauri', 'target', 'release', 'bundle')
const artifacts = (await walk(bundleRoot)).filter((path) => expectedArtifact(path))
const freshArtifacts = []
for (const path of artifacts) {
  if ((await stat(path)).mtimeMs >= startedAt - 5_000) freshArtifacts.push(path)
}
if (!freshArtifacts.length) {
  throw new Error(`Release rehearsal did not create a fresh ${bundleTargets.join('/')} artifact`)
}

process.stdout.write([
  `Release rehearsal passed for ${tag}.`,
  ...freshArtifacts.map((path) => `- ${path}`),
  'Updater metadata generation and promotion validation are covered by the release asset tests.',
  'No tag, GitHub Release, updater channel, or product registry was changed.'
].join('\n') + '\n')

function run(command, args) {
  const executable = process.platform === 'win32' && ['npm', 'npx'].includes(command)
    ? `${command}.cmd`
    : command
  const result = spawnSync(executable, args, { cwd: root, stdio: 'inherit' })
  if (result.status !== 0) process.exit(result.status ?? 1)
}

async function walk(directory) {
  const output = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory() && !entry.name.endsWith('.app')) output.push(...await walk(path))
    else if (entry.isFile() || entry.name.endsWith('.app')) output.push(path)
  }
  return output
}

function expectedArtifact(path) {
  const name = basename(path).toLowerCase()
  if (process.platform === 'darwin') return name.endsWith('.app') || name.endsWith('.dmg')
  if (process.platform === 'win32') return name.endsWith('-setup.exe')
  return name.endsWith('.appimage') || name.endsWith('.deb')
}
