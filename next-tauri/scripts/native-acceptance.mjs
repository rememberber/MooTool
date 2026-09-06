import { spawn, spawnSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import { mkdir, mkdtemp, readFile, rm, stat, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const skipBuild = process.argv.includes('--skip-build')
const cyclesArgument = process.argv.find((argument) => argument.startsWith('--cycles='))
const cycles = cyclesArgument ? Number(cyclesArgument.slice('--cycles='.length)) : 10
const reportArgument = process.argv.find((argument) => argument.startsWith('--report='))
const preservedReportPath = reportArgument
  ? resolve(root, reportArgument.slice('--report='.length))
  : undefined
if (!Number.isInteger(cycles) || cycles < 1 || cycles > 500) {
  throw new Error('--cycles must be an integer between 1 and 500')
}
if (reportArgument && !reportArgument.slice('--report='.length).trim()) {
  throw new Error('--report must point to a JSON file')
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
const jsonPerformancePath = join(temporaryRoot, 'json-performance.json')
const dataRoot = join(temporaryRoot, 'app-data')
let child

try {
  const vitestCli = resolve(root, 'node_modules', 'vitest', 'vitest.mjs')
  const jsonPerformance = spawnSync(process.execPath, [vitestCli, 'run', 'scripts/json-performance.test.ts'], {
    cwd: root,
    env: { ...process.env, MOOTOOL_JSON_PERFORMANCE_REPORT: jsonPerformancePath },
    stdio: 'inherit'
  })
  if (jsonPerformance.status !== 0) {
    throw new Error(`10 MiB JSON performance benchmark failed with exit code ${jsonPerformance.status ?? 1}`)
  }

  const spawnEpochMs = Date.now()
  child = spawn(executable, [], {
    cwd: root,
    env: {
      ...process.env,
      MOOTOOL_NATIVE_ACCEPTANCE_RESULT: resultPath,
      MOOTOOL_NATIVE_ACCEPTANCE_DATA: dataRoot,
      MOOTOOL_NATIVE_ACCEPTANCE_CYCLES: String(cycles),
      MOOTOOL_NATIVE_ACCEPTANCE_SPAWN_EPOCH_MS: String(spawnEpochMs)
    },
    stdio: ['ignore', 'pipe', 'pipe']
  })
  let stdout = ''
  let stderr = ''
  child.stdout.on('data', (chunk) => { stdout += String(chunk) })
  child.stderr.on('data', (chunk) => {
    stderr += String(chunk)
    process.stderr.write(chunk)
  })

  // Xvfb software rendering and serial WebView2 startup on hosted runners are
  // substantially slower than a hardware-backed desktop. Keep the full stress
  // count and scale the deadline instead of weakening acceptance.
  const timeoutBaseMs = 600_000
  const timeoutPerCycleMs = process.platform === 'linux' ? 4_000 : 1_000
  const timeoutMs = timeoutBaseMs + cycles * timeoutPerCycleMs
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

  report.performance.json10Mib = JSON.parse(await readFile(jsonPerformancePath, 'utf8'))

  report.artifact = {
    executable: executableName,
    executableBytes: (await stat(executable)).size
  }
  report.recordedAt = new Date().toISOString()
  if (preservedReportPath) {
    await mkdir(dirname(preservedReportPath), { recursive: true })
    await writeFile(preservedReportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
  }

  const passedTools = report.tools.filter((tool) => tool.passed).map((tool) => tool.toolId)
  const performance = report.performance
  process.stdout.write([
    `Native ${report.platform} ${report.architecture} acceptance passed in ${report.durationMs}ms.`,
    `${passedTools.length}/${report.tools.length} product tools: ${passedTools.join(', ')}`,
    `Isolation: ${report.isolation.detail}`,
    `Stress: ${report.stress.detail}`,
    `Performance: first tool ${performance.acceptanceStartToFirstToolReadyMs}ms; median open ${performance.medianToolOpenMs}ms; median detach/dock ${performance.medianDetachDockMs}ms.`,
    `JSON: 10 MiB validate ${performance.json10Mib.validationMs}ms; format ${performance.json10Mib.formatMs}ms.`,
    `Memory: idle ${formatMiB(performance.memory.idleBytes)} MiB; 1 tool ${formatMiB(performance.memory.oneToolBytes)} MiB; 10 tools ${formatMiB(performance.memory.tenToolsBytes)} MiB; 25 tools ${formatMiB(performance.memory.allToolsBytes)} MiB.`,
    `Data: ${performance.quickNote.records} Quick Notes listed in ${performance.quickNote.listMs}ms; 100 MiB SHA-256 in ${performance.digest100MibMs}ms.`,
    ...(preservedReportPath ? [`Report: ${preservedReportPath}`] : []),
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

function formatMiB(bytes) {
  return (Number(bytes) / 1024 / 1024).toFixed(1)
}
