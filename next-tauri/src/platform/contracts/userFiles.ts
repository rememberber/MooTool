export interface UserTextFile {
  name: string
  path: string
  content: string
}

export interface UserFilesApi {
  pickText(kind?: 'text' | 'source'): Promise<UserTextFile | null>
  exportText(defaultName: string, content: string): Promise<string | null>
  exportDataUrl(defaultName: string, dataUrl: string): Promise<string | null>
}
