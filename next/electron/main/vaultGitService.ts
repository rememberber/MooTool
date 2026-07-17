import { execFile } from 'node:child_process'
import { access, mkdir, realpath, writeFile } from 'node:fs/promises'
import { join, resolve } from 'node:path'
import type {
  VaultGitActionInput,
  VaultGitActionResult,
  VaultGitChange,
  VaultGitCommit,
  VaultGitDiffInput,
  VaultGitStatus
} from '../../src/shared/contracts/vaultGit'

type GitCredentials = {
  username?: string
  token?: string
  askPassPath?: string
}

type CommandResult = {
  stdout: string
  stderr: string
  exitCode: number
}

const defaultGitignore = `.DS_Store
.idea/
.vscode/
*.tmp
.migrated-from-db
`

export class VaultGitService {
  constructor(private readonly rootDirectory: string, private readonly credentials: GitCredentials = {}) {}

  async status(): Promise<VaultGitStatus> {
    await mkdir(this.rootDirectory, { recursive: true })
    const available = (await this.run(['--version'], { cwd: undefined })).exitCode === 0
    if (!available) return emptyStatus(false)
    const topLevelResult = await this.run(['rev-parse', '--show-toplevel'])
    if (topLevelResult.exitCode !== 0) return emptyStatus(true)
    const [rootDirectory, topLevel] = await Promise.all([
      realpath(this.rootDirectory),
      realpath(topLevelResult.stdout.trim())
    ])
    if (!sameFilesystemPath(rootDirectory, topLevel)) return emptyStatus(true)

    const porcelain = await this.run(['status', '--porcelain=v1', '-z', '--branch', '--untracked-files=all'])
    const { branchLine, changes } = parsePorcelainStatus(porcelain.stdout)
    const branch = parseBranch(branchLine)
    const ahead = Number(/ahead (\d+)/.exec(branchLine)?.[1] ?? 0)
    const behind = Number(/behind (\d+)/.exec(branchLine)?.[1] ?? 0)
    const remoteResult = await this.run(['remote', 'get-url', 'origin'])
    const [mergeResult, rebaseMergePath, rebaseApplyPath] = await Promise.all([
      this.run(['rev-parse', '-q', '--verify', 'MERGE_HEAD']),
      this.run(['rev-parse', '--git-path', 'rebase-merge']),
      this.run(['rev-parse', '--git-path', 'rebase-apply'])
    ])
    const rebasePaths = [rebaseMergePath, rebaseApplyPath]
      .filter((result) => result.exitCode === 0 && result.stdout.trim())
      .map((result) => resolve(this.rootDirectory, result.stdout.trim()))
    const rebaseInProgress = (await Promise.all(rebasePaths.map(pathExists))).some(Boolean)
    const operation = rebaseInProgress ? 'rebase' : mergeResult.exitCode === 0 ? 'merge' : 'none'
    return {
      available: true,
      repository: true,
      branch,
      remote: remoteResult.exitCode === 0 ? remoteResult.stdout.trim() : '',
      ahead,
      behind,
      changes,
      conflicts: changes.filter((change) => change.conflict).length,
      merging: operation !== 'none',
      operation
    }
  }

  async history(limit = 50): Promise<VaultGitCommit[]> {
    const status = await this.status()
    if (!status.repository) return []
    const result = await this.run(['log', `--max-count=${Math.min(100, Math.max(1, limit))}`, '--pretty=format:%H%x1f%h%x1f%an%x1f%aI%x1f%s'])
    if (result.exitCode !== 0) return []
    return result.stdout.split(/\r?\n/).filter(Boolean).map((line) => {
      const [hash, shortHash, author, date, message] = line.split('\x1f')
      return { hash, shortHash, author, date, message }
    })
  }

  async diff(input: VaultGitDiffInput): Promise<string> {
    if (!(await this.status()).repository) throw new Error('Git repository is not initialized in the Vault root')
    const path = normalizeGitPath(input.path)
    if (input.commit) {
      if (!/^[0-9a-f]{7,40}$/i.test(input.commit)) throw new Error('Invalid Git commit')
      return this.requireSuccess(['show', '--format=fuller', '--stat', '--patch', input.commit, ...(path ? ['--', path] : [])])
    }
    const args = path ? ['--', path] : []
    const [working, staged] = await Promise.all([
      this.requireSuccess(['diff', '--no-ext-diff', ...args]),
      this.requireSuccess(['diff', '--cached', '--no-ext-diff', ...args])
    ])
    return [staged, working].filter(Boolean).join('\n\n')
  }

  async action(input: VaultGitActionInput): Promise<VaultGitActionResult> {
    await mkdir(this.rootDirectory, { recursive: true })
    if (input.action !== 'init' && !(await this.status()).repository) {
      return { success: false, message: 'Git repository is not initialized in the Vault root' }
    }
    switch (input.action) {
      case 'init':
        return this.initialize()
      case 'configure-remote':
        return this.configureRemote(input.remote)
      case 'commit':
        return this.commit(input.message)
      case 'fetch':
        return this.result(['fetch', '--prune', 'origin'], true)
      case 'pull':
        return this.pull()
      case 'push':
        return this.result(['push', '-u', 'origin', 'HEAD'], true)
      case 'discard':
        return this.discard(input.path)
      case 'abort-merge':
        return this.abortMerge()
      case 'resolve-conflict':
        return this.resolveConflict(input.path, input.strategy)
      case 'continue-operation':
        return this.continueOperation()
    }
  }

  async automaticCheckpoint(message: string): Promise<VaultGitActionResult> {
    let status = await this.status()
    if (!status.available) return { success: true, message: 'Git is unavailable; checkpoint skipped' }
    if (!status.repository) {
      const initialized = await this.initialize()
      if (!initialized.success) return initialized
      status = await this.status()
    }
    if (status.merging || status.conflicts > 0) return { success: true, message: 'Merge/rebase in progress; checkpoint skipped' }
    let result: VaultGitActionResult = { success: true, message: 'No changes to commit' }
    if (status.changes.length > 0) {
      result = await this.commit(message)
      if (!result.success) return result
    }
    if (status.remote && (status.changes.length > 0 || status.ahead > 0)) return this.result(['push', '-u', 'origin', 'HEAD'], true)
    return result
  }

  private async initialize(): Promise<VaultGitActionResult> {
    if ((await this.status()).repository) return { success: true, message: 'Git repository is already initialized' }
    const initialized = await this.run(['init'])
    if (initialized.exitCode !== 0) return commandFailure(initialized)
    try {
      await writeFile(join(this.rootDirectory, '.gitignore'), defaultGitignore, { encoding: 'utf8', flag: 'wx' })
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'EEXIST') throw error
    }
    const status = await this.status()
    if (!status.changes.length) return { success: true, message: initialized.stdout.trim() || 'Done' }
    return this.commit('Initial MooTool Vault setup')
  }

  private async discard(value: string | undefined): Promise<VaultGitActionResult> {
    const path = normalizeGitPath(value)
    if (!path) throw new Error('Git path is required')
    const currentStatus = await this.status()
    const change = currentStatus.changes.find((item) => item.path === path)
    if (!change) return { success: true, message: 'No changes to discard' }
    if (change.status === '??') return this.result(['clean', '-f', '--', path])

    const paths = [change.originalPath, path].filter((item): item is string => Boolean(item))
    let restored = await this.run(['restore', '--staged', '--worktree', '--source=HEAD', '--', ...paths])
    if (restored.exitCode !== 0) {
      await this.run(['reset', 'HEAD', '--', ...paths])
      restored = await this.run(['checkout', 'HEAD', '--', ...paths])
    }
    if (restored.exitCode !== 0 && change.status.includes('A')) {
      const cleaned = await this.run(['clean', '-f', '--', path])
      return cleaned.exitCode === 0 ? { success: true, message: cleaned.stdout.trim() || 'Done' } : commandFailure(cleaned)
    }
    return restored.exitCode === 0 ? { success: true, message: restored.stdout.trim() || 'Done' } : commandFailure(restored)
  }

  private async abortMerge(): Promise<VaultGitActionResult> {
    const merge = await this.run(['merge', '--abort'])
    if (merge.exitCode === 0) return { success: true, message: merge.stdout.trim() || 'Done' }
    const rebase = await this.run(['rebase', '--abort'])
    return rebase.exitCode === 0 ? { success: true, message: rebase.stdout.trim() || 'Done' } : commandFailure(rebase)
  }

  private async pull(): Promise<VaultGitActionResult> {
    const status = await this.status()
    if (status.merging) return { success: false, message: 'Finish or abort the current merge/rebase before pulling' }
    return this.result(['pull', '--no-rebase', 'origin'], true)
  }

  private async continueOperation(): Promise<VaultGitActionResult> {
    const status = await this.status()
    if (!status.repository || status.operation === 'none') return { success: false, message: 'No merge or rebase is in progress' }
    if (status.conflicts > 0) return { success: false, message: 'Resolve all conflicts before continuing' }
    await this.ensureIdentity()
    return status.operation === 'rebase'
      ? this.result(['-c', 'core.editor=true', '-c', 'commit.gpgsign=false', 'rebase', '--continue'])
      : this.result(['-c', 'commit.gpgsign=false', 'commit', '--no-edit'])
  }

  private async resolveConflict(value: string | undefined, strategy: 'ours' | 'theirs' | undefined): Promise<VaultGitActionResult> {
    const path = normalizeGitPath(value)
    if (!path) throw new Error('Git path is required')
    if (strategy !== 'ours' && strategy !== 'theirs') throw new Error('Invalid conflict strategy')
    const currentStatus = await this.status()
    if (!currentStatus.changes.some((change) => change.path === path && change.conflict)) {
      throw new Error('Git path is not conflicted')
    }
    const checkout = await this.run(['checkout', `--${strategy}`, '--', path])
    if (checkout.exitCode !== 0) return commandFailure(checkout)
    return this.result(['add', '--', path])
  }

  private async configureRemote(value: string | undefined): Promise<VaultGitActionResult> {
    const remote = normalizeRemote(value)
    const exists = (await this.run(['remote', 'get-url', 'origin'])).exitCode === 0
    if (!remote) return exists ? this.result(['remote', 'remove', 'origin']) : { success: true, message: 'Remote is already removed' }
    return this.result(exists ? ['remote', 'set-url', 'origin', remote] : ['remote', 'add', 'origin', remote])
  }

  private async commit(message: string | undefined): Promise<VaultGitActionResult> {
    const normalizedMessage = message?.trim().slice(0, 300)
    if (!normalizedMessage) throw new Error('Commit message is required')
    const currentStatus = await this.status()
    if (!currentStatus.repository) throw new Error('Git repository is not initialized')
    if (currentStatus.merging || currentStatus.conflicts > 0) return { success: false, message: 'Finish or abort the current merge/rebase before creating a checkpoint' }
    if (currentStatus.changes.length === 0) return { success: true, message: 'No changes to commit' }
    await this.ensureIdentity()
    const add = await this.run(['add', '--all'])
    if (add.exitCode !== 0) return commandFailure(add)
    return this.result(['commit', '-m', normalizedMessage])
  }

  private async ensureIdentity(): Promise<void> {
    if ((await this.run(['config', '--get', 'user.name'])).exitCode !== 0) {
      await this.requireSuccess(['config', 'user.name', this.credentials.username?.trim() || 'MooTool'])
    }
    if ((await this.run(['config', '--get', 'user.email'])).exitCode !== 0) {
      await this.requireSuccess(['config', 'user.email', 'mootool@local'])
    }
  }

  private async result(args: string[], authenticated = false): Promise<VaultGitActionResult> {
    const result = await this.run(args, { authenticated })
    return result.exitCode === 0
      ? { success: true, message: result.stdout.trim() || result.stderr.trim() || 'Done' }
      : commandFailure(result)
  }

  private async requireSuccess(args: string[]): Promise<string> {
    const result = await this.run(args)
    if (result.exitCode !== 0) throw new Error(result.stderr.trim() || result.stdout.trim() || 'Git command failed')
    return result.stdout.trim()
  }

  private run(args: string[], options: { authenticated?: boolean; cwd?: string } = {}): Promise<CommandResult> {
    const env: NodeJS.ProcessEnv = { ...process.env, GIT_TERMINAL_PROMPT: '0' }
    if (options.authenticated && this.credentials.askPassPath && this.credentials.token) {
      env.GIT_ASKPASS = this.credentials.askPassPath
      env.MOOTOOL_GIT_USERNAME = this.credentials.username ?? ''
      env.MOOTOOL_GIT_TOKEN = this.credentials.token
    }
    return new Promise((resolve) => {
      execFile('git', args, {
        cwd: options.cwd === undefined ? this.rootDirectory : options.cwd,
        env,
        timeout: 60_000,
        maxBuffer: 10 * 1024 * 1024,
        windowsHide: true
      }, (error, stdout, stderr) => {
        const exitCode = typeof (error as NodeJS.ErrnoException & { code?: number } | null)?.code === 'number'
          ? (error as NodeJS.ErrnoException & { code: number }).code
          : error ? 1 : 0
        resolve({ stdout, stderr, exitCode })
      })
    })
  }
}

function emptyStatus(available: boolean): VaultGitStatus {
  return { available, repository: false, branch: '', remote: '', ahead: 0, behind: 0, changes: [], conflicts: 0, merging: false, operation: 'none' }
}

function parsePorcelainStatus(output: string): { branchLine: string; changes: VaultGitChange[] } {
  const records = output.split('\0')
  const branchLine = records.shift() ?? ''
  const changes: VaultGitChange[] = []
  for (let index = 0; index < records.length; index += 1) {
    const record = records[index]
    if (!record || record.length < 4) continue
    const status = record.slice(0, 2)
    const path = record.slice(3)
    const renamed = status.includes('R') || status.includes('C')
    const originalPath = renamed ? records[++index] : undefined
    const conflict = status.includes('U') || status === 'AA' || status === 'DD'
    changes.push({ path, originalPath: originalPath || undefined, status, conflict })
  }
  return { branchLine, changes }
}

function parseBranch(line: string): string {
  const value = line.replace(/^##\s*/, '')
  const emptyRepository = /^(?:No commits yet on|Initial commit on)\s+(.+)$/.exec(value)
  if (emptyRepository) return emptyRepository[1].trim()
  if (value.startsWith('HEAD ')) return 'HEAD'
  return value.split('...')[0].trim().split(' ')[0] || 'HEAD'
}

function normalizeGitPath(value: unknown): string | undefined {
  if (value == null || value === '') return undefined
  if (typeof value !== 'string' || value.length > 512 || value.startsWith('/') || value.includes('\0')) {
    throw new Error('Invalid Git path')
  }
  const normalized = value.replaceAll('\\', '/')
  if (normalized.split('/').some((part) => !part || part === '.' || part === '..')) throw new Error('Invalid Git path')
  return normalized
}

function normalizeRemote(value: unknown): string {
  if (typeof value !== 'string') throw new Error('Invalid Git remote')
  const remote = value.trim()
  if (!remote) return ''
  if (remote.length > 2048 || /[\r\n\0]/.test(remote) || !/^(https?:\/\/|ssh:\/\/|git:\/\/|git@|file:\/\/)/i.test(remote)) {
    throw new Error('Invalid Git remote')
  }
  return remote
}

function sameFilesystemPath(left: string, right: string): boolean {
  const normalizedLeft = resolve(left)
  const normalizedRight = resolve(right)
  return process.platform === 'win32'
    ? normalizedLeft.toLocaleLowerCase() === normalizedRight.toLocaleLowerCase()
    : normalizedLeft === normalizedRight
}

async function pathExists(path: string): Promise<boolean> {
  try {
    await access(path)
    return true
  } catch {
    return false
  }
}

function commandFailure(result: CommandResult): VaultGitActionResult {
  return { success: false, message: result.stderr.trim() || result.stdout.trim() || 'Git command failed' }
}
