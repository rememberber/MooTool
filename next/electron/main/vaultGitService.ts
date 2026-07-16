import { execFile } from 'node:child_process'
import { mkdir } from 'node:fs/promises'
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

export class VaultGitService {
  constructor(private readonly rootDirectory: string, private readonly credentials: GitCredentials = {}) {}

  async status(): Promise<VaultGitStatus> {
    await mkdir(this.rootDirectory, { recursive: true })
    const available = (await this.run(['--version'], { cwd: undefined })).exitCode === 0
    if (!available) return emptyStatus(false)
    if ((await this.run(['rev-parse', '--is-inside-work-tree'])).exitCode !== 0) return emptyStatus(true)

    const porcelain = await this.run(['status', '--porcelain=v1', '--branch'])
    const lines = porcelain.stdout.split(/\r?\n/).filter(Boolean)
    const branchLine = lines.shift() ?? ''
    const branch = parseBranch(branchLine)
    const ahead = Number(/ahead (\d+)/.exec(branchLine)?.[1] ?? 0)
    const behind = Number(/behind (\d+)/.exec(branchLine)?.[1] ?? 0)
    const changes = lines.map(parseChangeLine).filter((change): change is VaultGitChange => change !== null)
    const remoteResult = await this.run(['remote', 'get-url', 'origin'])
    const merging = (await this.run(['rev-parse', '-q', '--verify', 'MERGE_HEAD'])).exitCode === 0
    return {
      available: true,
      repository: true,
      branch,
      remote: remoteResult.exitCode === 0 ? remoteResult.stdout.trim() : '',
      ahead,
      behind,
      changes,
      conflicts: changes.filter((change) => change.conflict).length,
      merging
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
    const path = normalizeGitPath(input.path)
    if (input.commit) {
      if (!/^[0-9a-f]{7,40}$/i.test(input.commit)) throw new Error('Invalid Git commit')
      return this.requireSuccess(['show', '--format=fuller', '--stat', '--patch', input.commit, ...(path ? ['--', path] : [])])
    }
    const args = path ? ['--', path] : []
    const working = await this.requireSuccess(['diff', '--no-ext-diff', ...args])
    const staged = await this.requireSuccess(['diff', '--cached', '--no-ext-diff', ...args])
    return [staged, working].filter(Boolean).join('\n\n')
  }

  async action(input: VaultGitActionInput): Promise<VaultGitActionResult> {
    await mkdir(this.rootDirectory, { recursive: true })
    switch (input.action) {
      case 'init':
        return this.result(['init'])
      case 'configure-remote':
        return this.configureRemote(input.remote)
      case 'commit':
        return this.commit(input.message)
      case 'fetch':
        return this.result(['fetch', '--prune', 'origin'], true)
      case 'pull':
        return this.result(['pull', '--no-rebase', 'origin'], true)
      case 'push':
        return this.result(['push', '-u', 'origin', 'HEAD'], true)
      case 'discard':
        return this.discard(input.path)
      case 'abort-merge':
        return this.abortMerge()
      case 'resolve-conflict':
        return this.resolveConflict(input.path, input.strategy)
    }
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
    return rebase.exitCode === 0 ? { success: true, message: rebase.stdout.trim() || 'Done' } : commandFailure(merge)
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
    return this.result(exists ? ['remote', 'set-url', 'origin', remote] : ['remote', 'add', 'origin', remote])
  }

  private async commit(message: string | undefined): Promise<VaultGitActionResult> {
    const normalizedMessage = message?.trim().slice(0, 300)
    if (!normalizedMessage) throw new Error('Commit message is required')
    const currentStatus = await this.status()
    if (!currentStatus.repository) throw new Error('Git repository is not initialized')
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
  return { available, repository: false, branch: '', remote: '', ahead: 0, behind: 0, changes: [], conflicts: 0, merging: false }
}

function parseChangeLine(line: string): VaultGitChange | null {
  if (line.length < 4) return null
  const status = line.slice(0, 2)
  const rawPath = line.slice(3).trim()
  const [originalPath, path] = rawPath.includes(' -> ') ? rawPath.split(' -> ', 2) : [undefined, rawPath]
  const conflict = status.includes('U') || status === 'AA' || status === 'DD'
  return { path: unquoteGitPath(path), originalPath: originalPath ? unquoteGitPath(originalPath) : undefined, status, conflict }
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
  if (remote.length > 2048 || /[\r\n\0]/.test(remote) || !/^(https?:\/\/|ssh:\/\/|git:\/\/|git@|file:\/\/)/i.test(remote)) {
    throw new Error('Invalid Git remote')
  }
  return remote
}

function unquoteGitPath(value: string): string {
  if (!value.startsWith('"')) return value
  try {
    return JSON.parse(value) as string
  } catch {
    return value
  }
}

function commandFailure(result: CommandResult): VaultGitActionResult {
  return { success: false, message: result.stderr.trim() || result.stdout.trim() || 'Git command failed' }
}
