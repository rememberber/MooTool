import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtemp, mkdir, readFile, readdir, realpath, rm, symlink, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { VaultReadService } from './vaultAccess'
import { callVaultTool } from './vaultTools'

describe('read-only vault tools', () => {
  let home: string
  let notes: string
  let json: string
  let policy: string
  let service: VaultReadService
  beforeEach(async () => {
    home = await realpath(await mkdtemp(join(tmpdir(), 'mootool-vault-read-')))
    notes = join(home, 'notes')
    json = join(home, 'json')
    policy = join(home, 'access.json')
    await mkdir(notes)
    await mkdir(json)
    await writeFile(policy, JSON.stringify({ version: 1, notes, json }))
    await writeFile(join(notes, 'note.md'), '---\ntitle: 我的笔记\nsyntax: text/markdown\n---\nMooTool fixture 🐮')
    await writeFile(join(json, 'document.json'), '{"fixture":"mootool","value":42}')
    service = new VaultReadService(policy)
  })
  afterEach(async () => { await rm(home, { recursive: true, force: true }) })

  it('searches metadata and content, reads paged content and never seeds or modifies files', async () => {
    const original = await readFile(join(notes, 'note.md'), 'utf8')
    const found = await service.search('notes', '我的笔记', 10, 0)
    expect(found.entries.map((item) => item.path)).toEqual(['note.md'])
    expect(found.entries[0].excerpt).toBe('MooTool fixture 🐮')
    const first = await service.read('notes', 'note.md', 0, 7)
    const second = await service.read('notes', 'note.md', first.nextOffset!, 50)
    expect(first.content + second.content).toBe('MooTool fixture 🐮')
    expect(await readFile(join(notes, 'note.md'), 'utf8')).toBe(original)
    expect(await readdir(notes)).toEqual(['note.md'])
    expect((await service.search('json', '42', 10, 0)).entries[0].path).toBe('document.json')
    expect(JSON.parse((await service.read('json', 'document.json', 0, 100)).content).value).toBe(42)
  })

  it('revokes access for an existing client immediately on its next call', async () => {
    expect((await callVaultTool('mootool_notes_read', { path: 'note.md' }, policy)).isError).not.toBe(true)
    await writeFile(policy, JSON.stringify({ version: 1, notes: null, json: null }))
    expect((await callVaultTool('mootool_notes_read', { path: 'note.md' }, policy)).isError).toBe(true)
    await expect(new VaultReadService().search('notes', '', 10, 0)).rejects.toThrow('disabled')
    expect((await callVaultTool('mootool_json_documents_read', { path: 'document.json', length: 100_000 }, policy)).isError).toBe(true)
  })

  it('rejects traversal, hidden paths, gitignored files, symlinks and oversized documents', async () => {
    await writeFile(join(notes, '.gitignore'), 'private/\nignored.md\n')
    await mkdir(join(notes, 'private'))
    await writeFile(join(notes, 'private', 'secret.md'), 'private secret')
    await writeFile(join(notes, 'ignored.md'), 'ignored secret')
    await writeFile(join(notes, '.secret.md'), 'hidden secret')
    await writeFile(join(home, 'outside.md'), 'outside secret')
    await symlink(join(home, 'outside.md'), join(notes, 'link.md'))
    await writeFile(join(notes, 'large.md'), 'x'.repeat(2_000_001))
    for (const path of ['../outside.md', '/etc/hosts', 'C:/outside.md', '..\\outside.md', '.secret.md', 'private/secret.md', 'ignored.md', 'link.md', 'large.md']) {
      await expect(service.read('notes', path, 0, 100)).rejects.toThrow()
    }
    expect((await service.search('notes', 'secret', 20, 0)).entries).toEqual([])
    expect((await service.search('notes', '', 20, 0)).entries.map((item) => item.path)).toEqual(['note.md'])
  })

  it('pages search results without dropping matches', async () => {
    for (let index = 0; index < 4; index++) await writeFile(join(json, `file-${index}.json`), `{"index":${index}}`)
    const first = await service.search('json', 'index', 2, 0)
    const second = await service.search('json', 'index', 2, first.nextOffset!)
    expect([...first.entries, ...second.entries].map((item) => item.path)).toEqual(['file-0.json', 'file-1.json', 'file-2.json', 'file-3.json'])
    expect(second.nextOffset).toBeNull()
  })
})
