import { describe, expect, it } from 'vitest'
import { ensureAncestorsExpanded, resolveExpandedPaths } from './vaultTreeExpand'

describe('vault tree expand mode', () => {
  const directories = ['Work', 'Work/API', 'Archive']

  it('expands every directory by default and keeps only ancestors when collapsed', () => {
    expect([...resolveExpandedPaths(directories, 'expandAll')].sort()).toEqual(['Archive', 'Work', 'Work/API'])
    expect([...resolveExpandedPaths(directories, 'collapseAll', 'Work/API/ideas.md')]).toEqual(['Work', 'Work/API'])
    expect([...resolveExpandedPaths(directories, 'collapseAll')]).toEqual([])
  })

  it('opens ancestor directories without collapsing a manual selection', () => {
    const expanded = ensureAncestorsExpanded(new Set(['Archive']), 'Work/API/ideas.md')
    expect([...expanded].sort()).toEqual(['Archive', 'Work', 'Work/API'])
  })
})
