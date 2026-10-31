import { mkdtemp, mkdir, readFile, rm, truncate, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'
import { pathToFileURL } from 'node:url'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ExternalFileOpenQueue, externalFileSizeLimit, externalFilesFromArgv, readExternalTextFile } from './externalFileOpen'
import { JsonVaultRepository } from './jsonVaultRepository'
import { externalFileSyntax } from './externalFileOpen'
import packageJson from '../../package.json'

const roots: string[] = []
async function temporaryDirectory() {
  const root = await mkdtemp(join(tmpdir(), 'mootool-file-open-'))
  roots.push(root)
  return root
}
afterEach(async () => { await Promise.all(roots.splice(0).map(root => rm(root, { recursive: true, force: true }))) })

describe('external document files', () => {
  it('parses initial and second-instance arguments, preserving spaces and resolving relative paths', () => {
    const cwd = resolve('example')
    expect(externalFilesFromArgv(['MooTool', '--flag', '--user-data-dir=a.json', '中文 a.JSON', './b.md', './b.md', 'https://x', 'bad.constructor'], cwd, false))
      .toEqual([join(cwd, '中文 a.JSON'), join(cwd, 'b.md')])
    expect(externalFilesFromArgv(['electron', 'app.json', 'doc.json'], cwd, true)).toEqual([join(cwd, 'doc.json')])
    const file = join(cwd, '中文 with spaces.json')
    expect(externalFilesFromArgv(['MooTool', pathToFileURL(file).href, file, 'https://example.com/doc.json', 'file://%'], cwd, false)).toEqual([file])
  })

  it('reads UTF-8 and BOM-marked UTF-16, including empty and malformed JSON text', async () => {
    const root = await temporaryDirectory()
    for (const [name, bytes, content] of [
      ['中文.json', Buffer.from('\ufeff{"name":"你好"}'), '{"name":"你好"}'],
      ['empty.txt', Buffer.from(''), ''],
      ['note.md', Buffer.concat([Buffer.from([0xff, 0xfe]), Buffer.from('# 笔记', 'utf16le')]), '# 笔记'],
      ['be.xml', Buffer.from([0xfe, 0xff, 0, 60, 0, 120, 0, 47, 0, 62]), '<x/>'],
      ['broken.jsonc', Buffer.from('{ /* comment */'), '{ /* comment */']
    ] as const) {
      const path = join(root, name)
      await writeFile(path, bytes)
      expect(await readExternalTextFile(path)).toMatchObject({ path, name, content })
      expect(await readFile(path)).toEqual(bytes)
    }
    expect((await readExternalTextFile(join(root, 'note.md'))).syntax).toBe('text/markdown')
  })

  it('rejects missing files, directories, binary, undecodable and oversized input', async () => {
    const root = await temporaryDirectory()
    await expect(readExternalTextFile(join(root, 'missing.txt'))).rejects.toThrow()
    await mkdir(join(root, 'directory.json'))
    await expect(readExternalTextFile(join(root, 'directory.json'))).rejects.toThrow('fileOpen.unreadable')
    for (const bytes of [Buffer.from([0, 1]), Buffer.from([0xff, 0xff])]) {
      await writeFile(join(root, 'binary.txt'), bytes)
      await expect(readExternalTextFile(join(root, 'binary.txt'))).rejects.toThrow('fileOpen.encoding')
    }
    await expect(readExternalTextFile(join(root, 'photo.png'))).rejects.toThrow('fileOpen.unsupported')
    await expect(readExternalTextFile(join(root, 'bad.constructor'))).rejects.toThrow('fileOpen.unsupported')
    const large = join(root, 'large.json')
    await writeFile(large, '')
    await truncate(large, externalFileSizeLimit + 1)
    await expect(readExternalTextFile(large)).rejects.toThrow('fileOpen.tooLarge')
  })

  it('keeps the original JSON name and creates separate copies on repeat imports', async () => {
    const root = await temporaryDirectory()
    const repository = new JsonVaultRepository(root)
    expect((await repository.createDefault('first', '例子.json')).relativePath).toBe('例子.json')
    expect((await repository.createDefault('second', '例子.json')).relativePath).toBe('例子 (2).json')
    expect((await repository.read('例子.json')).content).toBe('first')
    expect((await repository.createDefault()).relativePath).toBe('snippet.json')
  })

  it('registers the same supported extensions on every packaged platform', async () => {
    const expected = Object.keys(externalFileSyntax).sort()
    for (const config of [packageJson.build.mac, packageJson.build.linux]) {
      expect(config.fileAssociations.flatMap(item => item.ext).sort()).toEqual(expected)
    }
    const script = await readFile(new URL('../../resources/file-associations.nsh', import.meta.url), 'utf8')
    expect([...script.matchAll(/!insertmacro \$\{ACTION\} "(\w+)"/g)].map(match => match[1]).sort()).toEqual(expected)
    expect(script).toContain('OpenWithProgids')
    expect(script).toContain('!macro customUnInstall')
    expect(packageJson.build.nsis.include).toBe('resources/file-associations.nsh')
  })
})

describe('OS open queue', () => {
  it('waits for renderer readiness, serializes events, deduplicates pending paths, and accepts a later reopen', async () => {
    let finish!: () => void
    const first = new Promise<void>(resolve => { finish = resolve })
    const process = vi.fn().mockImplementationOnce(() => first).mockResolvedValue(undefined)
    const error = vi.fn().mockResolvedValue(undefined)
    const queue = new ExternalFileOpenQueue(process, error)
    const a = resolve('a.json'), b = resolve('b.md')
    queue.enqueue([a, a, b])
    expect(process).not.toHaveBeenCalled()
    queue.setReady(true)
    expect(process).toHaveBeenCalledExactlyOnceWith(a)
    queue.enqueue([a, b])
    finish()
    await vi.waitFor(() => expect(process).toHaveBeenCalledTimes(2))
    expect(process.mock.calls[1]).toEqual([b])
    await Promise.resolve()
    queue.enqueue([a])
    await vi.waitFor(() => expect(process).toHaveBeenCalledTimes(3))
    expect(error).not.toHaveBeenCalled()
  })

  it('pauses during a renderer reload and continues after failures', async () => {
    const process = vi.fn().mockRejectedValueOnce(new Error('unreadable')).mockResolvedValue(undefined)
    const error = vi.fn(async () => { queue.setReady(false) })
    const queue = new ExternalFileOpenQueue(process, error)
    queue.enqueue(['missing.txt', 'good.json'])
    queue.setReady(true)
    await vi.waitFor(() => expect(error).toHaveBeenCalledOnce())
    expect(process).toHaveBeenCalledTimes(1)
    queue.setReady(true)
    await vi.waitFor(() => expect(process).toHaveBeenCalledTimes(2))
    expect(process.mock.calls[1]).toEqual([resolve('good.json')])
  })
})
