export interface QuickNote {
  id: string
  title: string
  content: string
  tags: string[]
  color: 'default' | 'coral' | 'yellow' | 'green' | 'blue' | 'purple' | 'red'
  folderPath: string
  editorFont: 'default' | 'mono' | 'serif'
  lineHeight: 'compact' | 'normal' | 'relaxed'
  lineWrapping: boolean
  syntax: 'markdown' | 'plain' | 'json' | 'yaml'
  pinned: boolean
  createdAt: number
  updatedAt: number
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
  deleteNote(id: string): Promise<boolean>
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
  listHostProfiles(): Promise<HostProfile[]>
  saveHostProfile(profile: HostProfile): Promise<HostProfile>
  deleteHostProfile(id: string): Promise<boolean>
  listTranslationWords(): Promise<TranslationWord[]>
  saveTranslationWord(word: TranslationWord): Promise<TranslationWord>
  deleteTranslationWord(id: string): Promise<boolean>
  listTranslationHistory(): Promise<TranslationHistory[]>
  deleteTranslationHistory(id: string): Promise<boolean>
  clearTranslationHistory(): Promise<number>
}

export interface HostProfile {
  id: string
  name: string
  content: string
  createdAt: number
  updatedAt: number
}

export interface TranslationWord {
  id: string
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  remark: string
  createdAt: number
  updatedAt: number
}

export interface TranslationHistory {
  id: string
  sourceText: string
  targetText: string
  sourceLang: string
  targetLang: string
  provider: 'google' | 'bing'
  createdAt: number
}
