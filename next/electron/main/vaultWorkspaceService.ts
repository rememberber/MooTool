import { resolve } from 'node:path'
import type { RememberVaultFileInput, VaultKind, VaultWorkspace } from '../../src/shared/contracts/vaultWorkspace'
import type { VaultTreeLikeNode } from '../../src/shared/vaultTreeExpand'

type WorkspaceFile = { relativePath: string }
type WorkspaceRepository<File> = {
  list(): Promise<VaultTreeLikeNode[]>
  read(path: string): Promise<File>
}

/** Serializes startup and selection writes across renderer windows for each vault. */
export class VaultWorkspaceService {
  private readonly queues = new Map<string, Promise<unknown>>()

  constructor(private readonly storage: {
    get(): Record<string, string>
    set(paths: Record<string, string>): void
  }) {}

  open<File extends WorkspaceFile>(kind: VaultKind, rootDirectory: string, repository: WorkspaceRepository<File>, create: () => Promise<File>): Promise<VaultWorkspace<File>> {
    const key = this.key(kind, rootDirectory)
    return this.enqueue(key, async () => {
      const lastPath = this.storage.get()[key]
      if (lastPath) {
        try { return { rootDirectory, file: await repository.read(lastPath) } }
        catch (error) { if (!isMissingFile(error)) throw error }
      }
      const first = firstFile(await repository.list())
      const file = first ? await repository.read(first.relativePath) : await create()
      return { rootDirectory, file }
    })
  }

  remember(input: RememberVaultFileInput, repository: WorkspaceRepository<WorkspaceFile>): Promise<void> {
    const key = this.key(input.kind, input.rootDirectory)
    return this.enqueue(key, async () => {
      const file = await repository.read(input.relativePath)
      this.storage.set({ ...this.storage.get(), [key]: file.relativePath })
    })
  }

  relocate(kind: VaultKind, rootDirectory: string, before: string, next: string): void {
    const key = this.key(kind, rootDirectory)
    const paths = this.storage.get()
    const path = paths[key]
    if (path === before || path?.startsWith(`${before}/`)) {
      this.storage.set({ ...paths, [key]: `${next}${path.slice(before.length)}` })
    }
  }

  private key(kind: VaultKind, rootDirectory: string): string {
    return JSON.stringify([kind, resolve(rootDirectory)])
  }

  private enqueue<T>(key: string, action: () => Promise<T>): Promise<T> {
    const operation = (this.queues.get(key) ?? Promise.resolve()).catch(() => undefined).then(action)
    this.queues.set(key, operation)
    void operation.finally(() => { if (this.queues.get(key) === operation) this.queues.delete(key) }).catch(() => undefined)
    return operation
  }
}

function firstFile(nodes: VaultTreeLikeNode[]): VaultTreeLikeNode | undefined {
  for (const node of nodes) {
    if (node.kind === 'file') return node
    const nested = firstFile(node.children ?? [])
    if (nested) return nested
  }
}

function isMissingFile(error: unknown): boolean {
  return error instanceof Error && 'code' in error && (error.code === 'ENOENT' || error.code === 'ENOTDIR')
}
