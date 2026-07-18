import { mkdir, mkdtemp, realpath, rm, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { NativeMemoryService } from '../../electron/main/ai/nativeMemoryService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('NativeMemoryService', () => {
  it('discovers documented Claude Auto Memory roots read-only with entrypoint limits and secret redaction', async () => {
    const home = await fixtureRoot()
    const standard = join(home, '.claude', 'projects', '-repo', 'memory')
    const custom = join(home, 'custom-memory')
    await Promise.all([mkdir(standard, { recursive: true }), mkdir(custom, { recursive: true })])
    await writeFile(join(home, '.claude', 'settings.json'), JSON.stringify({ autoMemoryDirectory: '~/custom-memory' }))
    const lines = Array.from({ length: 240 }, (_, index) => index === 1 ? 'api_key = "sk-native-memory-secret-123456789"' : `line ${index}`)
    await writeFile(join(standard, 'MEMORY.md'), lines.join('\n'))
    await writeFile(join(standard, 'debugging.md'), '# Debugging\nUse the fixture command.')
    await writeFile(join(custom, 'MEMORY.md'), '# Custom memory')
    await writeFile(join(custom, 'ignored.txt'), 'not markdown')

    const snapshot = await new NativeMemoryService({ homeDirectory: home }).scan()

    expect(snapshot.readOnly).toBe(true)
    expect(snapshot.roots).toHaveLength(2)
    expect(snapshot.artifacts).toHaveLength(3)
    const entryPath = await realpath(join(standard, 'MEMORY.md'))
    const entry = snapshot.artifacts.find((artifact) => artifact.path === entryPath)!
    expect(entry).toMatchObject({ clientId: 'claudeCode', source: 'claudeAutoMemory', projectKey: '-repo', entrypoint: true, excerptTruncated: true })
    expect(entry.sensitiveFindings).toBeGreaterThan(0)
    expect(entry.contentExcerpt).toContain('[REDACTED]')
    expect(JSON.stringify(snapshot)).not.toContain('sk-native-memory-secret')
    expect(entry.contentExcerpt).not.toContain('line 239')
  })

  it('skips oversized files and symbolic links and reports malformed user settings without following them', async () => {
    const home = await fixtureRoot()
    const memory = join(home, '.claude', 'projects', '-repo', 'memory')
    const outside = join(home, 'outside.md')
    await mkdir(memory, { recursive: true })
    await writeFile(join(home, '.claude', 'settings.json'), '{broken')
    await writeFile(join(memory, 'large.md'), 'x'.repeat(2_000))
    await writeFile(outside, 'outside')
    if (process.platform !== 'win32') await symlink(outside, join(memory, 'linked.md'))

    const snapshot = await new NativeMemoryService({ homeDirectory: home, maxFileBytes: 1_000 }).scan()

    expect(snapshot.artifacts).toEqual([])
    expect(snapshot.diagnostics).toContainEqual(expect.objectContaining({ message: expect.stringContaining('settings could not be parsed') }))
    expect(snapshot.diagnostics).toContainEqual(expect.objectContaining({ message: expect.stringContaining('exceeds 1000 bytes') }))
  })
})

async function fixtureRoot(): Promise<string> {
  const root = await mkdtemp(join(tmpdir(), 'mootool-native-memory-'))
  temporaryDirectories.push(root)
  await mkdir(join(root, '.claude'), { recursive: true })
  return root
}
