export type QuickNoteSyntax =
  | 'text/plain'
  | 'text/markdown'
  | 'application/json'
  | 'text/java'
  | 'text/javascript'
  | 'text/typescript'
  | 'text/python'
  | 'text/html'
  | 'text/xml'
  | 'text/yaml'
  | 'text/sql'

const QUICK_NOTE_SYNTAX: Record<string, QuickNoteSyntax> = {
  plain: 'text/plain',
  markdown: 'text/markdown',
  json: 'application/json',
  java: 'text/java',
  javascript: 'text/javascript',
  typescript: 'text/typescript',
  python: 'text/python',
  html: 'text/html',
  xml: 'text/xml',
  yaml: 'text/yaml',
  sql: 'text/sql',
  'text/plain': 'text/plain',
  'text/markdown': 'text/markdown',
  'application/json': 'application/json',
  'text/java': 'text/java',
  'text/javascript': 'text/javascript',
  'text/typescript': 'text/typescript',
  'text/python': 'text/python',
  'text/html': 'text/html',
  'text/xml': 'text/xml',
  'text/yaml': 'text/yaml',
  'text/sql': 'text/sql'
}

export function normalizeNoteSyntax(value: unknown): QuickNoteSyntax {
  return typeof value === 'string' ? QUICK_NOTE_SYNTAX[value] ?? 'text/plain' : 'text/plain'
}

export interface QuickNote {
  id: string
  title: string
  content: string
  tags: string[]
  color: 'default' | 'coral' | 'yellow' | 'green' | 'blue' | 'purple' | 'red'
  folderPath: string
  editorFont: 'default' | 'mono' | 'serif'
  lineHeight: 'compact' | 'normal' | 'relaxed'
  fontSize: number
  fontName: string
  lineSpacing: number
  relativePath: string
  lineWrapping: boolean
  syntax: QuickNoteSyntax
  pinned: boolean
  createdAt: number
  updatedAt: number
}

export interface CreateQuickNoteInput {
  title: string
  parentPath: string
  fontName: string
  fontSize: number
  lineWrap: boolean
}

export interface QuickNoteFolder {
  path: string
  createdAt: number
  updatedAt: number
}

export interface ToolFavorite {
  id: string
  toolId: string
  name: string
  payloadJson: string
  createdAt: number
  updatedAt: number
}

export interface QuickNoteAttachment {
  id: string
  noteId: string
  name: string
  mimeType: string
  sizeBytes: number
  createdAt: number
  relativePath?: string
  markdown?: string
  dataUrl?: string
}

export type BoardMessageColor = 'blue' | 'green' | 'yellow' | 'pink' | 'purple' | 'gray'

export interface BoardMessage {
  id: string
  content: string
  color: BoardMessageColor
  pinned: boolean
  createdAt: number
  updatedAt: number
}

export interface LocalDataApi {
  listNotes(): Promise<QuickNote[]>
  saveNote(note: QuickNote): Promise<QuickNote>
  createNote(input: CreateQuickNoteInput): Promise<QuickNote>
  duplicateNote(relativePath: string): Promise<QuickNote>
  moveNoteEntry(relativePath: string, targetDirectory: string): Promise<QuickNote>
  renameNoteEntry(relativePath: string, name: string): Promise<QuickNote>
  deleteNote(id: string): Promise<boolean>
  revealNoteEntry(relativePath: string): Promise<void>
  exportNotes(directory: string, relativePaths: string[]): Promise<number>
  openNoteVault(): Promise<void>
  listNoteFolders(): Promise<QuickNoteFolder[]>
  saveNoteFolder(folder: QuickNoteFolder): Promise<QuickNoteFolder>
  renameNoteFolder(path: string, nextPath: string, updatedAt: number): Promise<QuickNoteFolder[]>
  deleteNoteFolder(path: string): Promise<number>
  listToolFavorites(toolId: string): Promise<ToolFavorite[]>
  saveToolFavorite(favorite: ToolFavorite): Promise<ToolFavorite>
  deleteToolFavorite(id: string): Promise<boolean>
  listMessages(): Promise<BoardMessage[]>
  saveMessage(message: BoardMessage): Promise<BoardMessage>
  deleteMessage(id: string): Promise<boolean>
  listHostProfiles(keyword?: string, includeContent?: boolean): Promise<HostProfile[]>
  saveHostProfile(profile: SaveHostProfileInput): Promise<HostProfile>
  deleteHostProfile(id: number): Promise<boolean>
  listTranslationWords(keyword?: string): Promise<TranslationWord[]>
  saveTranslationWord(word: SaveTranslationWordInput): Promise<TranslationWord>
  deleteTranslationWord(id: number): Promise<boolean>
  listTranslationHistory(keyword?: string): Promise<TranslationHistory[]>
  deleteTranslationHistory(id: number): Promise<boolean>
  clearTranslationHistory(): Promise<number>
}

export interface SaveHostProfileInput {
  id?: number
  name: string
  content: string
}

export interface HostProfile {
  id: number
  name: string
  content: string
  createTime: string
  modifiedTime: string
}

export interface SaveTranslationWordInput {
  id?: number
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  remark: string
}

export interface TranslationWord extends SaveTranslationWordInput {
  id: number
  createTime: string
  modifiedTime: string
}

export interface TranslationHistory {
  id: number
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  translatorType: 'google' | 'bing' | 'deepl'
  createTime: string
}
