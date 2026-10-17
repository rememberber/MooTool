import { readdir, access } from 'node:fs/promises'
import { resolve, join } from 'node:path'
import { createRequire } from 'node:module'
import { spawnSync } from 'node:child_process'
const require = createRequire(import.meta.url)
let executable
if (process.platform === 'darwin') {
  const directory = resolve('dist', process.arch === 'arm64' ? 'mac-arm64' : 'mac')
  const bundle = (await readdir(directory)).find(name => name.endsWith('.app'))
  if (!bundle) throw new Error(`No app bundle in ${directory}`)
  executable = join(directory, bundle, 'Contents/MacOS/MooTool Next Electron')
} else executable = resolve(process.platform === 'win32' ? 'dist/win-unpacked/MooTool Next Electron.exe' : 'dist/linux-unpacked/mootool-next')
await access(executable)
const specs = ['tests/electron/image-ocr.spec.ts', ...(process.platform === 'darwin' ? [] : ['tests/electron/ppocr-lifecycle.spec.ts', 'tests/electron/ocr-component.spec.ts'])]
const result = spawnSync(process.execPath, [require.resolve('@playwright/test/cli'), 'test', ...specs, '--trace=retain-on-failure'], { stdio: 'inherit', env: { ...process.env, MOOTOOL_OCR_EXECUTABLE: executable } })
if (result.error) throw result.error
process.exitCode = result.status ?? 1
