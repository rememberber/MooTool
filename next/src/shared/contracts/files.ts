export type TextFileKind = 'json' | 'xml' | 'text' | 'source'

export type TextFileResult = {
  path: string
  name: string
  content: string
}

export type SaveTextFileInput = {
  kind: TextFileKind
  defaultName: string
  content: string
}
