export type QuickNoteSort = 'name' | 'modified' | 'created'

export type QuickNoteMetadata = {
  title: string
  style: string
  syntax: string
  fontName: string
  fontSize: number
  color: string
  lineWrap: boolean
  createdAt: string
  modifiedAt: string
}

export type QuickNoteNode = {
  name: string
  relativePath: string
  kind: 'directory' | 'file'
  children?: QuickNoteNode[]
  title?: string
  color?: string
  createdAt?: string
  modifiedAt?: string
}

export type QuickNoteFile = {
  relativePath: string
  content: string
  metadata: QuickNoteMetadata
  modifiedAt: string
}

export type QuickNoteListInput = {
  keyword?: string
  includeContent?: boolean
  sort?: QuickNoteSort
  hideIgnored?: boolean
}

export type CreateQuickNoteInput = {
  title: string
  parentPath?: string
  fontSize?: number
  lineWrap?: boolean
}

export type SaveQuickNoteInput = {
  relativePath: string
  content: string
  metadata: QuickNoteMetadata
}

export type RenameQuickNoteEntryInput = {
  relativePath: string
  name: string
}

export type MoveQuickNoteEntryInput = {
  relativePath: string
  targetDirectory: string
}

export type QuickNoteAttachment = {
  relativePath: string
  markdown: string
  dataUrl: string
}
