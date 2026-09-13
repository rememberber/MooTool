import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mkdtemp, mkdir, readFile, readdir, rm, symlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { parse as parseToml } from 'smol-toml'
import { parse as parseJsonc } from 'jsonc-parser'
import { AiIntegrationService, mergeJson, mergeToml } from './aiIntegrationService'
import * as fs from 'node:fs/promises'

vi.mock('node:fs/promises', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:fs/promises')>()
  return { ...actual, rename: vi.fn(actual.rename) }
})

const launch = { command: 'C:\\Apps\\Moo Tool\\MooTool.exe', args: ['C:\\Apps\\Moo Tool\\mcp.js'], env: { ELECTRON_RUN_AS_NODE: '1' } }

describe('client configuration merging', () => {
  it('preserves TOML comments, unrelated values, tables and quoted Windows paths', () => {
    const source = '# keep this\nmodel = "custom"\n[mcp_servers.other]\ncommand = "other"\n[profiles.work]\nmodel = "another"\n'
    const next = mergeToml(source, launch)
    expect(next.startsWith(source)).toBe(true)
    expect(parseToml(next)).toEqual({ model: 'custom', mcp_servers: { other: { command: 'other' }, mootool: launch }, profiles: { work: { model: 'another' } } })
    expect(mergeToml(next, launch)).toBe(next)
    expect(() => mergeToml('[mcp_servers.mootool]\nurl = "https://example.com"', launch)).toThrow('already exists')
    expect(() => mergeToml('mcp_servers = {}', launch)).toThrow()
    expect(() => mergeToml('model = "unfinished', launch)).toThrow()
  })

  it('preserves JSONC comments, trailing commas and other servers', () => {
    const source = '{\n  // preserve me\n  "custom": true,\n  "mcpServers": {"other": {"command": "existing"},},\n}'
    const value = { type: 'stdio', ...launch }
    const next = mergeJson(source, value, true)
    expect(next).toContain('// preserve me')
    expect(parseJsonc(next)).toEqual({ custom: true, mcpServers: { other: { command: 'existing' }, mootool: value } })
    expect(mergeJson(next, value, true)).toBe(next)
    for (const malformed of ['[]', '{"mcpServers":null}', '{"mcpServers":1}', '{"mcpServers":{},"mcpServers":{}}', '{"nested":{"a":1,"a":2}}', '{bad}']) {
      expect(() => mergeJson(malformed, value, true)).toThrow()
    }
    expect(() => mergeJson(source, value, false)).toThrow()
    expect(() => mergeJson('{"mcpServers":{"mootool":{"command":"custom"}}}', value, true)).toThrow('already exists')
  })
})

describe('one-click installation', () => {
  let home: string
  let entry: string
  let service: AiIntegrationService
  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'mootool-ai-test-'))
    entry = join(home, 'mcp.js')
    await writeFile(entry, '// test runtime')
    service = new AiIntegrationService({ home, entry, command: process.execPath, codexHome: join(home, 'custom-codex') })
    vi.spyOn(service, 'testConnection').mockResolvedValue({ serverName: 'MooTool', tools: ['mootool_json_format'] })
  })
  afterEach(async () => { vi.restoreAllMocks(); await rm(home, { recursive: true, force: true }) })

  it('previews without writing, installs both modes, backs up and is idempotent', async () => {
    const config = join(home, 'custom-codex', 'config.toml')
    const original = '# personal settings\nmodel = "private-model"\n'
    await mkdir(dirname(config), { recursive: true })
    await writeFile(config, original)
    const preview = await service.preview({ client: 'codex', mode: 'both' })
    expect(preview.files).toHaveLength(3)
    expect(JSON.stringify(preview)).not.toContain('private-model')
    expect(await readFile(config, 'utf8')).toBe(original)
    expect(preview.files[1].path).toBe(join(home, '.agents', 'skills', 'mootool', 'SKILL.md'))
    const result = await service.install(preview.id)
    expect(result.backups).toHaveLength(1)
    expect(await readFile(result.backups[0], 'utf8')).toBe(original)
    expect((parseToml(await readFile(config, 'utf8')).mcp_servers as Record<string, unknown>).mootool).toEqual(service.launch)
    const again = await service.preview({ client: 'codex', mode: 'both' })
    expect(again.files.every((file) => file.action === 'unchanged')).toBe(true)
    expect((await service.install(again.id)).backups).toEqual([])
    await expect(service.install(preview.id)).rejects.toThrow('expired')
  })

  it('does not install if the preview became stale or connection failed', async () => {
    const preview = await service.preview({ client: 'claude-code', mode: 'mcp' })
    await writeFile(preview.files[0].path, '{"updatedByClaude":true}')
    await expect(service.install(preview.id)).rejects.toThrow('changed since preview')
    expect(await readFile(preview.files[0].path, 'utf8')).toBe('{"updatedByClaude":true}')
    const fresh = await service.preview({ client: 'cursor', mode: 'mcp' })
    vi.mocked(service.testConnection).mockRejectedValue(new Error('Cannot start runtime'))
    await expect(service.install(fresh.id)).rejects.toThrow('Cannot start runtime')
    await expect(readFile(fresh.files[0].path)).rejects.toThrow()
  })

  it('supports standalone Skills and keeps existing custom skills intact', async () => {
    const preview = await service.preview({ client: 'claude-code', mode: 'skill' })
    await service.install(preview.id)
    expect(await readdir(home)).not.toContain('.claude.json')
    const skill = join(home, '.claude', 'skills', 'mootool', 'SKILL.md')
    await writeFile(skill, 'My own skill')
    await expect(service.preview({ client: 'claude-code', mode: 'both' })).rejects.toThrow('modified outside the installer')
    expect(await readFile(skill, 'utf8')).toBe('My own skill')
  })

  it('rolls back earlier writes if a later file cannot be installed', async () => {
    const config = join(home, 'custom-codex', 'config.toml')
    await mkdir(dirname(config), { recursive: true })
    await writeFile(config, 'model = "keep-me"\n')
    const preview = await service.preview({ client: 'codex', mode: 'both' })
    const real = await vi.importActual<typeof import('node:fs/promises')>('node:fs/promises')
    vi.mocked(fs.rename).mockImplementation(async (from, to) => {
      if (String(to).endsWith('SKILL.md')) throw new Error('Disk write failed')
      return real.rename(from, to)
    })
    await expect(service.install(preview.id)).rejects.toThrow('Disk write failed')
    expect(await readFile(config, 'utf8')).toBe('model = "keep-me"\n')
    await expect(readFile(preview.files[1].path)).rejects.toThrow()
    vi.mocked(fs.rename).mockImplementation(real.rename)
  })

  it('rejects symlink files, unsupported modes, arbitrary paths and invalid plan ids', async () => {
    const target = join(home, 'secret.json')
    await writeFile(target, '{}')
    await symlink(target, join(home, '.claude.json'))
    await expect(service.preview({ client: 'claude-code', mode: 'mcp' })).rejects.toThrow('regular configuration file')
    await expect(service.preview({ client: 'cursor', mode: 'skill' })).rejects.toThrow('MCP installation only')
    await expect(service.preview({ client: '../../other', mode: 'mcp' })).rejects.toThrow()
    await expect(service.preview({ client: 'codex', mode: 'mcp', path: '/tmp/unwanted' })).rejects.toThrow()
    await expect(service.install('unknown')).rejects.toThrow('expired')
  })

  it('repairs a moved runtime and missing files, then removes only managed content', async () => {
    await service.install((await service.preview({ client: 'codex', mode: 'both' })).id)
    expect(await service.getStatus('codex')).toMatchObject({ mcp: 'installed', skill: 'installed' })
    const movedEntry = join(home, "moved app's runtime.js")
    await writeFile(movedEntry, '// moved')
    const moved = new AiIntegrationService({ home, entry: movedEntry, command: process.execPath, codexHome: join(home, 'custom-codex') })
    vi.spyOn(moved, 'testConnection').mockResolvedValue({ serverName: 'MooTool', tools: [] })
    expect(await moved.getStatus('codex')).toMatchObject({ mcp: 'needs-repair', skill: 'needs-repair' })
    await rm(join(home, '.agents', 'skills', 'mootool', 'SKILL.md'))
    await moved.install((await moved.preview({ client: 'codex', mode: 'both' })).id)
    expect(await moved.getStatus('codex')).toMatchObject({ mcp: 'installed', skill: 'installed' })
    const config = join(home, 'custom-codex', 'config.toml')
    await writeFile(config, `# keep my comment\nmodel = "user-model"\n${await readFile(config, 'utf8')}`)
    await writeFile(join(home, '.agents', 'skills', 'mootool', 'my-resource.txt'), 'user resource')
    vi.mocked(moved.testConnection).mockRejectedValue(new Error('Runtime unavailable'))
    await moved.install((await moved.preview({ client: 'codex', mode: 'both', operation: 'uninstall' })).id)
    expect(parseToml(await readFile(config, 'utf8'))).toEqual({ model: 'user-model' })
    expect(await readdir(join(home, '.agents', 'skills', 'mootool'))).toContain('my-resource.txt')
    expect(await moved.getStatus('codex')).toMatchObject({ mcp: 'not-installed', skill: 'not-installed' })
  })

  it('detects user changes and refuses repair or uninstall instead of discarding them', async () => {
    await service.install((await service.preview({ client: 'claude-code', mode: 'mcp' })).id)
    const config = join(home, '.claude.json')
    const original = JSON.parse(await readFile(config, 'utf8'))
    original.mcpServers.mootool.args.push('--user-option')
    await writeFile(config, JSON.stringify(original))
    expect(await service.getStatus('claude-code')).toMatchObject({ mcp: 'conflict' })
    for (const operation of ['install', 'uninstall']) await expect(service.preview({ client: 'claude-code', mode: 'mcp', operation })).rejects.toThrow('changed outside')
    expect(JSON.parse(await readFile(config, 'utf8'))).toEqual(original)
  })
})
