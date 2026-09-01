import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const skipBuild = process.argv.includes('--skip-build')
const cyclesArgument = process.argv.find((argument) => argument.startsWith('--cycles='))
const cycles = cyclesArgument ? Number(cyclesArgument.slice('--cycles='.length)) : 10
if (!Number.isInteger(cycles) || cycles < 1 || cycles > 500) {
  throw new Error('--cycles must be an integer between 1 and 500')
}

if (!skipBuild) {
  const build = spawnSync('npm', ['run', 'build:desktop'], {
    cwd: root,
    stdio: 'inherit',
    shell: process.platform === 'win32'
  })
  if (build.status !== 0) process.exit(build.status ?? 1)
}

const executableName = process.platform === 'win32' ? 'mootool-next-tauri.exe' : 'mootool-next-tauri'
const executable = resolve(root, 'src-tauri', 'target', 'release', executableName)
if (!existsSync(executable)) throw new Error(`Native acceptance binary not found: ${executable}`)

const temporaryRoot = await mkdtemp(join(tmpdir(), 'mootool-next-tauri-native-'))
const resultPath = join(temporaryRoot, 'acceptance.json')
const dataRoot = join(temporaryRoot, 'app-data')
let child

try {
  child = spawn(executable, [], {
    cwd: root,
    env: {
      ...process.env,
      MOOTOOL_NATIVE_ACCEPTANCE_RESULT: resultPath,
      MOOTOOL_NATIVE_ACCEPTANCE_DATA: dataRoot,
      MOOTOOL_NATIVE_ACCEPTANCE_CYCLES: String(cycles)
    },
    stdio: ['ignore', 'pipe', 'pipe']
  })
  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (chunk) => { stdout += String(chunk) })
  child.stderr.on('data', (chunk) => { stderr += String(chunk) })

  const timeoutMs = 240_000
  let timeout
  const exit = new Promise((resolveExit) => child.once('exit', (code, signal) => resolveExit({ code, signal })))
  const timedOut = new Promise((resolveTimeout) => {
    timeout = setTimeout(() => resolveTimeout(null), timeoutMs)
  })
  const outcome = await Promise.race([exit, timedOut])
  clearTimeout(timeout)
  if (!outcome) {
    terminate(child)
    throw new Error(`Native acceptance timed out after ${timeoutMs / 1_000}s\n${stderr}`)
  }

  let report
  try {
    report = JSON.parse(await readFile(resultPath, 'utf8'))
  } catch (error) {
    throw new Error(`Native acceptance did not produce a readable report (code ${outcome.code}, signal ${outcome.signal})\n${stderr}\n${stdout}\n${error}`)
  }
  if (outcome.code !== 0 || !report.passed) {
    const failures = Array.isArray(report.failures) ? report.failures.join('\n- ') : 'unknown failure'
    throw new Error(`Native acceptance failed (code ${outcome.code}, signal ${outcome.signal})\n- ${failures}\n${stderr}`)
  }

  const passedTools = report.tools.filter((tool) => tool.passed).map((tool) => tool.toolId)
  process.stdout.write([
    `Native ${report.platform} ${report.architecture} acceptance passed in ${report.durationMs}ms.`,
    `${passedTools.length}/${report.tools.length} product tools: ${passedTools.join(', ')}`,
    `Isolation: ${report.isolation.detail}`,
    `Stress: ${report.stress.detail}`,
    `Acceptance data was isolated under ${temporaryRoot} and removed.`
  ].join('\n') + '\n')
} finally {
  if (child && child.exitCode === null && child.signalCode === null) terminate(child)
  await rm(temporaryRoot, { recursive: true, force: true })
}

function terminate(processHandle) {
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/PID', String(processHandle.pid), '/T', '/F'], { stdio: 'ignore' })
  } else {
    processHandle.kill('SIGTERM')
  }
}
