import { describe, expect, it } from 'vitest'
import type { VaultFileEntry } from '../../platform/contracts/vault'
import { selectVaultRows } from './jsonVaultList'

function file(relativePath: string, modifiedAt: number): VaultFileEntry {
  return { relativePath, sizeBytes: 10, modifiedAt }
}

describe('JSON Vault list', () => {
  it('lists folders before files and sorts files by name by default', () => {
    const rows = selectVaultRows(
      [file('zeta.json', 1), file('alpha.json', 9)],
      ['middle'],
      '',
      true,
      new Set(),
      'name'
    )
    expect(rows.map((row) => row.kind === 'folder' ? row.path : row.file.relativePath)).toEqual(['middle', 'alpha.json', 'zeta.json'])
  })

  it('sorts sibling files by modification time without moving folders', () => {
    const rows = selectVaultRows(
      [file('zeta.json', 9), file('alpha.json', 1)],
      ['middle'],
      '',
      true,
      new Set(),
      'modified'
    )
    expect(rows.map((row) => row.kind === 'folder' ? row.path : row.file.relativePath)).toEqual(['middle', 'zeta.json', 'alpha.json'])
  })

  it('matches paths without reading content, and uses content matches only when requested', () => {
    const files = [file('notes.json', 1), file('other.json', 1)]
    const content = new Set(['notes.json'])
    expect(selectVaultRows(files, [], 'token', true, content, 'name').map((row) => row.kind === 'file' ? row.file.relativePath : row.path)).toEqual(['notes.json'])
    expect(selectVaultRows(files, [], 'token', false, content, 'name')).toEqual([])
    expect(selectVaultRows(files, [], 'other', false, content, 'name').map((row) => row.kind === 'file' ? row.file.relativePath : row.path)).toEqual(['other.json'])
  })

  it('keeps ancestor folders when only a nested folder name matches', () => {
    const rows = selectVaultRows([], ['Work', 'Work/API', 'Archive'], 'api', true, new Set(), 'name')
    expect(rows.map((row) => row.kind === 'folder' ? row.path : '')).toEqual(['Work', 'Work/API'])
  })
})
