import { readdir, access } from 'node:fs/promises'
import { resolve, join } from 'node:path'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'

const require = createRequire(import.meta.url)
let executable
if (process.platform === 'darwin') {
  const directory = resolve('dist', process.arch === 'arm64' ? 'mac-arm64' : 'mac')
  const bundle = (await readdir(directory)).find((name) => name.endsWith('.app'))
  if (!bundle) throw new Error(`No app bundle in ${directory}`)
  executable = join(directory, bundle, 'Contents', 'MacOS', 'MooTool Next Electron')
} else if (process.platform === 'win32') {
  executable = resolve('dist/win-unpacked/MooTool Next Electron.exe')
} else {
  executable = resolve('dist/linux-unpacked/mootool-next')
}
await access(executable)
const result = spawnSync(process.execPath, [require.resolve('@playwright/test/cli'), 'test', 'tests/electron/ai-integration.spec.ts', '--trace=retain-on-failure'], {
  stdio: 'inherit', env: { ...process.env, MOOTOOL_TEST_EXECUTABLE: executable }
})
if (result.error) throw result.error
process.exitCode = result.status ?? 1
