export const backupKinds = ['all', 'database', 'settings', 'images'] as const

export type BackupKind = (typeof backupKinds)[number]
export type BackupLocation = 'data' | 'images' | 'quickNote' | 'jsonVault'

export type BackupInfo = {
  dataDirectory: string
  databasePath: string
  settingsPath: string
  imagesPath: string
  quickNotePath: string
  jsonVaultPath: string
}

export type BackupExportResult = {
  directory: string
  exported: string[]
}
