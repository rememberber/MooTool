/// <reference types="vite/client" />

import type { AppNavigationEvent, AppPaths, ExternalPageId, RuntimeStatus, WorkspaceState } from './src/shared/contracts/app'
import type { AppSettings, SecretKey, SecretStatus, SettingsPatch } from './src/shared/contracts/settings'
import type { FuncHistoryRecord, HistoryQuery, SaveFuncHistoryInput } from './src/shared/contracts/history'
import type { SaveTextFileInput, TextFileKind, TextFileResult } from './src/shared/contracts/files'
import type { ImageAsset, ImageAssetSummary, RenameImageAssetInput, SaveImageAssetInput, ScreenCapture } from './src/shared/contracts/images'
import type { DigestAlgorithmId, DigestFileResult, ImageFilePayload, SaveBinaryFileInput } from './src/shared/contracts/nativeFiles'
import type { PdfFileInfo, PdfMergeSource, PdfOperationResult, PdfSplitTask } from './src/shared/contracts/pdf'
import type { JsonVaultFile, JsonVaultListInput, JsonVaultNode, MoveJsonVaultEntryInput, RenameJsonVaultEntryInput, SaveJsonVaultFileInput } from './src/shared/contracts/jsonVault'
import type { CreateQuickNoteInput, MoveQuickNoteEntryInput, QuickNoteAttachment, QuickNoteFile, QuickNoteListInput, QuickNoteNode, RenameQuickNoteEntryInput, SaveQuickNoteInput } from './src/shared/contracts/quickNote'
import type { VaultGitActionInput, VaultGitActionResult, VaultGitCommit, VaultGitDiffInput, VaultGitStatus } from './src/shared/contracts/vaultGit'
import type { FavoriteKind, FavoriteRecord, SaveFavoriteInput } from './src/shared/contracts/favorites'
import type { HttpRequestDraft, HttpRequestHistory, HttpResponseResult, HttpSendInput, SaveTranslationHistoryInput, SaveTranslationWordInput, SavedHttpRequest, TranslationHistory, TranslationInput, TranslationResult, TranslationWord } from './src/shared/contracts/network'
import type { EnvironmentSnapshot, HostProfile, LocalAddressSnapshot, NetworkCommandInput, NetworkCommandResult, SaveHostProfileInput, SystemHostsFile, SystemInfoSnapshot } from './src/shared/contracts/system'
import type { RuntimeExecutionInput, RuntimeExecutionResult, RuntimeOutputEvent } from './src/shared/contracts/runtime'
import type { BackupExportResult, BackupInfo, BackupKind, BackupLocation } from './src/shared/contracts/backup'
import type { LegacyMigrationInput, LegacyMigrationPreview, LegacyMigrationResult } from './src/shared/contracts/migration'
import type { UpdateCheckEvent, UpdateCheckResult, UpdateDownloadState } from './src/shared/contracts/update'

declare global {
  interface Window {
    mootool: {
      platform: string
      getAppVersion: () => Promise<string>
      getAppPaths: () => Promise<AppPaths>
      getSystemTheme: () => Promise<'light' | 'dark'>
      getSettings: () => Promise<AppSettings>
      updateSettings: (patch: SettingsPatch) => Promise<AppSettings>
      openSettings: () => Promise<void>
      closeSettings: () => Promise<void>
      getSecretStatus: (key: SecretKey) => Promise<SecretStatus>
      setSecret: (key: SecretKey, value: string) => Promise<SecretStatus>
      clearSecret: (key: SecretKey) => Promise<SecretStatus>
      getWorkspaceState: () => Promise<WorkspaceState>
      setWorkspaceState: (state: WorkspaceState) => Promise<WorkspaceState>
      listHistory: (query: HistoryQuery) => Promise<FuncHistoryRecord[]>
      saveHistory: (input: SaveFuncHistoryInput) => Promise<void>
      deleteHistory: (id: number) => Promise<void>
      clearHistory: (funcType: string) => Promise<void>
      listFavorites: (kind: FavoriteKind) => Promise<FavoriteRecord[]>
      saveFavorite: (input: SaveFavoriteInput) => Promise<FavoriteRecord>
      deleteFavorite: (id: number) => Promise<void>
      listHttpRequests: (keyword?: string) => Promise<SavedHttpRequest[]>
      saveHttpRequest: (request: HttpRequestDraft, response?: HttpResponseResult) => Promise<SavedHttpRequest>
      deleteHttpRequest: (id: number) => Promise<void>
      listHttpHistory: (keyword?: string) => Promise<HttpRequestHistory[]>
      deleteHttpHistory: (id: number) => Promise<void>
      clearHttpHistory: () => Promise<void>
      sendHttpRequest: (input: HttpSendInput) => Promise<HttpResponseResult>
      cancelNetworkRequest: (requestId: string) => Promise<boolean>
      translate: (input: TranslationInput) => Promise<TranslationResult>
      listTranslationWords: (keyword?: string) => Promise<TranslationWord[]>
      saveTranslationWord: (input: SaveTranslationWordInput) => Promise<TranslationWord>
      deleteTranslationWord: (id: number) => Promise<void>
      listTranslationHistory: (keyword?: string) => Promise<TranslationHistory[]>
      saveTranslationHistory: (input: SaveTranslationHistoryInput) => Promise<TranslationHistory>
      deleteTranslationHistory: (id: number) => Promise<void>
      clearTranslationHistory: () => Promise<void>
      listHostProfiles: (keyword?: string) => Promise<HostProfile[]>
      saveHostProfile: (input: SaveHostProfileInput) => Promise<HostProfile>
      deleteHostProfile: (id: number) => Promise<void>
      readSystemHosts: () => Promise<SystemHostsFile>
      writeSystemHosts: (content: string) => Promise<SystemHostsFile>
      runNetworkCommand: (input: NetworkCommandInput) => Promise<NetworkCommandResult>
      cancelSystemCommand: (requestId: string) => Promise<boolean>
      getEnvironmentSnapshot: () => Promise<EnvironmentSnapshot>
      getLocalAddresses: () => Promise<LocalAddressSnapshot>
      getSystemInfo: () => Promise<SystemInfoSnapshot>
      chooseDirectory: (initialPath?: string) => Promise<string | null>
      openTextFile: (kind: TextFileKind) => Promise<TextFileResult | null>
      saveTextFile: (input: SaveTextFileInput) => Promise<string | null>
      digestFile: (algorithm: DigestAlgorithmId) => Promise<DigestFileResult | null>
      chooseImageFile: () => Promise<ImageFilePayload | null>
      saveBinaryFile: (input: SaveBinaryFileInput) => Promise<string | null>
      readClipboardImage: () => Promise<string | null>
      writeClipboardImage: (dataUrl: string) => Promise<void>
      captureScreens: () => Promise<ScreenCapture[]>
      listImageAssets: () => Promise<ImageAssetSummary[]>
      readImageAsset: (name: string) => Promise<ImageAsset>
      importImageAssets: () => Promise<ImageAssetSummary[]>
      saveImageAsset: (input: SaveImageAssetInput) => Promise<ImageAssetSummary>
      renameImageAsset: (input: RenameImageAssetInput) => Promise<ImageAssetSummary>
      deleteImageAssets: (names: string[]) => Promise<void>
      exportImageAssets: (names: string[]) => Promise<string | null>
      openImageAsset: (name: string) => Promise<void>
      choosePdfFiles: () => Promise<PdfFileInfo[]>
      mergePdfFiles: (sources: PdfMergeSource[]) => Promise<PdfOperationResult | null>
      splitPdfFiles: (tasks: PdfSplitTask[]) => Promise<PdfOperationResult>
      listJsonVault: (input?: JsonVaultListInput) => Promise<JsonVaultNode[]>
      readJsonVaultFile: (relativePath: string) => Promise<JsonVaultFile>
      saveJsonVaultFile: (input: SaveJsonVaultFileInput) => Promise<JsonVaultFile>
      createJsonVaultFolder: (relativePath: string) => Promise<string>
      renameJsonVaultEntry: (input: RenameJsonVaultEntryInput) => Promise<string>
      moveJsonVaultEntry: (input: MoveJsonVaultEntryInput) => Promise<string>
      duplicateJsonVaultFile: (relativePath: string) => Promise<JsonVaultFile>
      deleteJsonVaultFile: (relativePath: string) => Promise<void>
      openJsonVault: () => Promise<void>
      getVaultGitStatus: () => Promise<VaultGitStatus>
      listVaultGitHistory: () => Promise<VaultGitCommit[]>
      getVaultGitDiff: (input: VaultGitDiffInput) => Promise<string>
      runVaultGitAction: (input: VaultGitActionInput) => Promise<VaultGitActionResult>
      listQuickNotes: (input?: QuickNoteListInput) => Promise<QuickNoteNode[]>
      readQuickNote: (relativePath: string) => Promise<QuickNoteFile>
      createQuickNote: (input: CreateQuickNoteInput) => Promise<QuickNoteFile>
      saveQuickNote: (input: SaveQuickNoteInput) => Promise<QuickNoteFile>
      createQuickNoteFolder: (relativePath: string) => Promise<string>
      renameQuickNoteEntry: (input: RenameQuickNoteEntryInput) => Promise<string>
      moveQuickNoteEntry: (input: MoveQuickNoteEntryInput) => Promise<string>
      duplicateQuickNote: (relativePath: string) => Promise<QuickNoteFile>
      deleteQuickNoteEntry: (relativePath: string) => Promise<void>
      importQuickNoteAttachment: () => Promise<QuickNoteAttachment | null>
      readQuickNoteAttachment: (relativePath: string) => Promise<string>
      openQuickNoteVault: () => Promise<void>
      getQuickNoteGitStatus: () => Promise<VaultGitStatus>
      listQuickNoteGitHistory: () => Promise<VaultGitCommit[]>
      getQuickNoteGitDiff: (input: VaultGitDiffInput) => Promise<string>
      runQuickNoteGitAction: (input: VaultGitActionInput) => Promise<VaultGitActionResult>
      getBackupInfo: () => Promise<BackupInfo>
      exportBackup: (kind: BackupKind) => Promise<BackupExportResult | null>
      openBackupLocation: (location: BackupLocation) => Promise<void>
      getDefaultLegacySource: () => Promise<string>
      previewLegacyMigration: (input: LegacyMigrationInput) => Promise<LegacyMigrationPreview>
      runLegacyMigration: (input: LegacyMigrationInput) => Promise<LegacyMigrationResult>
      checkForUpdates: () => Promise<UpdateCheckResult>
      getUpdateState: () => Promise<UpdateDownloadState>
      downloadUpdate: () => Promise<UpdateDownloadState>
      installUpdate: () => Promise<void>
      openReleasePage: () => Promise<void>
      openProjectPage: () => Promise<void>
      openExternalPage: (pageId: ExternalPageId) => Promise<void>
      detectRuntimes: () => Promise<RuntimeStatus[]>
      runCode: (input: RuntimeExecutionInput) => Promise<RuntimeExecutionResult>
      cancelCodeRun: (requestId: string) => Promise<boolean>
      onSystemThemeChange: (callback: (theme: 'light' | 'dark') => void) => () => void
      onSettingsChange: (callback: (settings: AppSettings) => void) => () => void
      onNavigate: (callback: (event: AppNavigationEvent) => void) => () => void
      onJsonVaultChange: (callback: (relativePath: string) => void) => () => void
      onQuickNoteVaultChange: (callback: (relativePath: string) => void) => () => void
      onRuntimeOutput: (callback: (event: RuntimeOutputEvent) => void) => () => void
      onUpdateCheck: (callback: (event: UpdateCheckEvent) => void) => () => void
      onUpdateStateChange: (callback: (state: UpdateDownloadState) => void) => () => void
    }
  }
}

export {}
