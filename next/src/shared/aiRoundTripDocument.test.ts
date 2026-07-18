import { describe, expect, it } from 'vitest'
import { openRoundTripDocument, type RoundTripFormat } from '../../electron/main/ai/roundTripDocument'

describe('AI configuration round trips', () => {
  const goldenDocuments: Array<{ format: RoundTripFormat; source: string }> = [
    {
      format: 'json',
      source: '{\n  "known": true,\n  "unknownFutureField": { "kept": [1, 2, 3] }\n}\n'
    },
    {
      format: 'toml',
      source: '# keep this comment\nmodel = "gpt-5"\n\n[mcp_servers.github]\ncommand = "npx" # inline comment\nfuture_option = true\n'
    },
    {
      format: 'yaml',
      source: '# preserve comments\nname: fixture\nunknown:\n  nested: &value yes\nalias: *value\n'
    },
    {
      format: 'markdown',
      source: '---\nname: review\nmetadata:\n  future: true\n---\n\n# Review\n\nKeep **Markdown** byte-for-byte.\n'
    }
  ]

  it.each(goldenDocuments)('preserves an unchanged $format document byte-for-byte', ({ format, source }) => {
    const document = openRoundTripDocument(format, source)

    expect(document.serialize()).toBe(source)
    expect(Buffer.from(document.serialize())).toEqual(Buffer.from(source))
    expect(document.sourceHash).toMatch(/^[0-9a-f]{64}$/)
  })

  it('rejects damaged structured documents without rewriting them', () => {
    expect(() => openRoundTripDocument('json', '{"broken": }')).toThrow()
    expect(() => openRoundTripDocument('yaml', 'key: [unterminated')).toThrow('Invalid YAML')
    expect(() => openRoundTripDocument('markdown', '---\nname: broken\n')).toThrow('not closed')
  })
})
