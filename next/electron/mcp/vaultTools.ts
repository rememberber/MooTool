import { z } from 'zod'
import type { CallToolResult, Tool } from '@modelcontextprotocol/sdk/types.js'
import { VaultReadService, type VaultKind } from './vaultAccess'

const searchSchema = z.strictObject({ query: z.string().max(200).default(''), limit: z.number().int().min(1).max(50).default(20), offset: z.number().int().min(0).max(2000).default(0) })
const readSchema = z.strictObject({ path: z.string().min(1).max(1000), offset: z.number().int().min(0).max(2_000_000).default(0), length: z.number().int().min(1).max(50_000).default(20_000) })
const definitions = (['notes', 'json'] as const).flatMap((kind) => (['search', 'read'] as const).map((operation) => ({
  name: `mootool_${kind === 'notes' ? 'notes' : 'json_documents'}_${operation}`, kind, operation
})))

export function listVaultTools(): Tool[] {
  return definitions.map(({ name, kind, operation }) => ({
    name,
    description: `${operation === 'search' ? 'Search titles and content in' : 'Read a relative document path from'} MooTool ${kind === 'notes' ? 'Quick Notes' : 'JSON documents'}. Requires the user to enable read access in MooTool AI integration settings. Read-only; hidden, gitignored files and symlinks are excluded. Results can be paged.`,
    inputSchema: z.toJSONSchema(operation === 'search' ? searchSchema : readSchema, { target: 'draft-7' }) as Tool['inputSchema'],
    annotations: { readOnlyHint: true, destructiveHint: false, openWorldHint: false, idempotentHint: true }
  }))
}

export function isVaultTool(name: string): boolean { return definitions.some((item) => item.name === name) }

export async function callVaultTool(name: string, input: unknown, accessFile?: string): Promise<CallToolResult> {
  try {
    const definition = definitions.find((item) => item.name === name)
    if (!definition) throw new Error('Unknown vault tool')
    const vault = new VaultReadService(accessFile)
    let result: unknown
    const kind: VaultKind = definition.kind
    if (definition.operation === 'search') {
      const { query, limit, offset } = searchSchema.parse(input)
      result = await vault.search(kind, query, limit, offset)
    } else {
      const { path, offset, length } = readSchema.parse(input)
      result = await vault.read(kind, path, offset, length)
    }
    return { content: [{ type: 'text', text: JSON.stringify(result) }] }
  } catch (error) {
    return { isError: true, content: [{ type: 'text', text: error instanceof Error ? error.message : String(error) }] }
  }
}
