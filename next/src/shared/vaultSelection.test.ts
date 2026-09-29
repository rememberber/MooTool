import { describe, expect, it } from 'vitest'
import { selectTreePaths, visibleTreePaths } from './vaultSelection'
const plain = { ctrlKey: false, metaKey: false, shiftKey: false }
describe('vault multiselection', () => {
  it('toggles individual selections with either platform modifier', () => {
    expect(selectTreePaths(['a'], 'b', 'a', ['a', 'b'], { ...plain, metaKey: true })).toEqual(['a', 'b'])
    expect(selectTreePaths(['a', 'b'], 'a', 'b', ['a', 'b'], { ...plain, ctrlKey: true })).toEqual(['b'])
    expect(selectTreePaths(['a', 'b'], 'c', 'a', ['a', 'b', 'c'], plain)).toEqual(['c'])
  })
  it('selects ranges in visible order, excluding collapsed children', () => {
    const visible = visibleTreePaths([{ relativePath: 'a', kind: 'directory', children: [{ relativePath: 'a/child', kind: 'file' }] }, { relativePath: 'b', kind: 'file' }, { relativePath: 'c', kind: 'file' }], new Set())
    expect(selectTreePaths(['c'], 'a', 'c', visible, { ...plain, shiftKey: true })).toEqual(['a', 'b', 'c'])
    expect(selectTreePaths(['c'], 'b', 'gone', visible, { ...plain, shiftKey: true })).toEqual(['b'])
  })
})
