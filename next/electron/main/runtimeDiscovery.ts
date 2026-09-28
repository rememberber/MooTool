import { execFile } from 'node:child_process'
import { constants } from 'node:fs'
import { access, stat } from 'node:fs/promises'
import { homedir } from 'node:os'
import { delimiter, isAbsolute, join } from 'node:path'
import type { RuntimeId, RuntimeStatus } from '../../src/shared/contracts/app'

const allowed = ['PATH', 'HOME', 'USERPROFILE', 'TMPDIR', 'TMP', 'TEMP', 'SystemRoot', 'WINDIR', 'LANG', 'LC_ALL', 'JAVA_HOME', 'GROOVY_HOME']
let environment: Promise<NodeJS.ProcessEnv> | undefined

// Read shell initialization once; an explicit detection refresh retries after PATH changes.
export function getRuntimeEnvironment(refresh = false): Promise<NodeJS.ProcessEnv> {
  if (refresh || !environment) {
    environment = readShellEnvironment().then((shell) => buildRuntimeEnvironment(process.env, shell))
  }
  return environment
}

export function buildRuntimeEnvironment(base: NodeJS.ProcessEnv, shell: NodeJS.ProcessEnv = {}): NodeJS.ProcessEnv {
  const merged = { ...base, ...shell }
  const env = Object.fromEntries(allowed.flatMap((key) => merged[key] === undefined ? [] : [[key, merged[key]]]))
  const home = merged.HOME || merged.USERPROFILE || homedir()
  const fallback = process.platform === 'win32'
    ? [base.ProgramFiles && join(base.ProgramFiles, 'nodejs'), base.LOCALAPPDATA && join(base.LOCALAPPDATA, 'Programs', 'nodejs')]
    : ['/opt/homebrew/bin', '/usr/local/bin', join(home, '.local', 'bin'), join(home, '.volta', 'bin'), join(home, '.asdf', 'shims'), join(home, '.local', 'share', 'mise', 'shims'), '/usr/bin', '/bin', '/usr/sbin', '/sbin']
  const paths = [shell.PATH, base.PATH || base.Path, merged.JAVA_HOME && join(merged.JAVA_HOME, 'bin'), merged.GROOVY_HOME && join(merged.GROOVY_HOME, 'bin'), ...fallback]
  env.PATH = [...new Set(paths.flatMap((value) => value?.split(delimiter) ?? []).filter((value) => value && isAbsolute(value)))].join(delimiter)
  return env
}

function readShellEnvironment(): Promise<NodeJS.ProcessEnv> {
  if (process.platform === 'win32') return Promise.resolve({})
  const shell = process.env.SHELL || (process.platform === 'darwin' ? '/bin/zsh' : '/bin/bash')
  return new Promise((resolve) => {
    // Fixed command only: configured executable paths and user code never enter a shell.
    execFile(shell, ['-ilc', "printf '\\0MOOTOOL_ENV\\0'; /usr/bin/env -0"], {
      timeout: 3000, maxBuffer: 1024 * 1024, cwd: homedir()
    }, (error, stdout) => {
      if (error) return resolve({})
      const marker = '\0MOOTOOL_ENV\0'
      const start = stdout.indexOf(marker)
      if (start < 0) return resolve({})
      const entries = stdout.slice(start + marker.length).split('\0').flatMap((entry) => {
        const index = entry.indexOf('=')
        return index > 0 && allowed.includes(entry.slice(0, index)) ? [[entry.slice(0, index), entry.slice(index + 1)]] : []
      })
      resolve(Object.fromEntries(entries))
    })
  })
}

export async function resolveRuntimeCommand(id: RuntimeId, configured: string | undefined, env: NodeJS.ProcessEnv): Promise<string> {
  const command = configured?.trim() || (id === 'python' && process.platform !== 'win32' ? 'python3' : id)
  if (isAbsolute(command) || command.includes('/') || command.includes('\\')) return command
  const names = process.platform === 'win32' && !command.toLowerCase().endsWith('.exe') ? [`${command}.exe`, command] : [command]
  for (const directory of (env.PATH || '').split(delimiter).filter(Boolean)) {
    for (const name of names) {
      const candidate = join(directory, name)
      try {
        await access(candidate, process.platform === 'win32' ? constants.F_OK : constants.X_OK)
        if ((await stat(candidate)).isFile()) return candidate
      } catch { /* Try the next PATH entry. */ }
    }
  }
  return command
}

export async function detectRuntime(id: RuntimeId, configured: string | undefined, env: NodeJS.ProcessEnv): Promise<RuntimeStatus> {
  const command = await resolveRuntimeCommand(id, configured, env)
  return new Promise((resolve) => {
    execFile(command, [id === 'java' ? '-version' : '--version'], { env, timeout: 3000, windowsHide: true }, (error, stdout, stderr) => {
      resolve({ id, available: !error, command, version: !error ? `${stdout}\n${stderr}`.trim().split(/\r?\n/)[0] ?? '' : '' })
    })
  })
}
