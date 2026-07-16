// @vitest-environment node
import { mkdtemp, mkdir, readFile, rm, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { QuickNoteVaultRepository } from '../../electron/main/quickNoteVaultRepository'

const directories: string[] = []

afterEach(async () => {
  await Promise.all(directories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

async function createRepository(): Promise<{ directory: string; repository: QuickNoteVaultRepository }> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-quick-note-'))
  directories.push(directory)
  return { directory, repository: new QuickNoteVaultRepository(directory) }
}

describe('QuickNoteVaultRepository', () => {
  it('creates Java-compatible frontmatter and supports nested CRUD', async () => {
    const { directory, repository } = await createRepository()
    await repository.list()
    await repository.createFolder('Work')
    const note = await repository.create({ title: 'API ideas', parentPath: 'Work', fontSize: 15 })
    const saved = await repository.save({ ...note, content: '# Hello\nneedle' })

    expect(saved.relativePath).toBe('Work/API ideas.txt')
    expect(saved.metadata.fontName).toBe('ui-monospace')
    expect(saved.metadata.fontSize).toBe(15)
    expect(await readFile(join(directory, saved.relativePath), 'utf8')).toContain('font_name: ui-monospace')
    expect(await readFile(join(directory, saved.relativePath), 'utf8')).toContain('font_size: "15"')
    expect((await repository.list({ keyword: 'needle' }))[0].children).toHaveLength(1)

    const renamed = await repository.renameEntry({ relativePath: saved.relativePath, name: 'API notes' })
    expect(renamed).toBe('Work/API notes.txt')
    const duplicate = await repository.duplicate(renamed)
    expect(duplicate.relativePath).toBe('Work/API notes Copy.txt')
    const moved = await repository.moveEntry({ relativePath: duplicate.relativePath, targetDirectory: '' })
    expect(moved).toBe('API notes Copy.txt')
    await repository.delete(moved)
    await expect(repository.read(moved)).rejects.toThrow()
  })

  it('reads legacy files and honors gitignore filtering', async () => {
    const { directory, repository } = await createRepository()
    await mkdir(join(directory, 'Private'))
    await writeFile(join(directory, 'Legacy.txt'), '---\ntitle: Legacy title\nsyntax: text/markdown\nline_wrap: "0"\n---\nlegacy body')
    await writeFile(join(directory, 'Private', 'Secret.txt'), 'secret')
    await writeFile(join(directory, '.gitignore'), 'Private/\n')

    const note = await repository.read('Legacy.txt')
    expect(note.content).toBe('legacy body')
    expect(note.metadata.title).toBe('Legacy title')
    expect(note.metadata.lineWrap).toBe(false)
    expect(await repository.list({ hideIgnored: true })).toHaveLength(1)
    expect(await repository.list({ hideIgnored: false })).toHaveLength(2)
  })

  it('rejects traversal and non-empty folder deletion', async () => {
    const { repository } = await createRepository()
    await expect(repository.read('../outside.txt')).rejects.toThrow('Invalid Vault path')
    await repository.createFolder('Folder')
    await repository.create({ title: 'Note', parentPath: 'Folder' })
    await expect(repository.delete('Folder')).rejects.toThrow('empty')
  })

  it('cleans orphaned image attachments when the last reference is removed', async () => {
    const { directory, repository } = await createRepository()
    const image = join(directory, 'source.png')
    await writeFile(image, 'png')
    const attachment = await repository.importAttachment(image)
    const note = await repository.create({ title: 'Attachment note' })
    const saved = await repository.save({ ...note, content: attachment.markdown })
    expect(await repository.readAttachment(attachment.relativePath)).toMatch(/^data:image\/png;base64,/)
    await repository.save({ ...saved, content: '' })
    await expect(repository.readAttachment(attachment.relativePath)).rejects.toThrow()
  })

  it('does not follow an existing note symlink while saving', async () => {
    if (process.platform === 'win32') return
    const { directory, repository } = await createRepository()
    const outside = join(directory, 'outside.txt')
    await writeFile(outside, 'outside')
    await symlink(outside, join(directory, 'Linked.txt'))
    const now = new Date().toISOString()
    await expect(repository.save({
      relativePath: 'Linked.txt',
      content: 'unsafe',
      metadata: { title: 'Linked', style: '', syntax: 'text/plain', fontName: '', fontSize: 14, color: 'default', lineWrap: true, createdAt: now, modifiedAt: now }
    })).rejects.toThrow('Invalid Quick Note file')
    expect(await readFile(outside, 'utf8')).toBe('outside')
  })
})
