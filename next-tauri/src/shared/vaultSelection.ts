type TreeNode = { relativePath: string; kind: 'file' | 'directory'; children?: TreeNode[] }
export type SelectionGesture = { ctrlKey: boolean; metaKey: boolean; shiftKey: boolean }

export function visibleTreePaths(nodes: TreeNode[], expanded: ReadonlySet<string>): string[] {
  return nodes.flatMap((node) => [
    node.relativePath,
    ...(node.kind === 'directory' && expanded.has(node.relativePath) ? visibleTreePaths(node.children ?? [], expanded) : [])
  ])
}

export function selectTreePaths(current: string[], path: string, anchor: string, visible: string[], gesture: SelectionGesture): string[] {
  if (gesture.shiftKey && visible.includes(anchor)) {
    const start = visible.indexOf(anchor)
    const end = visible.indexOf(path)
    const range = visible.slice(Math.min(start, end), Math.max(start, end) + 1)
    return gesture.ctrlKey || gesture.metaKey ? [...new Set([...current, ...range])] : range
  }
  if (gesture.ctrlKey || gesture.metaKey) return current.includes(path) ? current.filter((value) => value !== path) : [...current, path]
  return [path]
}
