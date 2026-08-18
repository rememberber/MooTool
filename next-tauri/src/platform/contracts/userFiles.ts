export interface UserTextFile {
  name: string
  path: string
  content: string
}

export interface UserFilesApi {
  pickText(): Promise<UserTextFile | null>
  exportText(defaultName: string, content: string): Promise<string | null>
}
