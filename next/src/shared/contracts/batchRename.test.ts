import { describe, expect, it } from 'vitest'
import { defaultRenameOptions, normalizeRenameOptions, renameCandidate, validRenameName } from './batchRename'

describe('batch rename rules', () => {
  it('replaces literal text, preserves the final extension and appends numbered suffixes', () => {
    expect(renameCandidate('Photo.final.JPG', { ...defaultRenameOptions, find: 'photo', replacement: '$1', caseSensitive: false, prefix: 'new_', suffix: '_done', numbering: true }, 1)).toBe('new_$1.final_done_002.JPG')
    expect(renameCandidate('a[1].txt', { ...defaultRenameOptions, find: '[1]', replacement: '[2]' }, 0)).toBe('a[2].txt')
    expect(renameCandidate('name.txt', { ...defaultRenameOptions, find: '.txt', replacement: '.md' }, 0)).toBe('name.txt')
    expect(renameCandidate('name.txt', { ...defaultRenameOptions, find: '.txt', replacement: '.md', preserveExtension: false }, 0)).toBe('name.md')
    expect(renameCandidate('.env', { ...defaultRenameOptions, prefix: 'backup_' }, 0)).toBe('backup_.env')
  })
  it('rejects path escapes, reserved names and names beyond the UTF-8 filename limit', () => {
    for (const name of ['', '.', '..', '../x', 'a\\b', 'CON.txt', 'foo.', 'foo ', 'nul', 'a\0b', '你'.repeat(86)]) expect(validRenameName(name)).toBe(false)
    expect(validRenameName('你好_001.txt')).toBe(true)
  })
  it('validates types and numbering limits before generating names', () => {
    expect(normalizeRenameOptions(defaultRenameOptions)).toEqual(defaultRenameOptions)
    for (const value of [null, {}, { ...defaultRenameOptions, start: NaN }, { ...defaultRenameOptions, width: 9 }, { ...defaultRenameOptions, prefix: 'x'.repeat(121) }]) expect(() => normalizeRenameOptions(value)).toThrow('options')
  })
})
