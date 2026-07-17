export type VaultTreeExpandMode = 'expandAll' | 'collapseAll'

export type VaultTreeLikeNode = {
  kind: 'file' | 'directory'
  relativePath: string
  children?: VaultTreeLikeNode[]
}

export function isVaultTreeExpandMode(value: unknown): value is VaultTreeExpandMode {
  return value === 'expandAll' || value === 'collapseAll'
}

export function collectDirectoryPaths(nodes: VaultTreeLikeNode[]): string[] {
  return nodes.flatMap((node) => (
    node.kind === 'directory'
      ? [node.relativePath, ...collectDirectoryPaths(node.children ?? [])]
      : []
  ))
}

export function ancestorDirectoryPaths(relativePath: string): string[] {
  if (!relativePath.includes('/')) return []
  const parts = relativePath.split('/')
  const paths: string[] = []
  for (let index = 1; index < parts.length; index += 1) {
    paths.push(parts.slice(0, index).join('/'))
  }
  return paths
}

export function resolveExpandedPaths(
  directories: Iterable<string>,
  mode: VaultTreeExpandMode,
  selectedPath = ''
): Set<string> {
  if (mode === 'expandAll') return new Set(directories)
  return new Set(selectedPath ? ancestorDirectoryPaths(selectedPath) : [])
}

export function ensureAncestorsExpanded(expanded: ReadonlySet<string>, selectedPath: string): Set<string> {
  if (!selectedPath) return new Set(expanded)
  const next = new Set(expanded)
  for (const path of ancestorDirectoryPaths(selectedPath)) next.add(path)
  return next
}
