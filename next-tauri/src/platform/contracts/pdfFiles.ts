export interface PdfExportSession {
  targetPaths: string[]
  write(fileIndex: number, bytes: Uint8Array, onProgress?: (writtenBytes: number) => void): Promise<void>
  finish(): Promise<string[]>
  cancel(): Promise<void>
}

export interface PdfFilesApi {
  begin(names: string[]): Promise<PdfExportSession | null>
}
