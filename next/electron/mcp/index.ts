import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import { createMooToolServer } from './server'
import { callMooTool, listMooToolTools } from './tools'
import { callVaultTool, isVaultTool, listVaultTools } from './vaultTools'

// This entry never starts Electron UI or databases. Vault reads require an explicit grant.
async function main(): Promise<void> {
  const args = process.argv.slice(2)
  let accessFile: string | undefined
  if (args[0] === '--access-file' && args[1]) {
    accessFile = args[1]
    args.splice(0, 2)
  }
  if (args.length === 1 && args[0] === '--list') {
    process.stdout.write(`${JSON.stringify([...listMooToolTools(), ...listVaultTools()], null, 2)}\n`)
  } else if (args.length === 2 && args[0] === '--call') {
    const chunks: Buffer[] = []
    let size = 0
    for await (const chunk of process.stdin) {
      const bytes = Buffer.from(chunk)
      size += bytes.length
      if (size > 1_000_000) throw new Error('Input exceeds 1 MB')
      chunks.push(bytes)
    }
    // PowerShell 5.1 may prefix native stdin with a UTF-8 BOM. TextDecoder
    // accepts that marker and rejects malformed UTF-8 instead of replacing bytes.
    const input = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(Buffer.concat(chunks)))
    const result = isVaultTool(args[1]) ? await callVaultTool(args[1], input, accessFile) : callMooTool(args[1], input)
    process.stdout.write(`${JSON.stringify(result)}\n`)
    if (result.isError) process.exitCode = 1
  } else if (args.length === 0) {
    const server = createMooToolServer(accessFile)
    await server.connect(new StdioServerTransport())
  } else {
    throw new Error('Usage: mootool-mcp [--list | --call TOOL_NAME < arguments.json]')
  }
}

void main().catch((error) => {
  process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`)
  process.exitCode = 1
})
