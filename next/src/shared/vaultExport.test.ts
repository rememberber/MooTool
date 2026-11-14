import { mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import { exportVaultFiles } from '../../electron/main/vaultExport'

describe('vault batch export', () => {
  it('preserves existing files and gives duplicate basenames unique names', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'vault-export-'))
    try {
      await writeFile(join(directory, 'data.json'), 'original')
      expect(await exportVaultFiles(directory, [{ relativePath: 'a/data.json', content: 'first' }, { relativePath: 'b/data.json', content: 'second' }], 'json')).toBe(2)
      expect(await readFile(join(directory, 'data.json'), 'utf8')).toBe('original')
      expect(await readFile(join(directory, 'data-1.json'), 'utf8')).toBe('first')
      expect(await readFile(join(directory, 'data-2.json'), 'utf8')).toBe('second')
    } finally { await rm(directory, { recursive: true, force: true }) }
  })
  it('preserves the JSON document suffix and any dots in its name', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'vault-export-'))
    try {
      await exportVaultFiles(directory, [{ relativePath: 'requests/example.v2.JSON', content: '{}' }], 'json')
      expect(await readdir(directory)).toEqual(['example.v2.JSON'])
    } finally { await rm(directory, { recursive: true, force: true }) }
  })
  it('flattens note paths and resolves flattened-name collisions', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'vault-export-'))
    try {
      await exportVaultFiles(directory, [
        { relativePath: 'a/b.md', content: '# 正文' },
        { relativePath: 'a-b.md', content: 'other' },
        { relativePath: 'legacy.txt', content: 'SELECT 1;', metadata: { syntax: 'text/sql' } },
        { relativePath: 'legacy-json.txt', content: '{}', metadata: { syntax: 'text/json' } },
        { relativePath: 'config.v2.yml', content: 'enabled: true' },
        { relativePath: 'plain.txt', content: 'plain text', metadata: { syntax: 'text/plain' } }
      ], 'quickNote')
      expect((await readdir(directory)).sort()).toEqual(['a-b-1.md', 'a-b.md', 'config.v2.yml', 'legacy-json.json', 'legacy.sql', 'plain.txt'])
      expect(await readFile(join(directory, 'a-b.md'), 'utf8')).toBe('# 正文')
      expect(await readFile(join(directory, 'a-b-1.md'), 'utf8')).toBe('other')
    } finally { await rm(directory, { recursive: true, force: true }) }
  })
})
