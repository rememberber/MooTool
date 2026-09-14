import { createHash, randomUUID } from 'node:crypto'
import { z } from 'zod'
import { JSONPath } from 'jsonpath-plus'
import type { CallToolResult, Tool } from '@modelcontextprotocol/sdk/types.js'
import { formatJsonAdvanced } from '../../src/features/json/jsonTools'
import { compareText } from '../../src/features/diff/diffTools'
import { fromUnicode, hexToText, textToHex, toUnicode, urlDecode, urlEncode } from '../../src/features/encode/encodeTools'
import { localToTimestamp, timestampToLocal } from '../../src/features/time/timeTools'

const text = z.string().max(100_000)
const definitions: Array<{ tool: Tool; run: (input: unknown) => unknown }> = []

function define<S extends z.ZodRawShape>(name: string, description: string, shape: S, run: (input: z.output<z.ZodObject<S>>) => unknown): void {
  const schema = z.strictObject(shape)
  definitions.push({
    tool: {
      name, description,
      inputSchema: z.toJSONSchema(schema, { target: 'draft-7' }) as Tool['inputSchema'],
      annotations: { readOnlyHint: true, destructiveHint: false, openWorldHint: false }
    },
    run: (input) => run(schema.parse(input))
  })
}

define('mootool_json_format', 'Format or minify JSON with MooTool; optionally sort keys and reject duplicate keys.', {
  text, spaces: z.number().int().min(0).max(8).default(2), sortKeys: z.boolean().default(false),
  checkDuplicateKeys: z.boolean().default(true)
}, ({ text: input, ...options }) => formatJsonAdvanced(input, (key, params) => `${key}${params ? `: ${JSON.stringify(params)}` : ''}`, { ...options, ignoreCase: false }))

define('mootool_json_query', 'Query JSON with JSONPath. Script/filter evaluation is disabled. Returns an array of matches.', {
  text, path: z.string().min(1).max(1000)
}, ({ text: input, path }) => JSONPath({ path, json: JSON.parse(input), eval: false, wrap: true }))

define('mootool_encode', 'Encode or decode UTF-8 text as Base64, URL, hexadecimal or Unicode escapes. URL also supports GB2312.', {
  text, format: z.enum(['base64', 'url', 'hex', 'unicode']), direction: z.enum(['encode', 'decode']),
  charset: z.enum(['utf-8', 'gb2312']).default('utf-8')
}, ({ text: input, format, direction, charset }) => {
  const encode = direction === 'encode'
  switch (format) {
    case 'url': return encode ? urlEncode(input, charset) : urlDecode(input, charset)
    case 'hex': return encode ? textToHex(input) : hexToText(input)
    case 'unicode': return encode ? toUnicode(input) : fromUnicode(input)
    case 'base64': {
      if (encode) return Buffer.from(input, 'utf8').toString('base64')
      if (!/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(input)) throw new Error('Invalid Base64')
      return new TextDecoder('utf-8', { fatal: true }).decode(Buffer.from(input, 'base64'))
    }
  }
})

define('mootool_timestamp', 'Convert a Unix timestamp to local time or yyyy-MM-dd HH:mm:ss to a timestamp using MooTool timezone rules. 13+ digit timestamps are detected as milliseconds.', {
  text: z.string().max(100), direction: z.enum(['to-local', 'to-timestamp']),
  unit: z.enum(['second', 'millisecond']).default('second'), zone: z.string().max(100).default('UTC')
}, ({ text: input, direction, unit, zone }) => direction === 'to-local'
  ? timestampToLocal(input, unit, zone) : localToTimestamp(input, unit, zone))

define('mootool_diff', 'Compare two texts with MooTool and return a unified diff and line counts. Inputs are limited to 8,000 characters each.', {
  left: z.string().max(8000), right: z.string().max(8000), ignoreWhitespace: z.boolean().default(false)
}, ({ left, right, ignoreWhitespace }) => {
  const { unified, added, removed, changed } = compareText(left, right, ignoreWhitespace)
  return { unified, added, removed, changed }
})

define('mootool_hash', 'Calculate a hexadecimal digest of UTF-8 text. MD5/SHA-1 are available for legacy checksums only.', {
  text, algorithm: z.enum(['md5', 'sha1', 'sha256', 'sha384', 'sha512']).default('sha256')
}, ({ text: input, algorithm }) => createHash(algorithm).update(input, 'utf8').digest('hex'))

define('mootool_uuid', 'Generate random version 4 UUIDs locally.', {
  count: z.number().int().min(1).max(100).default(1)
}, ({ count }) => Array.from({ length: count }, () => randomUUID()))

export function listMooToolTools(): Tool[] {
  return definitions.map(({ tool }) => tool)
}

export function callMooTool(name: string, input: unknown): CallToolResult {
  try {
    const definition = definitions.find(({ tool }) => tool.name === name)
    if (!definition) throw new Error(`Unknown MooTool tool: ${name}`)
    const result = definition.run(input)
    const output = typeof result === 'string' ? result : JSON.stringify(result, null, 2)
    if (Buffer.byteLength(output ?? '', 'utf8') > 1_000_000) throw new Error('Result exceeds 1 MB; reduce the input or query scope.')
    return { content: [{ type: 'text', text: output ?? 'null' }] }
  } catch (error) {
    return { isError: true, content: [{ type: 'text', text: error instanceof Error ? error.message : String(error) }] }
  }
}
