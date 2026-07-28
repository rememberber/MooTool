import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const electronPackagePath = require.resolve('electron/package.json')
const installerPath = join(dirname(electronPackagePath), 'install.js')
const mirror = process.env.ELECTRON_MIRROR
  || process.env.npm_config_electron_mirror
  || 'https://npmmirror.com/mirrors/electron/'

const result = spawnSync(process.execPath, [installerPath], {
  stdio: 'inherit',
  env: {
    ...process.env,
    ELECTRON_MIRROR: mirror
  }
})

if (result.error) throw result.error
if (result.status !== 0) process.exit(result.status ?? 1)
