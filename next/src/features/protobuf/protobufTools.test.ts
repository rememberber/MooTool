import { describe, expect, it } from 'vitest'
import { convertBinary, decodeWire, formatProtoDefinition, jsonToProtobuf, protobufToJson } from './protobufTools'

const proto = 'syntax = "proto3"; message Person { string name = 1; int32 age = 2; repeated string tags = 3; }'

describe('Protobuf tools', () => {
  it('converts JSON to protobuf Hex/Base64 and back', () => {
    const json = '{"name":"Moo","age":25,"tags":["desktop","tool"]}'
    const hex = jsonToProtobuf(proto, 'Person', json, 'Hex')
    expect(hex).toMatch(/^[0-9a-f]+$/)
    expect(JSON.parse(protobufToJson(proto, 'Person', hex, 'Hex'))).toEqual({ name: 'Moo', age: 25, tags: ['desktop', 'tool'] })
    const base64 = convertBinary(hex, 'Hex', 'Base64')
    expect(convertBinary(base64, 'Base64', 'Hex')).toBe(hex)
  })

  it('decodes wire fields without a proto definition', () => {
    const hex = jsonToProtobuf(proto, 'Person', '{"name":"Moo","age":25}', 'Hex')
    const output = decodeWire(hex, 'Hex')
    expect(output).toContain('field=1')
    expect(output).toContain('value="Moo"')
    expect(output).toContain('field=2')
    expect(output).toContain('value=25')
  })

  it('formats compact proto definitions', () => {
    expect(formatProtoDefinition(proto)).toContain('message Person {\n  string name = 1;')
  })
})
