import { randomUUID } from 'node:crypto'
import { link, lstat, mkdir, readFile, readdir, realpath, rename, unlink, writeFile } from 'node:fs/promises'
import { basename, dirname, isAbsolute, join } from 'node:path'
import { normalizeRenameOptions, renameCandidate, renameNameKey, validRenameName, type RenameFile, type RenamePreview, type RenameResult, type RenameRow, type RenameStatus } from '../../src/shared/contracts/batchRename'

type Identity = { dev: string; ino: string }
type Move = { source: string; target: string; temporary: string; identity: Identity }
type Journal = { version: 1; phase: 'pending' | 'done'; moves: Move[]; previous?: Journal }
type Plan = { owner: number; rows: RenameRow[]; moves: Move[] }
type FsApi = { link: typeof link; unlink: typeof unlink }

function fail(code: string): never { throw new Error(`batchRename.error.${code}`) }
function matches(left: Identity, right: Identity): boolean { return left.dev === right.dev && left.ino === right.ino }

export class BatchRenameService {
  private selections = new Map<number, Map<string, RenameFile>>()
  private selectionIdentities = new Map<number, Map<string, Identity>>()
  private plans = new Map<string, Plan>()
  private busy = false
  readonly journalPath: string

  constructor(directory: string, private readonly fs: FsApi = { link, unlink }) { this.journalPath = join(directory, 'batch-rename-journal.json') }

  release(owner: number): void {
    this.selections.delete(owner)
    this.selectionIdentities.delete(owner)
    for (const [token, plan] of this.plans) if (plan.owner === owner) this.plans.delete(token)
  }

  async select(owner: number, paths: string[]): Promise<RenameFile[]> {
    return this.lock(async () => {
      if (paths.length > 500) fail('limit')
      const selected = new Map<string, RenameFile>()
      const identities = new Map<string, Identity>()
      const additions: RenameFile[] = []
      for (const path of paths) {
        if (!isAbsolute(path)) fail('file')
        const parent = await realpath(dirname(path))
        const names = await readdir(parent)
        const name = names.find((name) => name === basename(path)) ?? names.find((name) => renameNameKey(name) === renameNameKey(basename(path)))
        if (!name) fail('file')
        const canonical = join(parent, name)
        const identity = await this.identity(canonical)
        if (!identity) fail('file')
        const existing = [...selected.values()].find((file) => file.path === canonical)
        const file = existing ?? { id: randomUUID(), path: canonical, name }
        selected.set(file.id, file)
        identities.set(file.id, identity)
        additions.push(file)
      }
      const next = new Map(selected)
      for (const file of additions) next.set(file.id, file)
      if (next.size > 500) fail('limit')
      this.selections.set(owner, next)
      this.selectionIdentities.set(owner, identities)
      for (const [token, plan] of this.plans) if (plan.owner === owner) this.plans.delete(token)
      return [...next.values()]
    })
  }

  async preview(owner: number, ids: unknown, input: unknown): Promise<RenamePreview> {
    return this.lock(async () => {
      if ((await this.status()).recoveryRequired) fail('recovery')
      if (!Array.isArray(ids) || ids.length === 0 || ids.length > 500 || new Set(ids).size !== ids.length) fail('selection')
      const options = normalizeRenameOptions(input)
      const files = ids.map((id) => this.selections.get(owner)?.get(id))
      if (files.some((file) => !file)) fail('selection')
      const rows: RenameRow[] = []
      const moves: Move[] = []
      const targetKeys = new Map<string, RenameRow[]>()
      const identities = new Map<string, RenameRow[]>()
      for (const [index, file] of (files as RenameFile[]).entries()) {
        const newName = renameCandidate(file.name, options, index)
        const row: RenameRow = { ...file, newName, status: newName === file.name ? 'unchanged' : 'ready' }
        rows.push(row)
        const identity = await this.identity(file.path)
        const granted = this.selectionIdentities.get(owner)?.get(file.id)
        if (!identity || !granted || !matches(identity, granted)) { row.status = 'conflict'; row.issue = 'changed'; continue }
        const identityKey = `${identity.dev}:${identity.ino}`
        identities.set(identityKey, [...identities.get(identityKey) ?? [], row])
        if (row.status === 'unchanged') continue
        if (!validRenameName(newName)) { row.status = 'conflict'; row.issue = 'invalidName'; continue }
        const key = join(dirname(file.path), renameNameKey(newName))
        targetKeys.set(key, [...targetKeys.get(key) ?? [], row])
        moves.push({ source: file.path, target: join(dirname(file.path), newName), temporary: join(dirname(file.path), `.mootool-rename-${randomUUID()}`), identity })
      }
      for (const duplicates of targetKeys.values()) if (duplicates.length > 1) for (const row of duplicates) { row.status = 'conflict'; row.issue = 'duplicate' }
      for (const duplicates of identities.values()) if (duplicates.length > 1) for (const row of duplicates) { row.status = 'conflict'; row.issue = 'sameFile' }
      const sourceKeys = new Set(moves.map((move) => move.source.normalize('NFC')))
      for (const row of rows.filter((row) => row.status === 'ready')) {
        const occupied = (await readdir(dirname(row.path))).filter((name) => renameNameKey(name) === renameNameKey(row.newName))
        if (occupied.some((name) => !sourceKeys.has(join(dirname(row.path), name).normalize('NFC')))) { row.status = 'conflict'; row.issue = 'occupied' }
      }
      for (const [token, plan] of this.plans) if (plan.owner === owner) this.plans.delete(token)
      const token = rows.some((row) => row.status === 'conflict') || moves.length === 0 ? null : randomUUID()
      if (token) this.plans.set(token, { owner, rows, moves })
      return { token, rows, changedCount: rows.filter((row) => row.status === 'ready').length }
    })
  }

  async execute(owner: number, token: unknown): Promise<RenameResult> {
    return this.lock(async () => {
      const plan = typeof token === 'string' ? this.plans.get(token) : null
      if (!plan || plan.owner !== owner) fail('preview')
      await this.preflight(plan.moves)
      const previous = await this.readJournal()
      if (previous?.phase === 'pending') fail('recovery')
      this.plans.delete(String(token))
      const journal: Journal = { version: 1, phase: 'pending', moves: plan.moves, ...(previous ? { previous } : {}) }
      await this.saveJournal(journal)
      try {
        await this.run(plan.moves)
        await this.saveJournal({ version: 1, phase: 'done', moves: plan.moves })
      } catch (error) {
        await this.rollback(journal)
        if (['ENOTSUP', 'EOPNOTSUPP', 'EXDEV', 'EPERM'].includes((error as NodeJS.ErrnoException).code ?? '')) fail('unsupported')
        throw error
      }
      this.updateSelections(plan.moves)
      return { changedCount: plan.moves.length, files: [...this.selections.get(owner)?.values() ?? []], status: await this.status() }
    })
  }

  async undo(owner: number): Promise<RenameResult> {
    return this.lock(async () => {
      const journal = await this.readJournal()
      if (!journal) fail('undo')
      if (journal.phase === 'pending') {
        await this.rollback(journal)
        this.updateSelections(journal.moves.map((move) => ({ ...move, source: move.target, target: move.source })))
        return { changedCount: journal.moves.length, files: [...this.selections.get(owner)?.values() ?? []], status: await this.status() }
      }
      const moves = journal.moves.map((move) => ({ source: move.target, target: move.source, temporary: join(dirname(move.source), `.mootool-rename-${randomUUID()}`), identity: move.identity }))
      await this.preflight(moves)
      const pending: Journal = { version: 1, phase: 'pending', moves, previous: journal }
      await this.saveJournal(pending)
      try { await this.run(moves); await this.removeJournal() }
      catch (error) { await this.rollback(pending); throw error }
      this.updateSelections(moves)
      return { changedCount: moves.length, files: [...this.selections.get(owner)?.values() ?? []], status: await this.status() }
    })
  }

  async status(): Promise<RenameStatus> {
    const journal = await this.readJournal()
    return { undoAvailable: Boolean(journal), recoveryRequired: journal?.phase === 'pending', journalPath: this.journalPath }
  }

  private async lock<T>(run: () => Promise<T>): Promise<T> {
    if (this.busy) fail('busy')
    this.busy = true
    try { return await run() } finally { this.busy = false }
  }

  private async identity(path: string): Promise<Identity | null> {
    try {
      const stat = await lstat(path, { bigint: true })
      if (!stat.isFile() || stat.isSymbolicLink()) return null
      return { dev: String(stat.dev), ino: String(stat.ino) }
    } catch (error) { if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null; throw error }
  }

  private async exactIdentity(path: string): Promise<{ path: string; identity: Identity } | null> {
    const names = await readdir(dirname(path))
    const exact = names.find((name) => name === basename(path))
    const equivalent = names.filter((name) => name.normalize('NFC') === basename(path).normalize('NFC'))
    const name = exact ?? (equivalent.length === 1 ? equivalent[0] : undefined)
    if (!name) return null
    const actual = join(dirname(path), name)
    const identity = await this.identity(actual)
    return identity ? { path: actual, identity } : null
  }

  private async preflight(moves: Move[]): Promise<void> {
    const sources = new Set(moves.map((move) => move.source.normalize('NFC')))
    for (const move of moves) {
      const current = await this.identity(move.source)
      if (!current || !matches(current, move.identity)) fail('changed')
      const occupied = (await readdir(dirname(move.target))).filter((name) => renameNameKey(name) === renameNameKey(basename(move.target)))
      if (occupied.some((name) => !sources.has(join(dirname(move.target), name).normalize('NFC')))) fail('occupied')
    }
  }

  private async move(source: string, target: string, expected: Identity): Promise<void> {
    const current = await this.identity(source)
    if (!current || !matches(current, expected)) fail('changed')
    // link() creates exclusively; unlike rename(), it never replaces a destination.
    await this.fs.link(source, target)
    const after = await this.identity(source)
    if (!after || !matches(after, expected)) fail('changed')
    await this.fs.unlink(source)
  }

  private async run(moves: Move[]): Promise<void> {
    for (const move of moves) await this.move(move.source, move.temporary, move.identity)
    for (const move of moves) await this.move(move.temporary, move.target, move.identity)
  }

  private async rollback(journal: Journal): Promise<void> {
    try {
      const restore: Move[] = []
      for (const move of journal.moves) {
        const source = await this.exactIdentity(move.source)
        const temp = await this.exactIdentity(move.temporary)
        const target = await this.exactIdentity(move.target)
        const owned = [source, temp, target].filter((file) => file && matches(file.identity, move.identity))
        if (source && matches(source.identity, move.identity)) {
          for (const file of owned) if (file && file.path !== source.path) await this.fs.unlink(file.path)
          continue
        }
        if (temp && !matches(temp.identity, move.identity)) fail('occupied')
        const file = owned[0]
        if (!file) fail('changed')
        if (!temp) await this.move(file.path, move.temporary, move.identity)
        for (const other of owned) if (other && other.path !== move.temporary && (temp || other.path !== file.path)) await this.fs.unlink(other.path)
        restore.push(move)
      }
      for (const move of restore) await this.move(move.temporary, move.source, move.identity)
      if (journal.previous) await this.saveJournal(journal.previous)
      else await this.removeJournal()
    } catch { fail('recovery') }
  }

  private updateSelections(moves: Move[]): void {
    const mapping = new Map(moves.map((move) => [move.source, move.target]))
    for (const files of this.selections.values()) for (const [id, file] of files) {
      const path = mapping.get(file.path)
      if (path) files.set(id, { ...file, path, name: basename(path) })
    }
    this.plans.clear()
  }

  private async saveJournal(journal: Journal): Promise<void> {
    await mkdir(dirname(this.journalPath), { recursive: true })
    const temporary = `${this.journalPath}.${randomUUID()}.tmp`
    await writeFile(temporary, JSON.stringify(journal), { flag: 'wx', mode: 0o600 })
    await rename(temporary, this.journalPath)
  }
  private async removeJournal(): Promise<void> { try { await unlink(this.journalPath) } catch (error) { if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error } }
  private async readJournal(): Promise<Journal | null> {
    try {
      const stat = await lstat(this.journalPath)
      if (!stat.isFile() || stat.isSymbolicLink() || stat.size > 2_000_000) fail('journal')
      const validate = (value: unknown, previous = false): Journal => {
        if (!value || typeof value !== 'object') fail('journal')
        const journal = value as Journal
        if (journal.version !== 1 || !['pending', 'done'].includes(journal.phase) || !Array.isArray(journal.moves) || journal.moves.length < 1 || journal.moves.length > 500) fail('journal')
        for (const move of journal.moves) {
          if (!move || typeof move !== 'object') fail('journal')
          if (![move.source, move.target, move.temporary].every((path) => typeof path === 'string' && isAbsolute(path))
            || dirname(move.source) !== dirname(move.target) || dirname(move.source) !== dirname(move.temporary)
            || !/^\.mootool-rename-[\da-f-]{36}$/.test(basename(move.temporary))
            || !move.identity || ![move.identity.dev, move.identity.ino].every((value) => typeof value === 'string' && /^\d+$/.test(value))) fail('journal')
        }
        if (journal.previous) { if (previous || journal.previous.phase !== 'done') fail('journal'); validate(journal.previous, true) }
        return journal
      }
      return validate(JSON.parse(await readFile(this.journalPath, 'utf8')))
    } catch (error) { if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null; if (error instanceof SyntaxError) fail('journal'); throw error }
  }
}
