import { describe, expect, it } from 'vitest'
import {
  compressJson,
  escapeJsonString,
  findDuplicateJsonKeys,
  formatJson,
  formatJsonAdvanced,
  javaBeanToJson,
  jsonToJavaBean,
  jsonToXml,
  listJsonPaths,
  queryJsonPath,
  swapJsonKeysAndValues,
  unescapeJsonString,
  validateJson,
  xmlToJson
} from './jsonTools'
import type { MessageKey } from '@/shared/i18n/messages'

const translations: Partial<Record<MessageKey, string>> = {
  'json.valid.idle': 'idle',
  'json.valid.ok': 'valid {type}',
  'json.valid.error': 'invalid',
  'json.error.empty': 'empty',
  'json.error.notString': 'not string',
  'json.error.duplicateKeys': 'duplicates {paths}',
  'json.error.objectRequired': 'object required',
  'json.error.emptyJavaBean': 'empty bean',
  'json.error.noJavaFields': 'no fields',
  'json.error.emptyXml': 'empty xml',
  'json.error.emptyPath': 'empty path'
}

const t = (key: MessageKey, params?: Record<string, string>) => {
  let message = translations[key] ?? key
  for (const [name, value] of Object.entries(params ?? {})) {
    message = message.replaceAll(`{${name}}`, value)
  }
  return message
}

describe('jsonTools', () => {
  it('formats and compresses JSON without changing its value', () => {
    const input = '{"name":"MooTool","items":[1,2]}'
    expect(formatJson(input, t, 2)).toBe('{\n  "name": "MooTool",\n  "items": [\n    1,\n    2\n  ]\n}')
    expect(compressJson(formatJson(input, t), t)).toBe(input)
  })

  it('escapes and restores JSON strings', () => {
    const input = 'line one\nline two'
    expect(unescapeJsonString(escapeJsonString(input), t)).toBe(input)
  })

  it('reports idle, valid, and invalid input', () => {
    expect(validateJson('', t).kind).toBe('idle')
    expect(validateJson('[]', t)).toEqual({ kind: 'valid', message: 'valid Array' })
    expect(validateJson('{', t).kind).toBe('error')
  })

  it('sorts keys recursively and detects duplicate keys', () => {
    const input = '{"z":{"B":1,"a":2},"A":0}'
    expect(formatJsonAdvanced(input, t, { spaces: 2, sortKeys: true, ignoreCase: true, checkDuplicateKeys: true }))
      .toBe('{\n  "A": 0,\n  "z": {\n    "a": 2,\n    "B": 1\n  }\n}')
    expect(findDuplicateJsonKeys('{"a":1,"A":2,"child":{"x":1,"x":2}}', true))
      .toEqual(['$.A', '$.child.x'])
  })

  it('converts JSON and XML in both directions', () => {
    const xml = jsonToXml('{"name":"MooTool","enabled":true}', t)
    expect(xml).toContain('<name>MooTool</name>')
    expect(JSON.parse(xmlToJson('<tool><name>MooTool</name><enabled>true</enabled></tool>', t)))
      .toEqual({ tool: { name: 'MooTool', enabled: true } })
  })

  it('queries and enumerates JSON paths', () => {
    const input = '{"store":{"books":[{"title":"One"},{"title":"Two"}]}}'
    expect(queryJsonPath(input, '$.store.books[1].title', t)).toBe('"Two"')
    expect(listJsonPaths(input, t).map((entry) => entry.path)).toContain('$.store.books[0].title')
  })

  it('swaps object keys and values', () => {
    expect(JSON.parse(swapJsonKeysAndValues('{"first":"one","second":2}', t)))
      .toEqual({ one: 'first', 2: 'second' })
  })

  it('converts JavaBean fields and emits valid nested Java classes', () => {
    expect(JSON.parse(javaBeanToJson('public class User { private String name; private int age; private List<String> tags; }', t)))
      .toEqual({ name: '', age: 0, tags: [] })
    const source = jsonToJavaBean('{"name":"MooTool","profile":{"active":true}}', t, 'ToolConfig')
    expect(source).toContain('public class ToolConfig')
    expect(source).toContain('private Profile profile;')
    expect(source).toContain('public static class Profile')
    expect(source.trimEnd().endsWith('}')).toBe(true)
  })
})
