import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { link, lstat, mkdtemp, readFile, readdir, realpath, rm, symlink, unlink, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { BatchRenameService } from './batchRenameService'
import { defaultRenameOptions } from '../../src/shared/contracts/batchRename'

let directory: string
let service: BatchRenameService
beforeEach(async () => { directory = await realpath(await mkdtemp(join(tmpdir(), 'mootool-rename-test-'))); service = new BatchRenameService(join(directory, 'state')) })
afterEach(async () => { await rm(directory, { recursive: true, force: true }) })
async function file(name: string, content = name): Promise<string> { const path = join(directory, name); await writeFile(path, content); return path }

describe('batch rename service', () => {
  it('only executes approved selection ids and preview tokens belonging to their owner', async () => {
    const path = await file('a.txt')
    const files = await service.select(1, [path])
    await expect(service.preview(2, files.map((file) => file.id), defaultRenameOptions)).rejects.toThrow('selection')
    const preview = await service.preview(1, files.map((file) => file.id), { ...defaultRenameOptions, prefix: 'new_' })
    await expect(service.execute(2, preview.token)).rejects.toThrow('preview')
    await service.select(1, [path])
    await expect(service.execute(1, preview.token)).rejects.toThrow('preview')
    await expect(service.select(1, [await file('b.txt'), join(directory, 'state')])).rejects.toThrow('file')
    await symlink(path, join(directory, 'alias.txt'))
    await expect(service.select(1, [join(directory, 'alias.txt')])).rejects.toThrow('file')
    expect(await readFile(path, 'utf8')).toBe('a.txt')
  })

  it('previews without writes, preserves file identity/content and persists undo after restart', async () => {
    const paths = [await file('a.txt', 'alpha'), await file('b.txt', 'beta')]
    const before = await lstat(paths[0], { bigint: true })
    const files = await service.select(1, paths)
    const preview = await service.preview(1, files.map((file) => file.id), { ...defaultRenameOptions, prefix: 'new_', numbering: true })
    expect(preview.rows.map((row) => row.newName)).toEqual(['new_a_001.txt', 'new_b_002.txt'])
    expect(await readdir(directory)).toEqual(['a.txt', 'b.txt'])
    const result = await service.execute(1, preview.token)
    expect(result.changedCount).toBe(2)
    expect(await readFile(join(directory, 'new_a_001.txt'), 'utf8')).toBe('alpha')
    expect((await lstat(join(directory, 'new_a_001.txt'), { bigint: true })).ino).toBe(before.ino)
    const restarted = new BatchRenameService(join(directory, 'state'))
    expect((await restarted.status()).undoAvailable).toBe(true)
    await restarted.undo(9)
    expect(await readFile(paths[0], 'utf8')).toBe('alpha')
    expect(await readFile(paths[1], 'utf8')).toBe('beta')
    expect((await restarted.status()).undoAvailable).toBe(false)
  })

  it('blocks duplicate, occupied, invalid and same-inode names and leaves unchanged files alone', async () => {
    const a = await file('a.txt')
    const b = await file('b.txt')
    const files = await service.select(1, [a, b])
    const duplicate = await service.preview(1, files.map((file) => file.id), { ...defaultRenameOptions, find: 'a', replacement: 'b' })
    expect(duplicate.token).toBeNull()
    expect(duplicate.rows[0].issue).toBe('occupied')
    const invalid = await service.preview(1, [files[0].id], { ...defaultRenameOptions, prefix: '../' })
    expect(invalid.rows[0].issue).toBe('invalidName')
    expect((await service.preview(1, [files[0].id], defaultRenameOptions)).rows[0].status).toBe('unchanged')
    await link(a, join(directory, 'alias.txt'))
    const linked = await service.select(1, [a, join(directory, 'alias.txt')])
    expect((await service.preview(1, linked.map((file) => file.id), { ...defaultRenameOptions, prefix: 'new_' })).rows.every((row) => row.issue === 'sameFile')).toBe(true)
    const x = await file('x.txt'); const xx = await file('xx.txt')
    const duplicates = await service.select(1, [x, xx])
    expect((await service.preview(1, duplicates.map((file) => file.id), { ...defaultRenameOptions, find: 'x', replacement: 'z' })).token).not.toBeNull()
    const duplicateNames = await service.preview(1, duplicates.map((file) => file.id), { ...defaultRenameOptions, find: 'x', replacement: '' })
    expect(duplicateNames.rows.every((row) => row.issue === 'duplicate')).toBe(true)
  })

  it('revalidates target occupancy and source identity immediately before executing', async () => {
    const path = await file('a.txt')
    const files = await service.select(1, [path])
    let preview = await service.preview(1, [files[0].id], { ...defaultRenameOptions, prefix: 'new_' })
    await file('new_a.txt', 'foreign')
    await expect(service.execute(1, preview.token)).rejects.toThrow('occupied')
    expect(await readFile(join(directory, 'new_a.txt'), 'utf8')).toBe('foreign')
    await unlink(join(directory, 'new_a.txt'))
    preview = await service.preview(1, [files[0].id], { ...defaultRenameOptions, prefix: 'new_' })
    await unlink(path); await file('a.txt', 'replacement')
    await expect(service.execute(1, preview.token)).rejects.toThrow('changed')
    expect(await readFile(path, 'utf8')).toBe('replacement')
  })

  it('handles case-only renames and moving into another selected source name', async () => {
    const a = await file('a.txt', 'alpha'); const aa = await file('aa.txt', 'double')
    let files = await service.select(1, [a, aa])
    const preview = await service.preview(1, files.map((file) => file.id), { ...defaultRenameOptions, find: 'a', replacement: 'aa' })
    await service.execute(1, preview.token)
    expect(await readFile(join(directory, 'aa.txt'), 'utf8')).toBe('alpha')
    expect(await readFile(join(directory, 'aaaa.txt'), 'utf8')).toBe('double')
    await service.undo(1)
    files = await service.select(1, [a])
    await service.execute(1, (await service.preview(1, [files[0].id], { ...defaultRenameOptions, find: 'a', replacement: 'A' })).token)
    expect(await readdir(directory)).toContain('A.txt')
    await service.undo(1)
    expect(await readdir(directory)).toContain('a.txt')
  })

  it('never overwrites a target created after preflight and rolls back completed staging', async () => {
    const path = await file('a.txt', 'original')
    const target = join(directory, 'new_a.txt')
    const injected = new BatchRenameService(join(directory, 'state'), {
      link: async (source, destination) => { if (destination === target) await writeFile(target, 'foreign'); await link(source, destination) }, unlink
    })
    const files = await injected.select(1, [path])
    const preview = await injected.preview(1, [files[0].id], { ...defaultRenameOptions, prefix: 'new_' })
    await expect(injected.execute(1, preview.token)).rejects.toThrow()
    expect(await readFile(path, 'utf8')).toBe('original')
    expect(await readFile(target, 'utf8')).toBe('foreign')
    expect((await injected.status()).undoAvailable).toBe(false)
    expect((await readdir(directory)).some((name) => name.startsWith('.mootool-rename-'))).toBe(false)
  })

  it('recovers an interrupted batch after restart without losing file contents', async () => {
    const paths = [await file('a.txt', 'alpha'), await file('b.txt', 'beta')]
    const injected = new BatchRenameService(join(directory, 'state'), {
      link: async (source, target) => {
        if (source === paths[1] || target === paths[0]) throw new Error('injected interruption')
        await link(source, target)
      }, unlink
    })
    const files = await injected.select(1, paths)
    const preview = await injected.preview(1, files.map((file) => file.id), { ...defaultRenameOptions, prefix: 'new_' })
    await expect(injected.execute(1, preview.token)).rejects.toThrow('recovery')
    expect((await injected.status()).recoveryRequired).toBe(true)
    const restarted = new BatchRenameService(join(directory, 'state'))
    await restarted.undo(2)
    expect(await readFile(paths[0], 'utf8')).toBe('alpha')
    expect(await readFile(paths[1], 'utf8')).toBe('beta')
    expect((await restarted.status()).recoveryRequired).toBe(false)
  })

  it('leaves originals and prior undo intact when hard links are unsupported', async () => {
    const path = await file('a.txt', 'original')
    const initial = await service.select(1, [path])
    await service.execute(1, (await service.preview(1, [initial[0].id], { ...defaultRenameOptions, prefix: 'one_' })).token)
    const renamed = join(directory, 'one_a.txt')
    const injected = new BatchRenameService(join(directory, 'state'), { link: async () => { throw Object.assign(new Error('not supported'), { code: 'ENOTSUP' }) }, unlink })
    const files = await injected.select(1, [renamed])
    await expect(injected.execute(1, (await injected.preview(1, [files[0].id], { ...defaultRenameOptions, prefix: 'new_' })).token)).rejects.toThrow('unsupported')
    expect(await readFile(renamed, 'utf8')).toBe('original')
    expect((await injected.status()).undoAvailable).toBe(true)
    await new BatchRenameService(join(directory, 'state')).undo(1)
    expect(await readFile(path, 'utf8')).toBe('original')
    expect((await injected.status()).undoAvailable).toBe(false)
  })

  it('refuses undo when an original name has been taken, preserving both files', async () => {
    const original = await file('a.txt', 'original')
    const files = await service.select(1, [original])
    await service.execute(1, (await service.preview(1, [files[0].id], { ...defaultRenameOptions, prefix: 'new_' })).token)
    await file('a.txt', 'foreign')
    await expect(service.undo(1)).rejects.toThrow('occupied')
    expect(await readFile(original, 'utf8')).toBe('foreign')
    expect(await readFile(join(directory, 'new_a.txt'), 'utf8')).toBe('original')
    expect((await service.status()).undoAvailable).toBe(true)
  })
})
