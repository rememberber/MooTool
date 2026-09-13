import { Server } from '@modelcontextprotocol/sdk/server/index.js'
import { CallToolRequestSchema, ListToolsRequestSchema } from '@modelcontextprotocol/sdk/types.js'
import { version } from '../../package.json'
import { callMooTool, listMooToolTools } from './tools'
import { callVaultTool, isVaultTool, listVaultTools } from './vaultTools'

export function createMooToolServer(accessFile?: string): Server {
  const server = new Server({ name: 'MooTool', version }, { capabilities: { tools: {} } })
  server.setRequestHandler(ListToolsRequestSchema, async () => ({ tools: [...listMooToolTools(), ...listVaultTools()] }))
  server.setRequestHandler(CallToolRequestSchema, async ({ params }) => isVaultTool(params.name)
    ? callVaultTool(params.name, params.arguments ?? {}, accessFile) : callMooTool(params.name, params.arguments ?? {}))
  return server
}
