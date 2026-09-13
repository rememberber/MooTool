import { randomUUID } from 'node:crypto'
import { lstat, mkdir, readFile, rename, rm, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { isDeepStrictEqual } from 'node:util'
import { parse as parseToml, stringify as stringifyToml } from 'smol-toml'
import { applyEdits, modify, parse as parseJsonc, visit, type ParseError } from 'jsonc-parser'
export type Launch = { command: string; args: string[]; env: Record<string, string> }
const serverName = 'mootool'

export function readServer(source: string | null, toml: boolean): unknown {
  const parsed = toml ? parseClientToml(source ?? '') : parseJsonc(source ?? '{}')
  const servers = parsed?.[toml ? 'mcp_servers' : 'mcpServers']
  return record(servers) ? servers.mootool : undefined
}

export function manageServer(source: string, toml: boolean, desired: Launch | (Launch & { type: string }) | null, owned?: unknown): string {
  const existing = readServer(source, toml)
  if (existing === undefined) return desired ? (toml ? mergeToml(source, desired) : mergeJson(source, desired as Launch & { type: string }, true)) : source
  if (desired && isDeepStrictEqual(existing, desired)) return toml ? mergeToml(source, desired) : mergeJson(source, desired as Launch & { type: string }, true)
  if (!owned || !isDeepStrictEqual(existing, owned)) throw new Error('MooTool configuration was changed outside the installer. Preserve or rename that entry before continuing.')
  let next: string
  if (toml) {
    const block = stringifyToml({ mcp_servers: { mootool: owned } })
    const at = source.indexOf(block)
    if (at < 0 || source.indexOf(block, at + 1) >= 0) throw new Error('The managed TOML block was reformatted. Preserve or rename it before continuing.')
    next = source.slice(0, at) + source.slice(at + block.length)
    if (!isDeepStrictEqual(withoutServer(parseClientToml(source), 'mcp_servers'), withoutServer(parseClientToml(next), 'mcp_servers')) || readServer(next, true) !== undefined) throw new Error('Cannot preserve the existing TOML configuration')
    return desired ? mergeToml(next, desired) : next
  }
  // Validate duplicate keys and source syntax even when removing an entry.
  mergeJson(source, existing as Launch & { type: string }, true)
  next = applyEdits(source, modify(source, ['mcpServers', 'mootool'], desired ?? undefined, {}))
  if (!isDeepStrictEqual(withoutServer(parseJsonc(source), 'mcpServers'), withoutServer(parseJsonc(next), 'mcpServers'))) throw new Error('Cannot preserve the existing JSON configuration')
  return next
}

function withoutServer(parsed: Record<string, unknown>, key: string): Record<string, unknown> {
  const next = { ...parsed }
  if (record(next[key])) {
    const servers = { ...next[key] }
    delete servers.mootool
    if (Object.keys(servers).length) next[key] = servers
    else delete next[key]
  }
  return next
}
function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export function mergeToml(source: string, launch: Launch): string {
  const parsed = parseClientToml(source)
  const servers = parsed.mcp_servers
  if (servers !== undefined && !record(servers)) throw new Error('mcp_servers must be a TOML table')
  if (record(servers) && Object.hasOwn(servers, serverName)) {
    if (isDeepStrictEqual(servers[serverName], launch)) return source
    throw new Error('An MCP server named mootool already exists with different settings. Rename or remove that entry before installing.')
  }
  const result = `${source}${source.endsWith('\n') || !source ? '' : '\n'}\n${stringifyToml({ mcp_servers: { [serverName]: launch } })}`
  // Also rejects inline-table / dotted-key conflicts before touching the file.
  const next = parseClientToml(result)
  if (!isDeepStrictEqual(next, { ...parsed, mcp_servers: { ...(record(servers) ? servers : {}), [serverName]: launch } })) throw new Error('Cannot preserve the existing TOML configuration')
  return result
}

function parseClientToml(source: string) {
  try { return parseToml(source) }
  // Parser diagnostics can contain source snippets (including credentials).
  catch { throw new Error('Cannot safely merge this TOML configuration. Check its syntax and inline mcp_servers tables, then refresh the preview.') }
}

export function mergeJson(source: string, launch: Launch & { type: string }, allowComments: boolean): string {
  const errors: ParseError[] = []
  const parsed: unknown = parseJsonc(source, errors, { allowTrailingComma: allowComments, disallowComments: !allowComments })
  if (errors.length || !record(parsed)) throw new Error('Invalid client JSON configuration')
  const objectKeys: Set<string>[] = []
  visit(source, {
    onObjectBegin: () => { objectKeys.push(new Set()) },
    onObjectProperty: (key) => {
      const keys = objectKeys[objectKeys.length - 1]
      if (keys.has(key)) throw new Error('Duplicate JSON configuration keys must be resolved before installation')
      keys.add(key)
    },
    onObjectEnd: () => { objectKeys.pop() }
  })
  const servers = parsed.mcpServers
  if (servers !== undefined && !record(servers)) throw new Error('mcpServers must be an object')
  if (record(servers) && Object.hasOwn(servers, serverName)) {
    if (isDeepStrictEqual(servers[serverName], launch)) return source
    throw new Error('An MCP server named mootool already exists with different settings. Rename or remove that entry before installing.')
  }
  const result = applyEdits(source, modify(source, ['mcpServers', serverName], launch, {
    formattingOptions: { insertSpaces: true, tabSize: 2, eol: source.includes('\r\n') ? '\r\n' : '\n' }
  }))
  if (!isDeepStrictEqual(parseJsonc(result), { ...parsed, mcpServers: { ...(record(servers) ? servers : {}), [serverName]: launch } })) throw new Error('Cannot preserve the existing JSON configuration')
  return result
}

export async function readOptional(path: string): Promise<string | null> {
  try {
    const info = await lstat(path)
    if (!info.isFile() || info.isSymbolicLink()) throw new Error(`Expected a regular configuration file: ${path}`)
    if (info.size > 4_000_000) throw new Error(`Configuration is too large: ${path}`)
    return await readFile(path, 'utf8')
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null
    throw error
  }
}

export async function atomicWrite(path: string, content: string): Promise<void> {
  await mkdir(dirname(path), { recursive: true, mode: 0o700 })
  const temporary = `${path}.mootool-tmp-${randomUUID()}`
  try {
    await writeFile(temporary, content, { flag: 'wx', mode: 0o600 })
    await rename(temporary, path)
  } finally {
    await rm(temporary, { force: true })
  }
}
