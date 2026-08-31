import { existsSync } from 'node:fs'
import { spawn, spawnSync } from 'node:child_process'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const skipBuild = process.argv.includes('--skip-build')
if (!skipBuild) {
  const build = spawnSync('npm', ['run', 'build:desktop'], { cwd: root, stdio: 'inherit', shell: process.platform === 'win32' })
  if (build.status !== 0) process.exit(build.status ?? 1)
}

const executableName = process.platform === 'win32' ? 'mootool-next-tauri.exe' : 'mootool-next-tauri'
const executable = resolve(root, 'src-tauri', 'target', 'release', executableName)
if (!existsSync(executable)) throw new Error(`Native smoke binary not found: ${executable}`)

const child = spawn(executable, [], { cwd: root, stdio: ['ignore', 'pipe', 'pipe'] })
let stderr = ''
child.stderr.on('data', (chunk) => { stderr += String(chunk) })
const exit = new Promise((resolveExit) => child.once('exit', (code, signal) => resolveExit({ code, signal })))
const timeout = new Promise((resolveTimeout) => setTimeout(() => resolveTimeout(null), 8_000))
const early = await Promise.race([exit, timeout])

if (early) {
  throw new Error(`Native app exited during startup (code ${early.code}, signal ${early.signal})\n${stderr}`)
}

if (process.platform === 'win32') spawnSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], { stdio: 'ignore' })
else child.kill('SIGTERM')
await Promise.race([exit, new Promise((resolveWait) => setTimeout(resolveWait, 5_000))])
process.stdout.write(`Native startup smoke passed: ${executable}\n`)
