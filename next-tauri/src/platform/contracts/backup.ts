export interface BackupExportResult {
  backupPath: string
  imageCount: number
  databaseBytes: number
  vaultFileCount: number
}

export interface BackupImportResult {
  sourcePath: string
  rollbackPath: string
  imageCount: number
  vaultFileCount: number
}

export interface BackupApi {
  chooseExportDirectory(): Promise<string | null>
  chooseImportDirectory(): Promise<string | null>
  exportTo(destinationDirectory: string): Promise<BackupExportResult>
  importFrom(sourceDirectory: string): Promise<BackupImportResult>
}
