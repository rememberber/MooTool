import { execFile } from 'node:child_process'
import type { Stats } from 'node:fs'
import { access, lstat, mkdir, readFile, realpath, rename, unlink, writeFile } from 'node:fs/promises'
import { basename, join, resolve } from 'node:path'
import type {
  VaultGitActionInput,
  VaultGitActionResult,
  VaultGitChange,
  VaultGitCommit,
  VaultGitDiffFile,
  VaultGitDiffInput,
  VaultGitDiffPreview,
  VaultGitDiffResult,
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

type GitChangedPath = {
  path: string
  originalPath?: string
  status: string
}

type ContentPreview = {
  text: string
  state: 'text' | 'missing' | VaultGitDiffPreview
}

const maxDiffPreviewBytes = 512 * 1024
const staleIndexLockMilliseconds = 5 * 60 * 1_000
const indexLockRetryDelayMilliseconds = 250
const indexLockStabilityDelayMilliseconds = 100
const repositoryOperationQueues = new Map<string, Promise<void>>()

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

  async diff(input: VaultGitDiffInput): Promise<VaultGitDiffResult> {
    const status = await this.status()
    if (!status.repository) throw new Error('Git repository is not initialized in the Vault root')
    const path = normalizeGitPath(input.path)
    if (input.commit) {
      if (!/^[0-9a-f]{7,40}$/i.test(input.commit)) throw new Error('Invalid Git commit')
      return { files: await this.commitDiffFiles(input.commit, path) }
    }
    const changes = path ? status.changes.filter((change) => change.path === path) : status.changes
    const files: VaultGitDiffFile[] = []
    for (const change of changes) {
      const before = await this.readBlobPreview('HEAD', change.originalPath ?? change.path)
      const after = await this.readWorkingPreview(change.path)
      files.push(buildDiffFile(change, before, after))
    }
    return { files }
  }

  private async commitDiffFiles(commit: string, path?: string): Promise<VaultGitDiffFile[]> {
    const parentResult = await this.run(['rev-parse', '--verify', `${commit}^`])
    const parent = parentResult.exitCode === 0 ? parentResult.stdout.trim() : undefined
    const changes = await this.listCommitChanges(commit, parent, path)
    const files: VaultGitDiffFile[] = []
    for (const change of changes) {
      const before = parent
        ? await this.readBlobPreview(parent, change.originalPath ?? change.path)
        : missingContent()
      const after = await this.readBlobPreview(commit, change.path)
      files.push(buildDiffFile(change, before, after))
    }
    return files
  }

  private async listCommitChanges(commit: string, parent: string | undefined, path?: string): Promise<GitChangedPath[]> {
    const pathArgs = path ? ['--', path] : []
    const result = parent
      ? await this.run(['diff', '--name-status', '-z', '-M', '-C', parent, commit, ...pathArgs])
      : await this.run(['diff-tree', '--root', '--no-commit-id', '--name-status', '-z', '-r', '-M', '-C', commit, ...pathArgs])
    if (result.exitCode !== 0) throw new Error(result.stderr.trim() || result.stdout.trim() || 'Unable to read Git commit')
    return parseNameStatus(result.stdout)
  }

  private async readBlobPreview(ref: string, path: string): Promise<ContentPreview> {
    const objectSpec = `${ref}:${path}`
    const sizeResult = await this.run(['cat-file', '-s', objectSpec])
    if (sizeResult.exitCode !== 0) return missingContent()
    const size = Number(sizeResult.stdout.trim())
    if (Number.isFinite(size) && size > maxDiffPreviewBytes) return { text: '', state: 'too-large' }
    const contentResult = await this.run(['cat-file', 'blob', objectSpec])
    if (contentResult.exitCode !== 0) return missingContent()
    return contentResult.stdout.includes('\0')
      ? { text: '', state: 'binary' }
      : { text: contentResult.stdout, state: 'text' }
  }

  private async readWorkingPreview(path: string): Promise<ContentPreview> {
    const filePath = join(this.rootDirectory, path)
    try {
      const fileStat = await lstat(filePath)
      if (!fileStat.isFile()) return { text: '', state: 'binary' }
      if (fileStat.size > maxDiffPreviewBytes) return { text: '', state: 'too-large' }
      const content = await readFile(filePath)
      return content.includes(0)
        ? { text: '', state: 'binary' }
        : { text: content.toString('utf8'), state: 'text' }
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return missingContent()
      throw error
    }
  }

  async action(input: VaultGitActionInput): Promise<VaultGitActionResult> {
    return enqueueRepositoryOperation(this.rootDirectory, () => this.performAction(input))
  }

  private async performAction(input: VaultGitActionInput): Promise<VaultGitActionResult> {
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
    return enqueueRepositoryOperation(this.rootDirectory, () => this.performAutomaticCheckpoint(message))
  }

  private async performAutomaticCheckpoint(message: string): Promise<VaultGitActionResult> {
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
    return this.runWithIndexLockRecovery(() => this.execute(args, options))
  }

  private async runWithIndexLockRecovery(execute: () => Promise<CommandResult>): Promise<CommandResult> {
    let result = await execute()
    if (!isIndexLockFailure(result)) return result

    await delay(indexLockRetryDelayMilliseconds)
    result = await execute()
    if (!isIndexLockFailure(result)) return result

    const quarantinedLock = await this.quarantineStaleIndexLock()
    if (!quarantinedLock) return result
    try {
      return await execute()
    } finally {
      await unlink(quarantinedLock).catch(() => undefined)
    }
  }

  private async quarantineStaleIndexLock(): Promise<string | undefined> {
    const lockPath = await this.resolveIndexLockPath()
    if (!lockPath) return undefined

    const initial = await safeLstat(lockPath)
    if (!initial?.isFile() || Date.now() - initial.mtimeMs < staleIndexLockMilliseconds) return undefined
    if (await fileHasOpenHandle(lockPath)) return undefined

    await delay(indexLockStabilityDelayMilliseconds)
    const stable = await safeLstat(lockPath)
    if (!stable?.isFile() || !sameFileState(initial, stable)) return undefined

    const quarantinedPath = `${lockPath}.mootool-stale-${Date.now()}-${process.pid}`
    try {
      await rename(lockPath, quarantinedPath)
      return quarantinedPath
    } catch (error) {
      if (['ENOENT', 'EEXIST'].includes((error as NodeJS.ErrnoException).code ?? '')) return undefined
      throw error
    }
  }

  private async resolveIndexLockPath(): Promise<string | undefined> {
    const result = await this.execute(['rev-parse', '--git-path', 'index.lock'])
    if (result.exitCode !== 0 || !result.stdout.trim()) return undefined
    const lockPath = resolve(this.rootDirectory, result.stdout.trim())
    return basename(lockPath) === 'index.lock' ? lockPath : undefined
  }

  private execute(args: string[], options: { authenticated?: boolean; cwd?: string } = {}): Promise<CommandResult> {
    const env: NodeJS.ProcessEnv = { ...process.env, GIT_TERMINAL_PROMPT: '0' }
    env.GIT_OPTIONAL_LOCKS = '0'
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

function enqueueRepositoryOperation<T>(rootDirectory: string, operation: () => Promise<T>): Promise<T> {
  const resolved = resolve(rootDirectory)
  const key = process.platform === 'win32' ? resolved.toLocaleLowerCase() : resolved
  const previous = repositoryOperationQueues.get(key) ?? Promise.resolve()
  const current = previous.catch(() => undefined).then(operation)
  const settled = current.then(() => undefined, () => undefined)
  repositoryOperationQueues.set(key, settled)
  void settled.finally(() => {
    if (repositoryOperationQueues.get(key) === settled) repositoryOperationQueues.delete(key)
  })
  return current
}

function isIndexLockFailure(result: CommandResult): boolean {
  return /unable to create [^\r\n]*index\.lock['"]?: file exists/i.test(`${result.stderr}\n${result.stdout}`)
}

async function safeLstat(path: string): Promise<Stats | undefined> {
  try {
    return await lstat(path)
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return undefined
    throw error
  }
}

function sameFileState(left: Stats, right: Stats): boolean {
  return left.dev === right.dev
    && left.ino === right.ino
    && left.size === right.size
    && left.mtimeMs === right.mtimeMs
}

function fileHasOpenHandle(path: string): Promise<boolean> {
  const executable = process.platform === 'darwin'
    ? '/usr/sbin/lsof'
    : process.platform === 'linux'
      ? 'lsof'
      : undefined
  if (!executable) return Promise.resolve(false)
  return new Promise((resolve) => {
    execFile(executable, ['-t', '--', path], {
      timeout: 2_000,
      windowsHide: true
    }, (_error, stdout) => resolve(Boolean(stdout.trim())))
  })
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}

function parseNameStatus(output: string): GitChangedPath[] {
  const records = output.split('\0')
  const changes: GitChangedPath[] = []
  let index = 0
  while (index < records.length) {
    const record = records[index++]
    if (!record) continue
    const separator = record.indexOf('\t')
    const status = separator >= 0 ? record.slice(0, separator) : record
    const inlinePath = separator >= 0 ? record.slice(separator + 1) : ''
    const renamed = status.startsWith('R') || status.startsWith('C')
    const originalPathValue = renamed ? inlinePath || records[index++] : undefined
    const pathValue = renamed ? records[index++] : inlinePath || records[index++]
    const path = normalizeGitPath(pathValue)
    if (!path) continue
    const originalPath = normalizeGitPath(originalPathValue)
    changes.push({ path, originalPath, status })
  }
  return changes
}

function buildDiffFile(change: GitChangedPath, before: ContentPreview, after: ContentPreview): VaultGitDiffFile {
  const preview: VaultGitDiffPreview = before.state === 'too-large' || after.state === 'too-large'
    ? 'too-large'
    : before.state === 'binary' || after.state === 'binary'
      ? 'binary'
      : 'text'
  return {
    path: change.path,
    originalPath: change.originalPath,
    status: change.status,
    before: before.text,
    after: after.text,
    preview
  }
}

function missingContent(): ContentPreview {
  return { text: '', state: 'missing' }
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
