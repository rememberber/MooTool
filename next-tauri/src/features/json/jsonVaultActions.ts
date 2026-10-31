export const vaultDragPathType = 'application/x-mootool-vault-path'
export const vaultDragKindType = 'application/x-mootool-vault-kind'

/** Same rules as Electron `canMoveJsonVaultEntry`: not the current folder, and not into itself. */
export function canMoveVaultEntry(path: string, targetDirectory: string): boolean {
  const currentDirectory = path.includes('/') ? path.slice(0, path.lastIndexOf('/')) : ''
  return currentDirectory !== targetDirectory && path !== targetDirectory && !targetDirectory.startsWith(`${path}/`)
}

export function uniqueDuplicatePath(relativePath: string, taken: ReadonlySet<string>): string {
  const slash = relativePath.lastIndexOf('/')
  const parent = slash >= 0 ? relativePath.slice(0, slash) : ''
  const stem = (slash >= 0 ? relativePath.slice(slash + 1) : relativePath).replace(/\.json$/i, '')
  for (let index = 0; index < 10_000; index += 1) {
    const suffix = index === 0 ? '' : ` (${index + 1})`
    const leaf = `${stem} Copy${suffix}.json`
    const candidate = parent ? `${parent}/${leaf}` : leaf
    if (!taken.has(candidate)) return candidate
  }
  throw new Error('Unable to allocate a unique Vault path')
}

export function vaultDragDestination(path: string, targetDirectory: string): string {
  const leaf = path.split('/').pop() ?? path
  return targetDirectory ? `${targetDirectory}/${leaf}` : leaf
}

export function readDraggedVaultEntry(dataTransfer: DataTransfer): { path: string; kind: 'file' | 'folder' } | null {
  const path = dataTransfer.getData(vaultDragPathType)
  const kind = dataTransfer.getData(vaultDragKindType)
  if (!path || (kind !== 'file' && kind !== 'folder')) return null
  return { path, kind }
}

/** Rename stays in the same directory and follows Electron's Vault name rules. */
export function renamedVaultPath(relativePath: string, name: string, file: boolean): string {
  if (/[\r\n\0]/.test(name)) throw new Error('Invalid Vault name')
  const withoutExtension = file ? name.trim().replace(/\.json$/i, '') : name.trim()
  const sanitized = withoutExtension.replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim()
  if (!sanitized) throw new Error('Invalid Vault name')
  const leaf = `${sanitized.slice(0, 180)}${file ? '.json' : ''}`
  const slash = relativePath.lastIndexOf('/')
  const parent = slash >= 0 ? relativePath.slice(0, slash + 1) : ''
  return `${parent}${leaf}`
}
