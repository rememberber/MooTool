import { afterEach, describe, expect, it } from 'vitest'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { InMemoryTransport } from '@modelcontextprotocol/sdk/inMemory.js'
import { createMooToolServer } from './server'
import { callMooTool } from './tools'

describe('MooTool MCP', () => {
  const closers: Array<() => Promise<void>> = []
  afterEach(async () => { await Promise.all(closers.splice(0).map((close) => close())) })

  it('negotiates MCP, discovers schemas and calls the same utilities used by the app', async () => {
    const server = createMooToolServer()
    const client = new Client({ name: 'test', version: '1.0.0' })
    const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair()
    closers.push(() => client.close(), () => server.close())
    await server.connect(serverTransport)
    await client.connect(clientTransport)
    expect(client.getServerVersion()?.name).toBe('MooTool')
    const { tools } = await client.listTools()
    expect(tools).toHaveLength(11)
    expect(tools.every((tool) => tool.inputSchema.type === 'object' && tool.annotations?.readOnlyHint)).toBe(true)
    const result = await client.callTool({ name: 'mootool_json_format', arguments: { text: '{"b":2,"a":1}', sortKeys: true, spaces: 0 } })
    expect(result.content).toEqual([{ type: 'text', text: '{"a":1,"b":2}' }])
    const failure = await client.callTool({ name: 'mootool_json_format', arguments: { text: '{bad}' } })
    expect(failure.isError).toBe(true)
    expect((await client.listTools()).tools).toHaveLength(11)
  })

  it('handles UTF-8, timezone conversion, diff, digest and UUID results', () => {
    const encode = callMooTool('mootool_encode', { text: 'Moo 中文🐮', format: 'base64', direction: 'encode' })
    const encoded = (encode.content[0] as { text: string }).text
    expect(callMooTool('mootool_encode', { text: encoded, format: 'base64', direction: 'decode' }).content).toEqual([{ type: 'text', text: 'Moo 中文🐮' }])
    expect(callMooTool('mootool_timestamp', { text: '1970-01-01 08:00:00', direction: 'to-timestamp', zone: 'Asia/Shanghai' }).content).toEqual([{ type: 'text', text: '0' }])
    expect(callMooTool('mootool_diff', { left: 'one\n', right: 'two\n' }).isError).toBeUndefined()
    expect(callMooTool('mootool_hash', { text: 'abc' }).content).toEqual([{ type: 'text', text: 'ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad' }])
    const uuids = JSON.parse((callMooTool('mootool_uuid', { count: 3 }).content[0] as { text: string }).text)
    expect(new Set(uuids).size).toBe(3)
    expect(uuids[0]).toMatch(/^[0-9a-f-]{14}4[0-9a-f-]{21}$/)
  })

  it('validates types, limits, duplicate JSON keys and refuses JSONPath script execution', () => {
    for (const [name, args] of [
      ['missing', {}], ['mootool_hash', { text: 123 }], ['mootool_hash', { text: 'a', path: '/etc/hosts' }],
      ['mootool_uuid', { count: 101 }], ['mootool_diff', { left: 'x'.repeat(8001), right: '' }],
      ['mootool_json_format', { text: '{"a":1,"a":2}' }],
      ['mootool_encode', { text: 'invalid!', format: 'base64', direction: 'decode' }],
      ['mootool_json_query', { text: '[1,2]', path: '$[?(@ > 1)]' }]
    ] as const) expect(callMooTool(name, args).isError).toBe(true)
    expect(callMooTool('mootool_json_query', { text: '{"values":[1,2]}', path: '$.values[*]' }).content).toEqual([{ type: 'text', text: '[\n  1,\n  2\n]' }])
  })
})
