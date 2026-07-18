import { createHash } from 'node:crypto'
import type { AiClientId, AiScope } from '../../../src/shared/contracts/ai'
import type { AiMcpNamedReference, AiMcpRisk, AiMcpServer, AiMcpTransport } from '../../../src/shared/contracts/aiMcp'

export type ParsedMcpServer = {
  name: string
  transport: AiMcpTransport
  enabled: boolean
  command?: string
  args: string[]
  url?: string
  environment: Record<string, string>
  environmentReferences: Record<string, string>
  headers: Record<string, string>
  headerReferences: Record<string, string>
  bearerTokenEnvironmentVariable?: string
  oauth: boolean
  startupTimeoutMs?: number
  toolTimeoutMs?: number
}

export function parseClaudeMcpConfig(source: string): ParsedMcpServer[] {
  const parsed = JSON.parse(source) as unknown
  if (!isRecord(parsed)) throw new Error('Claude MCP configuration must be a JSON object')
  if (parsed.mcpServers !== undefined && !isRecord(parsed.mcpServers)) throw new Error('Claude mcpServers must be an object')
  if (parsed.servers !== undefined && !isRecord(parsed.servers)) throw new Error('MCP servers must be an object')
  const servers = {
    ...(isRecord(parsed.servers) ? parsed.servers : {}),
    ...(isRecord(parsed.mcpServers) ? parsed.mcpServers : {})
  }
  return Object.entries(servers).map(([name, value]) => parseClaudeServer(name, value))
}

export function parseCodexMcpConfig(source: string): ParsedMcpServer[] {
  const servers = new Map<string, ParsedMcpServer>()
  let current: { name: string; subtable: string } | undefined
  for (const statement of collectTomlStatements(source)) {
    const section = statement.match(/^\[\s*(.+?)\s*]$/s)
    if (section) {
      const keys = parseTomlDottedKey(section[1])
      current = keys[0] === 'mcp_servers' && keys.length >= 2 ? { name: keys[1], subtable: keys.slice(2).join('.') } : undefined
      if (current && !servers.has(current.name)) servers.set(current.name, emptyServer(current.name))
      continue
    }
    if (!current) continue
    const assignment = splitTomlAssignment(statement)
    if (!assignment) continue
    const server = servers.get(current.name)!
    const key = unquoteToml(assignment.key.trim())
    if (current.subtable === 'env') server.environment[key] = parseTomlString(assignment.value)
    else if (current.subtable === 'http_headers') server.headers[key] = parseTomlString(assignment.value)
    else if (current.subtable === 'env_http_headers') server.headerReferences[key] = parseTomlString(assignment.value)
    else if (!current.subtable) applyCodexValue(server, key, assignment.value)
  }
  return [...servers.values()].map((server) => ({ ...server, transport: inferTransport(server) }))
}

export function sanitizeMcpServer(parsed: ParsedMcpServer, clientId: AiClientId, scope: AiScope, configPath: string): AiMcpServer {
  const environment = namedReferences(parsed.environment, parsed.environmentReferences)
  const headers = namedReferences(parsed.headers, parsed.headerReferences)
  if (parsed.bearerTokenEnvironmentVariable) {
    headers.push({ name: 'Authorization', source: 'environment', reference: parsed.bearerTokenEnvironmentVariable, sensitive: true })
  }
  if (parsed.oauth) headers.push({ name: 'OAuth', source: 'oauth', sensitive: true })
  const args = redactArguments(parsed.args)
  const url = redactUrl(parsed.url)
  const risks = collectRisks(parsed, environment, headers, args)
  return {
    id: stableId(clientId, scope, configPath, parsed.name),
    clientId,
    scope,
    name: parsed.name,
    configPath,
    transport: parsed.transport,
    enabled: parsed.enabled,
    command: parsed.command,
    args,
    url,
    environment,
    headers,
    startupTimeoutMs: parsed.startupTimeoutMs,
    toolTimeoutMs: parsed.toolTimeoutMs,
    risks
  }
}

function parseClaudeServer(name: string, value: unknown): ParsedMcpServer {
  if (!isRecord(value)) throw new Error(`Claude MCP server ${name} must be an object`)
  const type = readOptionalString(value.type)
  const command = readOptionalString(value.command)
  const url = readOptionalString(value.url)
  const transport: AiMcpTransport = type === 'sse' ? 'legacySse' : type === 'http' || type === 'streamable-http' ? 'streamableHttp' : command ? 'stdio' : url ? 'streamableHttp' : 'unknown'
  const rawEnvironment = readStringMap(value.env, `${name}.env`)
  const rawHeaders = readStringMap(value.headers, `${name}.headers`)
  const [environment, environmentReferences] = splitEnvironmentReferences(rawEnvironment)
  const [headers, headerReferences] = splitEnvironmentReferences(rawHeaders)
  return {
    name,
    transport,
    enabled: value.disabled !== true,
    command,
    args: readStringArray(value.args, `${name}.args`),
    url,
    environment,
    environmentReferences,
    headers,
    headerReferences,
    oauth: isRecord(value.oauth),
    startupTimeoutMs: readOptionalNumber(value.startupTimeout) ?? readOptionalNumber(value.timeout),
    toolTimeoutMs: undefined
  }
}

function applyCodexValue(server: ParsedMcpServer, key: string, rawValue: string): void {
  if (key === 'command') server.command = parseTomlString(rawValue)
  else if (key === 'url') server.url = parseTomlString(rawValue)
  else if (key === 'args') server.args = parseTomlStringArray(rawValue)
  else if (key === 'enabled') server.enabled = rawValue.trim() !== 'false'
  else if (key === 'startup_timeout_ms') server.startupTimeoutMs = parseTomlNumber(rawValue)
  else if (key === 'startup_timeout_sec') server.startupTimeoutMs = parseTomlNumber(rawValue) * 1_000
  else if (key === 'tool_timeout_sec') server.toolTimeoutMs = parseTomlNumber(rawValue) * 1_000
  else if (key === 'bearer_token_env_var') server.bearerTokenEnvironmentVariable = parseTomlString(rawValue)
  else if (key === 'env_vars') {
    for (const name of parseTomlStringArray(rawValue)) server.environmentReferences[name] = name
  } else if (key === 'env') Object.assign(server.environment, parseTomlStringMap(rawValue))
  else if (key === 'http_headers') Object.assign(server.headers, parseTomlStringMap(rawValue))
  else if (key === 'env_http_headers') Object.assign(server.headerReferences, parseTomlStringMap(rawValue))
}

function emptyServer(name: string): ParsedMcpServer {
  return { name, transport: 'unknown', enabled: true, args: [], environment: {}, environmentReferences: {}, headers: {}, headerReferences: {}, oauth: false }
}

function inferTransport(server: ParsedMcpServer): AiMcpTransport {
  if (server.command) return 'stdio'
  if (server.url) return 'streamableHttp'
  return 'unknown'
}

function namedReferences(literals: Record<string, string>, references: Record<string, string>): AiMcpNamedReference[] {
  const names = new Set([...Object.keys(literals), ...Object.keys(references)])
  return [...names].sort().map((name) => {
    const reference = references[name]
    const value = literals[name]
    return {
      name,
      source: reference ? 'environment' : 'literal',
      ...(reference ? { reference } : {}),
      sensitive: isSensitiveName(name) || (value ? looksSensitive(value) : false)
    }
  })
}

function collectRisks(parsed: ParsedMcpServer, environment: AiMcpNamedReference[], headers: AiMcpNamedReference[], redactedArgs: string[]): AiMcpRisk[] {
  const risks = new Set<AiMcpRisk>()
  if ([...environment, ...headers].some((item) => item.source === 'literal' && item.sensitive)) risks.add('plaintextSecret')
  if (redactedArgs.some((argument) => argument === '[REDACTED]')) risks.add('sensitiveArgument')
  if (parsed.command && /(?:^|[\\/])(?:ba|z|c|k)?sh(?:\.exe)?$|(?:^|[\\/])(?:cmd|powershell|pwsh)(?:\.exe)?$/i.test(parsed.command)) risks.add('shellLauncher')
  if (parsed.transport === 'legacySse') risks.add('legacyTransport')
  if (parsed.transport === 'unknown') risks.add('unknownTransport')
  if (parsed.url && isInsecureRemoteUrl(parsed.url)) risks.add('insecureRemoteHttp')
  return [...risks]
}

function redactArguments(args: string[]): string[] {
  let redactNext = false
  return args.map((argument) => {
    if (redactNext) {
      redactNext = false
      return '[REDACTED]'
    }
    if (/^--?(?:api[-_]?key|token|secret|password|authorization)$/i.test(argument)) {
      redactNext = true
      return argument
    }
    if (/^--?(?:api[-_]?key|token|secret|password|authorization)=/i.test(argument) || looksSensitive(argument)) return '[REDACTED]'
    return argument
  })
}

function redactUrl(value?: string): string | undefined {
  if (!value) return undefined
  try {
    const url = new URL(value)
    for (const key of url.searchParams.keys()) url.searchParams.set(key, '[REDACTED]')
    if (url.username) url.username = '[REDACTED]'
    if (url.password) url.password = '[REDACTED]'
    return url.toString()
  } catch {
    return '[INVALID URL]'
  }
}

function isInsecureRemoteUrl(value: string): boolean {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' && !['127.0.0.1', 'localhost', '::1'].includes(url.hostname)
  } catch {
    return false
  }
}

function splitEnvironmentReferences(values: Record<string, string>): [Record<string, string>, Record<string, string>] {
  const literals: Record<string, string> = {}
  const references: Record<string, string> = {}
  for (const [name, value] of Object.entries(values)) {
    const match = value.match(/\$\{([A-Z_][A-Z0-9_]*)(?::-.*?)?}/)
    if (match) references[name] = match[1]
    else literals[name] = value
  }
  return [literals, references]
}

export function addClaudeMcpServer(source: string | undefined, name: string, definition: Record<string, unknown>): string {
  const initial = source ?? '{\n  "mcpServers": {}\n}\n'
  const parsed = JSON.parse(initial) as unknown
  if (!isRecord(parsed)) throw new Error('Claude MCP configuration must be a JSON object')
  if (isRecord(parsed.mcpServers) && Object.hasOwn(parsed.mcpServers, name)) throw new Error(`MCP server already exists in target: ${name}`)
  if (parsed.mcpServers !== undefined && !isRecord(parsed.mcpServers)) throw new Error('Claude mcpServers must be an object')

  const mcpObject = locateTopLevelJsonObjectProperty(initial, 'mcpServers')
  if (mcpObject) return insertJsonProperty(initial, mcpObject.start, mcpObject.end, name, definition)
  const rootStart = initial.indexOf('{')
  if (rootStart < 0) throw new Error('Claude MCP configuration must be a JSON object')
  const rootEnd = matchingJsonDelimiter(initial, rootStart, '{', '}')
  return insertJsonProperty(initial, rootStart, rootEnd, 'mcpServers', { [name]: definition })
}

export function addCodexMcpServer(source: string | undefined, name: string, definition: ParsedMcpServer, mappings: {
  environmentReferences: Record<string, string>
  headerReferences: Record<string, string>
}): string {
  const initial = source ?? ''
  if (parseCodexMcpConfig(initial).some((server) => server.name === name)) throw new Error(`MCP server already exists in target: ${name}`)
  const lines = [`[mcp_servers.${tomlKey(name)}]`]
  if (definition.transport === 'stdio') {
    if (!definition.command) throw new Error('A stdio MCP server requires a command')
    lines.push(`command = ${tomlString(definition.command)}`)
    if (definition.args.length > 0) lines.push(`args = [${definition.args.map(tomlString).join(', ')}]`)
    if (Object.keys(definition.environment).length > 0) lines.push(`env = ${tomlMap(definition.environment)}`)
    const references = Object.values(mappings.environmentReferences)
    if (references.length > 0) lines.push(`env_vars = [${[...new Set(references)].map(tomlString).join(', ')}]`)
  } else if (definition.transport === 'streamableHttp') {
    if (!definition.url) throw new Error('An HTTP MCP server requires a URL')
    lines.push(`url = ${tomlString(definition.url)}`)
    const authorization = mappings.headerReferences.Authorization
    if (authorization) lines.push(`bearer_token_env_var = ${tomlString(authorization)}`)
    const otherHeaders = Object.fromEntries(Object.entries(definition.headers).filter(([key]) => key !== 'Authorization'))
    if (Object.keys(otherHeaders).length > 0) lines.push(`http_headers = ${tomlMap(otherHeaders)}`)
    const otherReferences = Object.fromEntries(Object.entries(mappings.headerReferences).filter(([key]) => key !== 'Authorization'))
    if (Object.keys(otherReferences).length > 0) lines.push(`env_http_headers = ${tomlMap(otherReferences)}`)
  } else {
    throw new Error('Only stdio and Streamable HTTP MCP servers can be copied')
  }
  if (!definition.enabled) lines.push('enabled = false')
  if (definition.startupTimeoutMs !== undefined) lines.push(`startup_timeout_ms = ${Math.round(definition.startupTimeoutMs)}`)
  if (definition.toolTimeoutMs !== undefined) lines.push(`tool_timeout_sec = ${Math.round(definition.toolTimeoutMs / 1_000)}`)
  return `${initial.trimEnd()}${initial.trim() ? '\n\n' : ''}${lines.join('\n')}\n`
}

function locateTopLevelJsonObjectProperty(source: string, property: string): { start: number; end: number } | undefined {
  const rootStart = source.indexOf('{')
  if (rootStart < 0) return undefined
  let depth = 0
  let index = rootStart
  while (index < source.length) {
    const character = source[index]
    if (character === '"') {
      const tokenEnd = jsonStringEnd(source, index)
      if (depth === 1) {
        const key = JSON.parse(source.slice(index, tokenEnd + 1)) as string
        let cursor = skipWhitespace(source, tokenEnd + 1)
        if (source[cursor] === ':') {
          cursor = skipWhitespace(source, cursor + 1)
          if (key === property) {
            if (source[cursor] !== '{') throw new Error(`${property} must be a JSON object`)
            return { start: cursor, end: matchingJsonDelimiter(source, cursor, '{', '}') }
          }
        }
      }
      index = tokenEnd + 1
      continue
    }
    if (character === '{' || character === '[') depth += 1
    else if (character === '}' || character === ']') depth -= 1
    index += 1
  }
  return undefined
}

function insertJsonProperty(source: string, objectStart: number, objectEnd: number, name: string, value: unknown): string {
  let contentEnd = objectEnd - 1
  while (contentEnd > objectStart && /\s/.test(source[contentEnd])) contentEnd -= 1
  const hasContent = contentEnd > objectStart
  const closeIndent = lineIndent(source, objectEnd)
  const propertyIndent = `${closeIndent}  `
  const serializedValue = JSON.stringify(value, null, 2).replace(/\n/g, `\n${propertyIndent}`)
  const property = `${JSON.stringify(name)}: ${serializedValue}`
  return `${source.slice(0, contentEnd + 1)}${hasContent ? ',' : ''}\n${propertyIndent}${property}\n${closeIndent}${source.slice(objectEnd)}`
}

function matchingJsonDelimiter(source: string, start: number, open: string, close: string): number {
  let depth = 0
  for (let index = start; index < source.length; index += 1) {
    if (source[index] === '"') { index = jsonStringEnd(source, index); continue }
    if (source[index] === open) depth += 1
    else if (source[index] === close && --depth === 0) return index
  }
  throw new Error('Unclosed JSON object')
}

function jsonStringEnd(source: string, start: number): number {
  for (let index = start + 1; index < source.length; index += 1) {
    if (source[index] === '\\') index += 1
    else if (source[index] === '"') return index
  }
  throw new Error('Unclosed JSON string')
}

function skipWhitespace(source: string, start: number): number {
  let index = start
  while (index < source.length && /\s/.test(source[index])) index += 1
  return index
}

function lineIndent(source: string, index: number): string {
  const lineStart = source.lastIndexOf('\n', index - 1) + 1
  return source.slice(lineStart, index).match(/^\s*/)?.[0] ?? ''
}

function tomlKey(value: string): string {
  return /^[A-Za-z0-9_-]+$/.test(value) ? value : tomlString(value)
}

function tomlString(value: string): string {
  return JSON.stringify(value)
}

function tomlMap(values: Record<string, string>): string {
  return `{ ${Object.entries(values).map(([key, value]) => `${tomlKey(key)} = ${tomlString(value)}`).join(', ')} }`
}

function collectTomlStatements(source: string): string[] {
  const statements: string[] = []
  let current = ''
  let depth = 0
  let quote = ''
  let escaped = false
  for (const rawLine of source.split(/\r?\n/)) {
    const line = stripTomlComment(rawLine)
    if (!line.trim() && !current) continue
    current += `${current ? '\n' : ''}${line.trim()}`
    for (const character of line) {
      if (escaped) { escaped = false; continue }
      if (quote === '"' && character === '\\') { escaped = true; continue }
      if (quote) { if (character === quote) quote = ''; continue }
      if (character === '"' || character === "'") quote = character
      else if (character === '[' || character === '{') depth += 1
      else if (character === ']' || character === '}') depth -= 1
    }
    if (depth <= 0 && !quote) { statements.push(current.trim()); current = ''; depth = 0 }
  }
  if (current.trim()) statements.push(current.trim())
  return statements
}

function stripTomlComment(line: string): string {
  let quote = ''
  let escaped = false
  for (let index = 0; index < line.length; index += 1) {
    const character = line[index]
    if (escaped) { escaped = false; continue }
    if (quote === '"' && character === '\\') { escaped = true; continue }
    if (quote) { if (character === quote) quote = ''; continue }
    if (character === '"' || character === "'") quote = character
    else if (character === '#') return line.slice(0, index)
  }
  return line
}

function splitTomlAssignment(statement: string): { key: string; value: string } | undefined {
  let quote = ''
  for (let index = 0; index < statement.length; index += 1) {
    const character = statement[index]
    if (quote) { if (character === quote && statement[index - 1] !== '\\') quote = ''; continue }
    if (character === '"' || character === "'") quote = character
    else if (character === '=') return { key: statement.slice(0, index), value: statement.slice(index + 1).trim() }
  }
  return undefined
}

function parseTomlDottedKey(value: string): string[] {
  const keys: string[] = []
  let current = ''
  let quote = ''
  for (const character of value) {
    if (quote) { current += character; if (character === quote) quote = ''; continue }
    if (character === '"' || character === "'") { quote = character; current += character }
    else if (character === '.') { keys.push(unquoteToml(current.trim())); current = '' }
    else current += character
  }
  if (current.trim()) keys.push(unquoteToml(current.trim()))
  return keys
}

function parseTomlString(value: string): string {
  const trimmed = value.trim()
  if (trimmed.startsWith('"') && trimmed.endsWith('"')) return JSON.parse(trimmed) as string
  if (trimmed.startsWith("'") && trimmed.endsWith("'")) return trimmed.slice(1, -1)
  throw new Error('Expected a TOML string')
}

function parseTomlStringArray(value: string): string[] {
  const trimmed = value.trim()
  if (!trimmed.startsWith('[') || !trimmed.endsWith(']')) throw new Error('Expected a TOML array')
  const values: string[] = []
  for (const token of splitTopLevel(trimmed.slice(1, -1), ',')) {
    const item = token.trim()
    if (!item) continue
    if (item.startsWith('{')) {
      const map = parseTomlStringMap(item)
      if (typeof map.name === 'string') values.push(map.name)
    } else values.push(parseTomlString(item))
  }
  return values
}

function parseTomlStringMap(value: string): Record<string, string> {
  const trimmed = value.trim()
  if (!trimmed.startsWith('{') || !trimmed.endsWith('}')) throw new Error('Expected a TOML inline table')
  const output: Record<string, string> = {}
  for (const token of splitTopLevel(trimmed.slice(1, -1), ',')) {
    const assignment = splitTomlAssignment(token)
    if (assignment) output[unquoteToml(assignment.key.trim())] = parseTomlString(assignment.value)
  }
  return output
}

function splitTopLevel(value: string, separator: string): string[] {
  const output: string[] = []
  let current = ''
  let quote = ''
  let depth = 0
  for (let index = 0; index < value.length; index += 1) {
    const character = value[index]
    if (quote) {
      current += character
      if (character === quote && value[index - 1] !== '\\') quote = ''
      continue
    }
    if (character === '"' || character === "'") quote = character
    else if (character === '[' || character === '{') depth += 1
    else if (character === ']' || character === '}') depth -= 1
    if (character === separator && depth === 0) { output.push(current); current = '' } else current += character
  }
  output.push(current)
  return output
}

function unquoteToml(value: string): string {
  return value.startsWith('"') || value.startsWith("'") ? parseTomlString(value) : value
}

function parseTomlNumber(value: string): number {
  const number = Number(value.trim().replaceAll('_', ''))
  if (!Number.isFinite(number) || number < 0) throw new Error('Expected a non-negative TOML number')
  return number
}

function readStringMap(value: unknown, field: string): Record<string, string> {
  if (value === undefined) return {}
  if (!isRecord(value)) throw new Error(`${field} must be an object`)
  const output: Record<string, string> = {}
  for (const [key, item] of Object.entries(value)) {
    if (typeof item !== 'string') throw new Error(`${field}.${key} must be a string`)
    output[key] = item
  }
  return output
}

function readStringArray(value: unknown, field: string): string[] {
  if (value === undefined) return []
  if (!Array.isArray(value) || value.some((item) => typeof item !== 'string')) throw new Error(`${field} must be a string array`)
  return value as string[]
}

function readOptionalString(value: unknown): string | undefined {
  if (value === undefined) return undefined
  if (typeof value !== 'string') throw new Error('Expected a string')
  return value
}

function readOptionalNumber(value: unknown): number | undefined {
  return typeof value === 'number' && Number.isFinite(value) && value >= 0 ? value : undefined
}

function isSensitiveName(name: string): boolean {
  return /(?:api[_-]?key|token|secret|password|authorization|credential|private[_-]?key)/i.test(name)
}

function looksSensitive(value: string): boolean {
  return /\bsk-[A-Za-z0-9_-]{20,}\b|\bgh[opusr]_[A-Za-z0-9]{20,}\b|\bAKIA[0-9A-Z]{16}\b/.test(value)
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function stableId(...parts: string[]): string {
  return createHash('sha256').update(parts.join('\0')).digest('hex').slice(0, 32)
}
