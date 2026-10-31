import type { VaultFileEntry } from '../../platform/contracts/vault'
import { ancestorDirectoryPaths } from '../../shared/vaultTreeExpand'

export type VaultListSort = 'name' | 'modified'

export type VaultListRow =
  | { kind: 'folder'; path: string; name: string; depth: number }
  | { kind: 'file'; file: VaultFileEntry; name: string; depth: number }

export function selectVaultRows(
  files: VaultFileEntry[],
  directories: string[],
  query: string,
  includeContent: boolean,
  contentMatches: ReadonlySet<string>,
  sort: VaultListSort
): VaultListRow[] {
  const keyword = query.trim().toLocaleLowerCase()
  const visibleFiles = files.filter((file) => {
    if (!keyword) return true
    if (file.relativePath.toLocaleLowerCase().includes(keyword)) return true
    return includeContent && contentMatches.has(file.relativePath)
  })
  const folders = new Set(directories)
  for (const file of files) addParentFolders(folders, file.relativePath)
  if (keyword) {
    const keep = new Set<string>()
    for (const path of folders) {
      const matches = path.toLocaleLowerCase().includes(keyword)
      const hasFile = visibleFiles.some((file) => file.relativePath.startsWith(`${path}/`))
      if (matches || hasFile) keep.add(path)
    }
    for (const path of [...keep]) {
      for (const ancestor of ancestorDirectoryPaths(path)) if (folders.has(ancestor)) keep.add(ancestor)
    }
    for (const path of [...folders]) if (!keep.has(path)) folders.delete(path)
  }
  return flatten('', [...folders], visibleFiles, sort)
}

function addParentFolders(folders: Set<string>, relativePath: string): void {
  const segments = relativePath.split('/')
  for (let depth = 0; depth < segments.length - 1; depth += 1) folders.add(segments.slice(0, depth + 1).join('/'))
}

function flatten(parent: string, folders: string[], files: VaultFileEntry[], sort: VaultListSort): VaultListRow[] {
  const depth = parent ? parent.split('/').length : 0
  const childFolders = folders.filter((path) => parentOf(path) === parent).sort((left, right) => leaf(left).localeCompare(leaf(right)))
  const childFiles = files.filter((file) => parentOf(file.relativePath) === parent).sort((left, right) => {
    if (sort === 'modified') {
      const byTime = right.modifiedAt - left.modifiedAt
      if (byTime) return byTime
    }
    return leaf(left.relativePath).localeCompare(leaf(right.relativePath))
  })
  const rows: VaultListRow[] = []
  for (const path of childFolders) {
    rows.push({ kind: 'folder', path, name: leaf(path), depth })
    rows.push(...flatten(path, folders, files, sort))
  }
  for (const file of childFiles) rows.push({ kind: 'file', file, name: leaf(file.relativePath), depth })
  return rows
}

function parentOf(path: string): string {
  const slash = path.lastIndexOf('/')
  return slash < 0 ? '' : path.slice(0, slash)
}

function leaf(path: string): string {
  return path.split('/').at(-1) ?? path
}
