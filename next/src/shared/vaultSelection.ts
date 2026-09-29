import { useEffect, useRef, useState } from 'react'

type TreeNode = { relativePath: string; kind: 'file' | 'directory'; children?: TreeNode[] }
export type SelectionGesture = { ctrlKey: boolean; metaKey: boolean; shiftKey: boolean }

export function visibleTreePaths(nodes: TreeNode[], expanded: ReadonlySet<string>): string[] {
  return nodes.flatMap(node => [node.relativePath, ...(node.kind === 'directory' && expanded.has(node.relativePath) ? visibleTreePaths(node.children ?? [], expanded) : [])])
}

export function selectTreePaths(current: string[], path: string, anchor: string, visible: string[], gesture: SelectionGesture): string[] {
  if (gesture.shiftKey && visible.includes(anchor)) {
    const start = visible.indexOf(anchor)
    const end = visible.indexOf(path)
    const range = visible.slice(Math.min(start, end), Math.max(start, end) + 1)
    return gesture.ctrlKey || gesture.metaKey ? [...new Set([...current, ...range])] : range
  }
  if (gesture.ctrlKey || gesture.metaKey) return current.includes(path) ? current.filter(value => value !== path) : [...current, path]
  return [path]
}

export function useVaultSelection(nodes: TreeNode[], expanded: ReadonlySet<string>, activePath: string) {
  const [paths, setPaths] = useState<string[]>(activePath ? [activePath] : [])
  const anchor = useRef(activePath)
  const visible = visibleTreePaths(nodes, expanded)
  const visibleKey = JSON.stringify(visible)
  useEffect(() => { setPaths(activePath ? [activePath] : []); anchor.current = activePath }, [activePath])
  useEffect(() => { setPaths(current => current.filter(path => visible.includes(path))) }, [visibleKey]) // Drop hidden or removed entries.
  const files = new Set<string>()
  const collect = (items: TreeNode[]) => items.forEach(node => { if (node.kind === 'file') files.add(node.relativePath); else collect(node.children ?? []) })
  collect(nodes)
  return {
    paths,
    filePaths: paths.filter(path => files.has(path)),
    select(path: string, gesture: SelectionGesture) {
      setPaths(current => selectTreePaths(current, path, anchor.current, visible, gesture))
      if (!gesture.shiftKey) anchor.current = path
      return !gesture.ctrlKey && !gesture.metaKey && !gesture.shiftKey
    },
    context(path: string) { if (!paths.includes(path)) { setPaths([path]); anchor.current = path } },
    filesFor(path: string) { return paths.includes(path) ? paths.filter(value => files.has(value)) : files.has(path) ? [path] : [] }
  }
}
