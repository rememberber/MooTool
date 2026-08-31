import { describe, expect, it } from 'vitest'
import {
  analyzeJson,
  contentFingerprint,
  escapeJsonString,
  findDuplicateJsonKeys,
  formatJson,
  javaBeanToJson,
  jsonToJavaBean,
  jsonToXml,
  listJsonPaths,
  minifyJson,
  queryJsonPath,
  swapJsonKeysAndValues,
  unescapeJsonString,
  validateJson
} from './jsonTools'

describe('Tauri JSON tools', () => {
  it('formats, sorts and minifies JSON without changing its value', () => {
    const input = '{"z":1,"nested":{"b":2,"a":1}}'
    const formatted = formatJson(input, { indent: 2, sortKeys: true })

    expect(formatted).toBe('{\n  "nested": {\n    "a": 1,\n    "b": 2\n  },\n  "z": 1\n}')
    expect(minifyJson(formatted)).toBe('{"nested":{"a":1,"b":2},"z":1}')
  })

  it('validates input and reports structural metrics', () => {
    expect(validateJson('').kind).toBe('idle')
    expect(validateJson('{"items":[1,2]}')).toMatchObject({ kind: 'valid', rootType: 'Object' })
    expect(validateJson('{').kind).toBe('error')
    expect(analyzeJson('{"items":[1,2]}')).toEqual({
      rootType: 'Object',
      nodes: 4,
      keys: 1,
      maxDepth: 2,
      bytes: 15
    })
  })

  it('queries JSONPath and escapes string content', () => {
    const input = '{"store":{"items":[{"name":"MooTool"},{"name":"Tauri"}]}}'
    expect(queryJsonPath(input, '$.store.items[1].name')).toBe('"Tauri"')
    expect(unescapeJsonString(escapeJsonString('你好\nTauri'))).toBe('你好\nTauri')
  })

  it('creates a stable compact fingerprint for session tracking', () => {
    expect(contentFingerprint('same')).toBe(contentFingerprint('same'))
    expect(contentFingerprint('same')).not.toBe(contentFingerprint('different'))
  })

  it('swaps scalar object keys and values without silently losing duplicates', () => {
    expect(swapJsonKeysAndValues('{"one":1,"two":2}')).toBe('{\n  "1": "one",\n  "2": "two"\n}')
    expect(() => swapJsonKeysAndValues('{"one":1,"uno":1}')).toThrow('JSON_TOOL_swapValue')
    expect(() => swapJsonKeysAndValues('{"nested":{}}')).toThrow('JSON_TOOL_swapValue')
  })

  it('converts JSON values to escaped XML', () => {
    expect(jsonToXml('{"message":"A & B","items":[1,2]}')).toContain('<message>A &amp; B</message>')
    expect(jsonToXml('{"items":[1,2]}')).toContain('<items>1</items>\n  <items>2</items>')
  })

  it('detects duplicate keys without losing their JSON paths', () => {
    expect(findDuplicateJsonKeys('{"a":1,"A":2,"child":{"x":1,"x":2}}', true)).toEqual(['$.A', '$.child.x'])
    expect(() => formatJson('{"x":1,"x":2}', { indent: 2, sortKeys: false, checkDuplicateKeys: true })).toThrow('JSON_TOOL_duplicateKeys')
  })

  it('converts JSON and JavaBean field skeletons in both directions', () => {
    const java = jsonToJavaBean('{"user":{"name":"Moo"},"items":[{"id":1}],"active":true}', 'Payload')
    expect(java).toContain('public class Payload')
    expect(java).toContain('private User user;')
    expect(java).toContain('private java.util.List<Item> items;')
    expect(jsonToJavaBean('{"class":"demo"}', 'Payload')).toContain('private String class_;')
    expect(javaBeanToJson('class User { private String name; private long id; private boolean active; }')).toBe('{\n  "name": "",\n  "id": 0,\n  "active": false\n}')
  })

  it('lists selectable JSONPath entries for nested values', () => {
    expect(listJsonPaths('{"users":[{"name":"Moo"}]}').map((entry) => entry.path)).toEqual(['$', '$.users', '$.users[0]', '$.users[0].name'])
  })
})
