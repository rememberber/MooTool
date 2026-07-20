import { describe, expect, it } from 'vitest'
import {
  ancestorDirectoryPaths,
  collectDirectoryPaths,
  ensureAncestorsExpanded,
  resolveExpandedPaths,
  type VaultTreeLikeNode
} from './vaultTreeExpand'

const sampleTree: VaultTreeLikeNode[] = [
  {
    kind: 'directory',
    relativePath: 'work',
    children: [
      {
        kind: 'directory',
        relativePath: 'work/nested',
        children: [{ kind: 'file', relativePath: 'work/nested/note.txt' }]
      },
      { kind: 'file', relativePath: 'work/root.txt' }
    ]
  },
  { kind: 'file', relativePath: 'solo.txt' }
]

describe('vaultTreeExpand', () => {
  it('collects directory paths recursively', () => {
    expect(collectDirectoryPaths(sampleTree)).toEqual(['work', 'work/nested'])
  })

  it('resolves ancestor directories for nested files', () => {
    expect(ancestorDirectoryPaths('work/nested/note.txt')).toEqual(['work', 'work/nested'])
    expect(ancestorDirectoryPaths('solo.txt')).toEqual([])
  })

  it('expands all directories or only selected ancestors', () => {
    const directories = collectDirectoryPaths(sampleTree)
    expect([...resolveExpandedPaths(directories, 'expandAll')].sort()).toEqual(['work', 'work/nested'])
    expect([...resolveExpandedPaths(directories, 'collapseAll', 'work/nested/note.txt')].sort()).toEqual(['work', 'work/nested'])
    expect([...resolveExpandedPaths(directories, 'collapseAll')]).toEqual([])
  })

  it('ensures selected ancestors stay expanded', () => {
    expect([...ensureAncestorsExpanded(new Set(['work']), 'work/nested/note.txt')].sort()).toEqual(['work', 'work/nested'])
  })
})
