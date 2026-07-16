import type { SettingsPatch } from './settings'

export const legacyMigrationCategories = [
  'history',
  'httpRequests',
  'httpHistory',
  'hosts',
  'translationWords',
  'translationHistory',
  'favoriteColors',
  'favoriteRegex',
  'favoriteCron',
  'toolDrafts',
  'qrCodes',
  'quickNotes',
  'jsonItems',
  'quickNoteVaultFiles',
  'jsonVaultFiles'
] as const

export type LegacyMigrationCategory = (typeof legacyMigrationCategories)[number]
export type LegacyMigrationCounts = Record<LegacyMigrationCategory, number>
export type LegacyMigrationWarning = 'differentVaultRemotes' | 'secretsSkipped'

export type LegacyMigrationInput = {
  sourceDirectory: string
}

export type LegacyMigrationPreview = {
  sourceDirectory: string
  databasePath: string
  configPath: string
  quickNoteVaultPath: string
  jsonVaultPath: string
  databaseFound: boolean
  configFound: boolean
  alreadyMigrated: boolean
  totalItems: number
  items: LegacyMigrationCounts
  warnings: LegacyMigrationWarning[]
}

export type LegacyMigrationResult = {
  sourceDirectory: string
  backupDirectory: string
  alreadyMigrated: boolean
  imported: LegacyMigrationCounts
  skipped: LegacyMigrationCounts
  settingsPatch: SettingsPatch
  warnings: LegacyMigrationWarning[]
}
