import { createHash, randomUUID } from 'node:crypto'
import { lstat, realpath, rm } from 'node:fs/promises'
import { isAbsolute, join } from 'node:path'
import { isDeepStrictEqual } from 'node:util'
import { stringify as stringifyToml } from 'smol-toml'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import { z } from 'zod'
import { aiClients, type AiClient, type AiComponentState, type AiDataAccess, type AiDataAccessRequest, type AiInstallPreview, type AiInstallResult, type AiConnectionResult, type AiIntegrationStatus } from '../../src/shared/contracts/aiIntegration'
import { atomicWrite, manageServer, readOptional, readServer, type Launch } from './aiIntegrationConfig'
import { readAccess } from '../mcp/vaultAccess'
import { version } from '../../package.json'
import skillText from '../mcp/skill/SKILL.md?raw'
export { mergeJson, mergeToml } from './aiIntegrationConfig'

const requestSchema = z.strictObject({ client: z.enum(aiClients), mode: z.enum(['mcp', 'skill', 'both']), operation: z.enum(['install', 'uninstall']).default('install') })
const receiptSchema = z.strictObject({
  schemaVersion: z.literal(1), appVersion: z.string(),
  mcp: z.strictObject({ command: z.string(), args: z.array(z.string()), env: z.record(z.string(), z.string()), type: z.string().optional() }).optional(),
  skills: z.record(z.enum(['SKILL.md', 'runtime.md']), z.string().regex(/^[a-f0-9]{64}$/)).optional()
})
type Receipt = z.infer<typeof receiptSchema>
type Change = { path: string; before: string | null; after: string | null; internal?: boolean }
type Plan = { files: Change[]; expires: number; uninstall: boolean }
type Options = { home: string; codexHome?: string; claudeConfigDir?: string; stateDirectory?: string; getRoots?: () => { notes: string; json: string }; command: string; entry: string; platform?: NodeJS.Platform; unavailableReason?: string }

export class AiIntegrationService {
  private readonly plans = new Map<string, Plan>()
  private installing = false
  readonly launch: Launch
  readonly accessFile: string
  private readonly stateDirectory: string

  constructor(private readonly options: Options) {
    this.stateDirectory = options.stateDirectory ?? join(options.home, '.mootool-ai')
    this.accessFile = join(this.stateDirectory, 'access.json')
    this.launch = { command: options.command, args: [options.entry, '--access-file', this.accessFile], env: { ELECTRON_RUN_AS_NODE: '1' } }
  }

  async getStatus(input: unknown): Promise<AiIntegrationStatus> {
    const client = z.enum(aiClients).parse(input)
    try {
      const { receipt } = await this.readReceipt(client)
      const source = await readOptional(this.configPath(client))
      const existing = readServer(source, client === 'codex')
      const desired = this.clientLaunch(client)
      let mcp: AiComponentState = existing === undefined ? (receipt.mcp ? 'needs-repair' : 'not-installed')
        : isDeepStrictEqual(existing, desired) ? 'installed' : isDeepStrictEqual(existing, receipt.mcp) ? 'needs-repair' : 'conflict'
      // Detect damaged syntax / formatting that cannot be changed without touching user edits.
      if (source !== null && mcp !== 'conflict') {
        try { manageServer(source, client === 'codex', desired, receipt.mcp) }
        catch { mcp = 'conflict' }
      }
      const states: AiComponentState[] = []
      if (client !== 'cursor') for (const [name, expected] of Object.entries(this.skillFiles())) {
        const actual = await readOptional(join(this.skillRoot(client), name))
        states.push(actual === null ? (receipt.skills?.[name as 'SKILL.md' | 'runtime.md'] ? 'needs-repair' : 'not-installed')
          : actual === expected ? 'installed' : hash(actual) === receipt.skills?.[name as 'SKILL.md' | 'runtime.md'] ? 'needs-repair' : 'conflict')
      }
      const skill: AiComponentState = states.includes('conflict') ? 'conflict' : states.includes('needs-repair') || (states.includes('installed') && states.includes('not-installed')) ? 'needs-repair' : states.includes('installed') ? 'installed' : 'not-installed'
      return { mcp, skill, version }
    } catch { return { mcp: 'conflict', skill: 'conflict', version } }
  }

  async preview(input: unknown): Promise<AiInstallPreview> {
    const { client, mode, operation } = requestSchema.parse(input)
    if (client === 'cursor' && mode !== 'mcp') throw new Error('Cursor currently supports MCP installation only.')
    const uninstall = operation === 'uninstall'
    if (!uninstall) await this.assertRuntime()
    const files: Change[] = []
    const { receipt, path: receiptPath, source: receiptSource } = await this.readReceipt(client)
    const nextReceipt: Receipt = { ...receipt, appVersion: version }
    const desired = this.clientLaunch(client)
    const configuration = client === 'codex' ? stringifyToml({ mcp_servers: { mootool: this.launch } }) : JSON.stringify({ mcpServers: { mootool: desired } }, null, 2)
    if (mode !== 'skill') {
      const path = this.configPath(client)
      const before = await readOptional(path)
      const after = before === null && uninstall ? null : manageServer(before ?? (client === 'codex' ? '' : '{}'), client === 'codex', uninstall ? null : desired, receipt.mcp)
      files.push({ path, before, after })
      if (uninstall) delete nextReceipt.mcp
      else nextReceipt.mcp = desired
    }
    if (mode !== 'mcp') {
      for (const [name, expected] of Object.entries(this.skillFiles())) {
        const path = join(this.skillRoot(client), name)
        const before = await readOptional(path)
        const owned = receipt.skills?.[name as 'SKILL.md' | 'runtime.md']
        if (before !== null && (uninstall ? hash(before) !== owned : before !== expected && hash(before) !== owned)) throw new Error(`The MooTool skill was modified outside the installer: ${path}. Preserve it before continuing.`)
        files.push({ path, before, after: uninstall ? null : expected })
      }
      if (uninstall) delete nextReceipt.skills
      else nextReceipt.skills = Object.fromEntries(Object.entries(this.skillFiles()).map(([name, content]) => [name, hash(content)])) as Receipt['skills']
    }
    files.push({ path: receiptPath, before: receiptSource, after: nextReceipt.mcp || nextReceipt.skills ? JSON.stringify(nextReceipt, null, 2) : null, internal: true })
    const id = randomUUID()
    for (const [key, plan] of this.plans) if (plan.expires < Date.now()) this.plans.delete(key)
    if (this.plans.size >= 20) this.plans.delete(this.plans.keys().next().value!)
    this.plans.set(id, { files, expires: Date.now() + 10 * 60_000, uninstall })
    return { id, configuration, files: files.filter((file) => !file.internal).map(({ path, before, after }) => ({ path,
      action: before === after ? 'unchanged' : after === null ? 'delete' : before === null ? 'create' : 'update',
      content: after === null ? '' : path.endsWith('.md') ? after : configuration
    })) }
  }

  async install(id: unknown): Promise<AiInstallResult> {
    if (typeof id !== 'string') throw new Error('Invalid installation preview')
    const plan = this.plans.get(id)
    if (!plan || plan.expires < Date.now()) throw new Error('Installation preview expired. Refresh the preview.')
    if (this.installing) throw new Error('An installation is already running.')
    this.installing = true
    const written: Change[] = []
    const backups: string[] = []
    try {
      for (const file of plan.files) await assertUnchanged(file)
      if (!plan.uninstall) await this.testConnection()
      for (const file of plan.files) {
        if (file.before === file.after) continue
        await assertUnchanged(file)
        if (file.before !== null) {
          const backup = `${file.path}.mootool-backup-${randomUUID()}`
          await atomicWrite(backup, file.before)
          if (!file.internal) backups.push(backup)
        }
        await put(file.path, file.after)
        written.push(file)
      }
      for (const file of plan.files) if (await readOptional(file.path) !== file.after) throw new Error(`Installation verification failed: ${file.path}`)
      this.plans.delete(id)
      return { paths: plan.files.filter((file) => !file.internal).map(({ path }) => path), backups }
    } catch (error) {
      const rollbackErrors: string[] = []
      for (const file of written.reverse()) {
        try {
          if (await readOptional(file.path) !== file.after) throw new Error('File changed after installation')
          await put(file.path, file.before)
        } catch { rollbackErrors.push(file.path) }
      }
      if (rollbackErrors.length) throw new Error(`Installation failed; restore these files from backups: ${rollbackErrors.join(', ')}. Backups: ${backups.join(', ')}`)
      throw error
    } finally { this.installing = false }
  }

  async getDataAccess(): Promise<AiDataAccess> {
    const { notes, json } = await readAccess(this.accessFile)
    return { notes, json }
  }

  async setDataAccess(input: unknown): Promise<AiDataAccess> {
    const request: AiDataAccessRequest = z.strictObject({ notes: z.boolean(), json: z.boolean() }).parse(input)
    const roots = this.options.getRoots?.()
    const access: AiDataAccess = { notes: null, json: null }
    for (const kind of ['notes', 'json'] as const) if (request[kind]) {
      if (!roots?.[kind] || !isAbsolute(roots[kind])) throw new Error('Configure the MooTool vault directory first')
      const path = await realpath(roots[kind])
      if (!(await lstat(path)).isDirectory()) throw new Error('The MooTool vault directory is unavailable')
      access[kind] = path
    }
    await atomicWrite(this.accessFile, JSON.stringify({ version: 1, ...access }, null, 2))
    return access
  }

  async testConnection(): Promise<AiConnectionResult> {
    await this.assertRuntime()
    const client = new Client({ name: 'mootool-connection-test', version })
    const transport = new StdioClientTransport({ ...this.launch, stderr: 'ignore' })
    try {
      await client.connect(transport, { timeout: 10_000 })
      const { tools } = await client.listTools({}, { timeout: 10_000 })
      const sample = await client.callTool({ name: 'mootool_json_format', arguments: { text: '{"moo":true}', spaces: 0 } }, undefined, { timeout: 10_000 })
      if (sample.isError || !Array.isArray(sample.content) || sample.content[0]?.type !== 'text' || sample.content[0].text !== '{"moo":true}') throw new Error('MooTool tool verification failed')
      return { serverName: client.getServerVersion()?.name ?? 'MooTool', tools: tools.map(({ name }) => name) }
    } finally { await transport.close() }
  }

  private clientLaunch(client: AiClient) { return client === 'codex' ? this.launch : { type: 'stdio', ...this.launch } }
  private configPath(client: AiClient): string {
    if (client === 'codex') return join(this.absoluteHome(this.options.codexHome, join(this.options.home, '.codex')), 'config.toml')
    if (client === 'cursor') return join(this.options.home, '.cursor', 'mcp.json')
    return join(this.absoluteHome(this.options.claudeConfigDir, this.options.home), '.claude.json')
  }
  private skillRoot(client: AiClient): string {
    return client === 'codex' ? join(this.options.home, '.agents', 'skills', 'mootool') : join(this.absoluteHome(this.options.claudeConfigDir, join(this.options.home, '.claude')), 'skills', 'mootool')
  }
  private skillFiles(): Record<'SKILL.md' | 'runtime.md', string> { return { 'SKILL.md': skillText, 'runtime.md': this.runtimeInstructions() } }
  private async readReceipt(client: AiClient) {
    const path = join(this.stateDirectory, `${client}.json`)
    const source = await readOptional(path)
    try { return { path, source, receipt: source === null ? { schemaVersion: 1, appVersion: version } as Receipt : receiptSchema.parse(JSON.parse(source)) } }
    catch { throw new Error('The MooTool installation record is damaged. Restore it from its backup before continuing.') }
  }
  private absoluteHome(value: string | undefined, fallback: string): string {
    if (!value) return fallback
    if (!isAbsolute(value)) throw new Error('Client configuration directory must be an absolute path')
    return value
  }
  private async assertRuntime(): Promise<void> {
    if (this.options.unavailableReason) throw new Error(this.options.unavailableReason)
    if (!isAbsolute(this.launch.command) || !isAbsolute(this.options.entry)) throw new Error('MooTool runtime paths must be absolute')
    await lstat(this.launch.command)
    await lstat(this.options.entry)
  }
  private runtimeInstructions(): string {
    const { command, args } = this.launch
    const powershell = (value: string) => `'${value.replaceAll("'", "''")}'`
    const posix = (value: string) => `'${value.replaceAll("'", "'\\''")}'`
    const windows = (this.options.platform ?? process.platform) === 'win32'
    const launch = windows
      ? `& ${[command, ...args].map(powershell).join(' ')}`
      : `ELECTRON_RUN_AS_NODE=1 ${[command, ...args].map(posix).join(' ')}`
    // A GUI executable at the end of a PowerShell pipeline may return early.
    const completion = windows ? ' | Out-String\nif ($LASTEXITCODE -ne 0) { throw "MooTool failed (exit $LASTEXITCODE)" }' : ''
    return `# Installed MooTool runtime\n\nRun on this machine; keep MooTool installed at its current location.\n\n\`\`\`${windows ? 'powershell' : 'sh'}\n${windows ? "$env:ELECTRON_RUN_AS_NODE = '1'\n$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)\n" : ''}${launch} --list${completion}\n\`\`\`\n\nCreate a UTF-8 JSON file containing the tool arguments, then run:\n\n\`\`\`${windows ? 'powershell' : 'sh'}\n${windows ? "$env:ELECTRON_RUN_AS_NODE = '1'\n$OutputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)\n" : ''}${windows ? `Get-Content -Raw -Encoding utf8 'arguments.json' | ${launch}` : launch} --call mootool_json_format${windows ? '' : ' < arguments.json'}${completion}\n\`\`\`\n\nExample arguments: \`{"text":"{\\"b\\":2,\\"a\\":1}","sortKeys":true}\`. Replace the tool name and arguments according to \`--list\`. The output is an MCP result with \`content\` and optional \`isError\`; a failed call exits nonzero.\n`
  }
}
function hash(value: string): string { return createHash('sha256').update(value).digest('hex') }
async function put(path: string, content: string | null): Promise<void> {
  if (content === null) await rm(path, { force: true })
  else await atomicWrite(path, content)
}
async function assertUnchanged(file: Change): Promise<void> {
  if (await readOptional(file.path) !== file.before) throw new Error(`Configuration changed since preview. Refresh the preview: ${file.path}`)
}
