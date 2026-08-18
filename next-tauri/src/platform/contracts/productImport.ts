export type ProductImportSource = 'java' | 'nextElectron'

export interface ProductImportCounts {
  quickNotes: number
  hostProfiles: number
  translationWords: number
  translationHistory: number
  operationHistory: number
  vaultFiles: number
  images: number
  settings: number
}

export interface ProductImportPreview {
  sourceProduct: ProductImportSource
  sourceDirectory: string
  fingerprint: string
  databaseFound: boolean
  settingsFound: boolean
  alreadyImported: boolean
  items: ProductImportCounts
  totalItems: number
  warnings: string[]
}

export interface ProductImportResult {
  preview: ProductImportPreview
  imported: ProductImportCounts
  skipped: ProductImportCounts
  backupPath: string
  reportPath: string
  importedVaultPath: string | null
}

export interface ProductImportApi {
  chooseSourceDirectory(): Promise<string | null>
  preview(sourceProduct: ProductImportSource, sourceDirectory: string): Promise<ProductImportPreview>
  run(
    sourceProduct: ProductImportSource,
    sourceDirectory: string,
    expectedFingerprint: string
  ): Promise<ProductImportResult>
}
