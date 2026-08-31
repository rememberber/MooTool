export interface QuickNote {
  id: string
  title: string
  content: string
  tags: string[]
  color: 'default' | 'coral' | 'yellow' | 'green' | 'blue' | 'purple' | 'red'
  pinned: boolean
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
