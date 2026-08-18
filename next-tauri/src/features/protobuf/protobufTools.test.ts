import { describe, expect, it } from 'vitest'
import {
  convertProtobufBinary,
  decodeProtobuf,
  encodeProtobuf,
  inspectProtobufSchema,
  inspectWire
} from './protobufTools'

const schema = `syntax = "proto3";
package mootool.demo;

message Person {
  uint64 id = 1;
  string name = 2;
  repeated string tags = 3;
  Role role = 4;
}

enum Role {
  ROLE_UNSPECIFIED = 0;
  ADMIN = 1;
}`

describe('Protobuf tools', () => {
  it('discovers namespaced message types', () => {
    expect(inspectProtobufSchema(schema)).toEqual({
      messageNames: ['mootool.demo.Person'],
      packageName: 'mootool.demo'
    })
  })

  it('encodes JSON and decodes it with lossless 64-bit strings', () => {
    const hex = encodeProtobuf(
      schema,
      'mootool.demo.Person',
      '{"id":"9007199254740993","name":"MooTool","tags":["tauri"],"role":"ADMIN"}',
      'hex'
    )
    expect(hex).toMatch(/^[0-9a-f]+$/)
    expect(JSON.parse(decodeProtobuf(schema, 'mootool.demo.Person', hex, 'hex'))).toMatchObject({
      id: '9007199254740993',
      name: 'MooTool',
      tags: ['tauri'],
      role: 'ADMIN'
    })
    expect(convertProtobufBinary(hex, 'hex', 'base64')).toMatch(/^[A-Za-z0-9+/]+=*$/)
  })

  it('inspects wire fields without a schema', () => {
    const hex = encodeProtobuf(schema, 'mootool.demo.Person', '{"id":"7","name":"Moo"}', 'hex')
    expect(inspectWire(hex, 'hex')).toContain('field 1 · Varint')
    expect(inspectWire(hex, 'hex')).toContain('field 2 · Length-delimited')
  })

  it('rejects malformed schema, JSON and binary input', () => {
    expect(() => inspectProtobufSchema('message {')).toThrow('PROTOBUF_TOOL_schemaParse')
    expect(() => encodeProtobuf(schema, 'mootool.demo.Person', '[]', 'hex')).toThrow('PROTOBUF_TOOL_jsonObject')
    expect(() => decodeProtobuf(schema, 'mootool.demo.Person', 'abc', 'hex')).toThrow('PROTOBUF_TOOL_hex')
  })
})
