import { spawn, spawnSync } from 'node:child_process'
import { mkdtemp, readdir, rmdir } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

if (process.platform !== 'darwin') throw new Error('DMG verification is available only on macOS')
const root = resolve(import.meta.dirname, '..')
const dmgArgument = process.argv.find((argument) => argument.startsWith('--dmg='))
if (!dmgArgument?.slice('--dmg='.length).trim()) throw new Error('--dmg must point to a DMG file')
const dmgPath = resolve(root, dmgArgument.slice('--dmg='.length))
const mountPath = await mkdtemp(join(tmpdir(), 'mootool-next-tauri-dmg-'))
let attached = false
let child
let exit

try {
  const attach = spawnSync('hdiutil', ['attach', '-nobrowse', '-readonly', '-mountpoint', mountPath, dmgPath], {
    encoding: 'utf8'
  })
  if (attach.status !== 0) throw new Error(`Could not mount DMG: ${attach.stderr}`)
  attached = true

  const appName = (await readdir(mountPath)).find((name) => name.endsWith('.app'))
  if (!appName) throw new Error('Mounted DMG does not contain an application bundle')
  const appPath = join(mountPath, appName)
  const executableLookup = spawnSync(
    '/usr/libexec/PlistBuddy',
    ['-c', 'Print :CFBundleExecutable', join(appPath, 'Contents', 'Info.plist')],
    { encoding: 'utf8' }
  )
  if (executableLookup.status !== 0) throw new Error(`Could not resolve DMG executable: ${executableLookup.stderr}`)
  const executable = join(appPath, 'Contents', 'MacOS', executableLookup.stdout.trim())
  child = spawn(executable, [], { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] })
  let stderr = ''
  child.stderr.on('data', (chunk) => { stderr += String(chunk) })
  exit = new Promise((resolveExit) => child.once('exit', (code, signal) => resolveExit({ code, signal })))
  const early = await Promise.race([
    exit,
    new Promise((resolveWait) => setTimeout(() => resolveWait(null), 8_000))
  ])
  if (early) {
    throw new Error(`Mounted DMG application exited during first launch (code ${early.code}, signal ${early.signal})\n${stderr}`)
  }
  process.stdout.write(`Mounted DMG first-launch smoke passed: ${dmgPath}\n`)
} finally {
  if (child?.exitCode === null && child?.signalCode === null) {
    child.kill('SIGTERM')
    await Promise.race([exit, new Promise((resolveWait) => setTimeout(resolveWait, 5_000))])
    if (child.exitCode === null && child.signalCode === null) child.kill('SIGKILL')
  }
  if (attached) {
    const detach = spawnSync('hdiutil', ['detach', mountPath, '-quiet'], { stdio: 'ignore' })
    if (detach.status !== 0) throw new Error(`Could not detach DMG mount at ${mountPath}`)
  }
  await rmdir(mountPath)
}
