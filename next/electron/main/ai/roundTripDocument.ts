import { createHash } from 'node:crypto'
import { parseDocument } from 'yaml'

export const roundTripFormats = ['json', 'toml', 'yaml', 'markdown'] as const
export type RoundTripFormat = (typeof roundTripFormats)[number]

export type RoundTripDocument = {
  format: RoundTripFormat
  sourceHash: string
  sizeBytes: number
  serialize(): string
}

const maximumDocumentBytes = 5 * 1024 * 1024

export function openRoundTripDocument(format: RoundTripFormat, source: string): RoundTripDocument {
  if (!roundTripFormats.includes(format)) throw new Error('Unsupported AI configuration format')
  if (typeof source !== 'string' || Buffer.byteLength(source, 'utf8') > maximumDocumentBytes) throw new Error('Invalid AI configuration document')
  validateDocument(format, source)
  return Object.freeze({
    format,
    sourceHash: createHash('sha256').update(source).digest('hex'),
    sizeBytes: Buffer.byteLength(source, 'utf8'),
    serialize: () => source
  })
}

function validateDocument(format: RoundTripFormat, source: string): void {
  if (format === 'json') {
    JSON.parse(source)
    return
  }
  if (format === 'yaml') {
    assertValidYaml(source)
    return
  }
  if (format === 'markdown') {
    const frontmatter = readFrontmatter(source)
    if (frontmatter !== undefined) assertValidYaml(frontmatter)
  }
  // TOML stays opaque until a versioned client adapter supplies a lossless parser.
}

function readFrontmatter(source: string): string | undefined {
  if (!source.startsWith('---\n') && !source.startsWith('---\r\n')) return undefined
  const lines = source.split(/\r?\n/)
  const closingIndex = lines.slice(1).findIndex((line) => line.trim() === '---')
  if (closingIndex < 0) throw new Error('Markdown frontmatter is not closed')
  return lines.slice(1, closingIndex + 1).join('\n')
}

function assertValidYaml(source: string): void {
  const document = parseDocument(source, { keepSourceTokens: true, strict: true })
  if (document.errors.length > 0) throw new Error(`Invalid YAML document: ${document.errors[0].message}`)
}
