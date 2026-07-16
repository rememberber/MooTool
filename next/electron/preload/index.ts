import { contextBridge, ipcRenderer, type IpcRendererEvent } from 'electron'
import type { AppNavigationEvent, AppPaths, RuntimeStatus, WorkspaceState } from '../../src/shared/contracts/app'
import type { AppSettings, SecretKey, SecretStatus, SettingsPatch } from '../../src/shared/contracts/settings'
import type { FuncHistoryRecord, HistoryQuery, SaveFuncHistoryInput } from '../../src/shared/contracts/history'
import type { SaveTextFileInput, TextFileKind, TextFileResult } from '../../src/shared/contracts/files'
import type { ImageAsset, ImageAssetSummary, RenameImageAssetInput, SaveImageAssetInput, ScreenCapture } from '../../src/shared/contracts/images'
import type { DigestAlgorithmId, DigestFileResult, ImageFilePayload, SaveBinaryFileInput } from '../../src/shared/contracts/nativeFiles'
import type { PdfFileInfo, PdfMergeSource, PdfOperationResult, PdfSplitTask } from '../../src/shared/contracts/pdf'
import type { JsonVaultFile, JsonVaultListInput, JsonVaultNode, MoveJsonVaultEntryInput, RenameJsonVaultEntryInput, SaveJsonVaultFileInput } from '../../src/shared/contracts/jsonVault'
import type { CreateQuickNoteInput, MoveQuickNoteEntryInput, QuickNoteAttachment, QuickNoteFile, QuickNoteListInput, QuickNoteNode, RenameQuickNoteEntryInput, SaveQuickNoteInput } from '../../src/shared/contracts/quickNote'
import type { VaultGitActionInput, VaultGitActionResult, VaultGitCommit, VaultGitDiffInput, VaultGitStatus } from '../../src/shared/contracts/vaultGit'
import type { FavoriteKind, FavoriteRecord, SaveFavoriteInput } from '../../src/shared/contracts/favorites'
import type { HttpRequestDraft, HttpRequestHistory, HttpResponseResult, HttpSendInput, SaveTranslationHistoryInput, SaveTranslationWordInput, SavedHttpRequest, TranslationHistory, TranslationInput, TranslationResult, TranslationWord } from '../../src/shared/contracts/network'
import type { EnvironmentSnapshot, HostProfile, LocalAddressSnapshot, NetworkCommandInput, NetworkCommandResult, SaveHostProfileInput, SystemHostsFile, SystemInfoSnapshot } from '../../src/shared/contracts/system'
import type { RuntimeExecutionInput, RuntimeExecutionResult, RuntimeOutputEvent } from '../../src/shared/contracts/runtime'
import type { BackupExportResult, BackupInfo, BackupKind, BackupLocation } from '../../src/shared/contracts/backup'
import type { LegacyMigrationInput, LegacyMigrationPreview, LegacyMigrationResult } from '../../src/shared/contracts/migration'
import type { UpdateCheckEvent, UpdateCheckResult } from '../../src/shared/contracts/update'

contextBridge.exposeInMainWorld('mootool', {
  platform: process.platform,
  getAppVersion: (): Promise<string> => ipcRenderer.invoke('app:get-version'),
  getAppPaths: (): Promise<AppPaths> => ipcRenderer.invoke('app:get-paths'),
  getSystemTheme: (): Promise<'light' | 'dark'> => ipcRenderer.invoke('theme:get-system'),
  getSettings: (): Promise<AppSettings> => ipcRenderer.invoke('settings:get'),
  updateSettings: (patch: SettingsPatch): Promise<AppSettings> => ipcRenderer.invoke('settings:update', patch),
  openSettings: (): Promise<void> => ipcRenderer.invoke('settings:open'),
  closeSettings: (): Promise<void> => ipcRenderer.invoke('settings:close'),
  getSecretStatus: (key: SecretKey): Promise<SecretStatus> => ipcRenderer.invoke('secret:status', key),
  setSecret: (key: SecretKey, value: string): Promise<SecretStatus> => ipcRenderer.invoke('secret:set', key, value),
  clearSecret: (key: SecretKey): Promise<SecretStatus> => ipcRenderer.invoke('secret:clear', key),
  getWorkspaceState: (): Promise<WorkspaceState> => ipcRenderer.invoke('workspace:get'),
  setWorkspaceState: (state: WorkspaceState): Promise<WorkspaceState> => ipcRenderer.invoke('workspace:set', state),
  listHistory: (query: HistoryQuery): Promise<FuncHistoryRecord[]> => ipcRenderer.invoke('history:list', query),
  saveHistory: (input: SaveFuncHistoryInput): Promise<void> => ipcRenderer.invoke('history:save', input),
  deleteHistory: (id: number): Promise<void> => ipcRenderer.invoke('history:delete', id),
  clearHistory: (funcType: string): Promise<void> => ipcRenderer.invoke('history:clear', funcType),
  listFavorites: (kind: FavoriteKind): Promise<FavoriteRecord[]> => ipcRenderer.invoke('favorite:list', kind),
  saveFavorite: (input: SaveFavoriteInput): Promise<FavoriteRecord> => ipcRenderer.invoke('favorite:save', input),
  deleteFavorite: (id: number): Promise<void> => ipcRenderer.invoke('favorite:delete', id),
  listHttpRequests: (keyword?: string): Promise<SavedHttpRequest[]> => ipcRenderer.invoke('http:list', keyword),
  saveHttpRequest: (request: HttpRequestDraft, response?: HttpResponseResult): Promise<SavedHttpRequest> => ipcRenderer.invoke('http:save', request, response),
  deleteHttpRequest: (id: number): Promise<void> => ipcRenderer.invoke('http:delete', id),
  listHttpHistory: (keyword?: string): Promise<HttpRequestHistory[]> => ipcRenderer.invoke('http:history-list', keyword),
  deleteHttpHistory: (id: number): Promise<void> => ipcRenderer.invoke('http:history-delete', id),
  clearHttpHistory: (): Promise<void> => ipcRenderer.invoke('http:history-clear'),
  sendHttpRequest: (input: HttpSendInput): Promise<HttpResponseResult> => ipcRenderer.invoke('http:send', input),
  cancelNetworkRequest: (requestId: string): Promise<boolean> => ipcRenderer.invoke('network:cancel', requestId),
  translate: (input: TranslationInput): Promise<TranslationResult> => ipcRenderer.invoke('translation:send', input),
  listTranslationWords: (keyword?: string): Promise<TranslationWord[]> => ipcRenderer.invoke('translation:words-list', keyword),
  saveTranslationWord: (input: SaveTranslationWordInput): Promise<TranslationWord> => ipcRenderer.invoke('translation:words-save', input),
  deleteTranslationWord: (id: number): Promise<void> => ipcRenderer.invoke('translation:words-delete', id),
  listTranslationHistory: (keyword?: string): Promise<TranslationHistory[]> => ipcRenderer.invoke('translation:history-list', keyword),
  saveTranslationHistory: (input: SaveTranslationHistoryInput): Promise<TranslationHistory> => ipcRenderer.invoke('translation:history-save', input),
  deleteTranslationHistory: (id: number): Promise<void> => ipcRenderer.invoke('translation:history-delete', id),
  clearTranslationHistory: (): Promise<void> => ipcRenderer.invoke('translation:history-clear'),
  listHostProfiles: (keyword?: string): Promise<HostProfile[]> => ipcRenderer.invoke('host:list', keyword),
  saveHostProfile: (input: SaveHostProfileInput): Promise<HostProfile> => ipcRenderer.invoke('host:save', input),
  deleteHostProfile: (id: number): Promise<void> => ipcRenderer.invoke('host:delete', id),
  readSystemHosts: (): Promise<SystemHostsFile> => ipcRenderer.invoke('host:read-system'),
  writeSystemHosts: (content: string): Promise<SystemHostsFile> => ipcRenderer.invoke('host:write-system', content),
  runNetworkCommand: (input: NetworkCommandInput): Promise<NetworkCommandResult> => ipcRenderer.invoke('system:network-command', input),
  cancelSystemCommand: (requestId: string): Promise<boolean> => ipcRenderer.invoke('system:cancel', requestId),
  getEnvironmentSnapshot: (): Promise<EnvironmentSnapshot> => ipcRenderer.invoke('system:environment'),
  getLocalAddresses: (): Promise<LocalAddressSnapshot> => ipcRenderer.invoke('system:local-addresses'),
  getSystemInfo: (): Promise<SystemInfoSnapshot> => ipcRenderer.invoke('system:info'),
  chooseDirectory: (initialPath?: string): Promise<string | null> => ipcRenderer.invoke('dialog:choose-directory', initialPath),
  openTextFile: (kind: TextFileKind): Promise<TextFileResult | null> => ipcRenderer.invoke('files:open-text', kind),
  saveTextFile: (input: SaveTextFileInput): Promise<string | null> => ipcRenderer.invoke('files:save-text', input),
  digestFile: (algorithm: DigestAlgorithmId): Promise<DigestFileResult | null> => ipcRenderer.invoke('files:digest', algorithm),
  chooseImageFile: (): Promise<ImageFilePayload | null> => ipcRenderer.invoke('files:choose-image'),
  saveBinaryFile: (input: SaveBinaryFileInput): Promise<string | null> => ipcRenderer.invoke('files:save-binary', input),
  readClipboardImage: (): Promise<string | null> => ipcRenderer.invoke('clipboard:read-image'),
  writeClipboardImage: (dataUrl: string): Promise<void> => ipcRenderer.invoke('clipboard:write-image', dataUrl),
  captureScreens: (): Promise<ScreenCapture[]> => ipcRenderer.invoke('screen:capture'),
  listImageAssets: (): Promise<ImageAssetSummary[]> => ipcRenderer.invoke('images:list'),
  readImageAsset: (name: string): Promise<ImageAsset> => ipcRenderer.invoke('images:read', name),
  importImageAssets: (): Promise<ImageAssetSummary[]> => ipcRenderer.invoke('images:import'),
  saveImageAsset: (input: SaveImageAssetInput): Promise<ImageAssetSummary> => ipcRenderer.invoke('images:save', input),
  renameImageAsset: (input: RenameImageAssetInput): Promise<ImageAssetSummary> => ipcRenderer.invoke('images:rename', input),
  deleteImageAssets: (names: string[]): Promise<void> => ipcRenderer.invoke('images:delete', names),
  exportImageAssets: (names: string[]): Promise<string | null> => ipcRenderer.invoke('images:export', names),
  openImageAsset: (name: string): Promise<void> => ipcRenderer.invoke('images:open', name),
  choosePdfFiles: (): Promise<PdfFileInfo[]> => ipcRenderer.invoke('pdf:choose-files'),
  mergePdfFiles: (sources: PdfMergeSource[]): Promise<PdfOperationResult | null> => ipcRenderer.invoke('pdf:merge', sources),
  splitPdfFiles: (tasks: PdfSplitTask[]): Promise<PdfOperationResult> => ipcRenderer.invoke('pdf:split', tasks),
  listJsonVault: (input?: JsonVaultListInput): Promise<JsonVaultNode[]> => ipcRenderer.invoke('json-vault:list', input),
  readJsonVaultFile: (relativePath: string): Promise<JsonVaultFile> => ipcRenderer.invoke('json-vault:read', relativePath),
  saveJsonVaultFile: (input: SaveJsonVaultFileInput): Promise<JsonVaultFile> => ipcRenderer.invoke('json-vault:save', input),
  createJsonVaultFolder: (relativePath: string): Promise<string> => ipcRenderer.invoke('json-vault:create-folder', relativePath),
  renameJsonVaultEntry: (input: RenameJsonVaultEntryInput): Promise<string> => ipcRenderer.invoke('json-vault:rename', input),
  moveJsonVaultEntry: (input: MoveJsonVaultEntryInput): Promise<string> => ipcRenderer.invoke('json-vault:move', input),
  duplicateJsonVaultFile: (relativePath: string): Promise<JsonVaultFile> => ipcRenderer.invoke('json-vault:duplicate', relativePath),
  deleteJsonVaultFile: (relativePath: string): Promise<void> => ipcRenderer.invoke('json-vault:delete', relativePath),
  openJsonVault: (): Promise<void> => ipcRenderer.invoke('json-vault:open'),
  getVaultGitStatus: (): Promise<VaultGitStatus> => ipcRenderer.invoke('vault-git:status'),
  listVaultGitHistory: (): Promise<VaultGitCommit[]> => ipcRenderer.invoke('vault-git:history'),
  getVaultGitDiff: (input: VaultGitDiffInput): Promise<string> => ipcRenderer.invoke('vault-git:diff', input),
  runVaultGitAction: (input: VaultGitActionInput): Promise<VaultGitActionResult> => ipcRenderer.invoke('vault-git:action', input),
  listQuickNotes: (input?: QuickNoteListInput): Promise<QuickNoteNode[]> => ipcRenderer.invoke('quick-note:list', input),
  readQuickNote: (relativePath: string): Promise<QuickNoteFile> => ipcRenderer.invoke('quick-note:read', relativePath),
  createQuickNote: (input: CreateQuickNoteInput): Promise<QuickNoteFile> => ipcRenderer.invoke('quick-note:create', input),
  saveQuickNote: (input: SaveQuickNoteInput): Promise<QuickNoteFile> => ipcRenderer.invoke('quick-note:save', input),
  createQuickNoteFolder: (relativePath: string): Promise<string> => ipcRenderer.invoke('quick-note:create-folder', relativePath),
  renameQuickNoteEntry: (input: RenameQuickNoteEntryInput): Promise<string> => ipcRenderer.invoke('quick-note:rename', input),
  moveQuickNoteEntry: (input: MoveQuickNoteEntryInput): Promise<string> => ipcRenderer.invoke('quick-note:move', input),
  duplicateQuickNote: (relativePath: string): Promise<QuickNoteFile> => ipcRenderer.invoke('quick-note:duplicate', relativePath),
  deleteQuickNoteEntry: (relativePath: string): Promise<void> => ipcRenderer.invoke('quick-note:delete', relativePath),
  importQuickNoteAttachment: (): Promise<QuickNoteAttachment | null> => ipcRenderer.invoke('quick-note:import-attachment'),
  readQuickNoteAttachment: (relativePath: string): Promise<string> => ipcRenderer.invoke('quick-note:read-attachment', relativePath),
  openQuickNoteVault: (): Promise<void> => ipcRenderer.invoke('quick-note:open-vault'),
  getQuickNoteGitStatus: (): Promise<VaultGitStatus> => ipcRenderer.invoke('quick-note-git:status'),
  listQuickNoteGitHistory: (): Promise<VaultGitCommit[]> => ipcRenderer.invoke('quick-note-git:history'),
  getQuickNoteGitDiff: (input: VaultGitDiffInput): Promise<string> => ipcRenderer.invoke('quick-note-git:diff', input),
  runQuickNoteGitAction: (input: VaultGitActionInput): Promise<VaultGitActionResult> => ipcRenderer.invoke('quick-note-git:action', input),
  getBackupInfo: (): Promise<BackupInfo> => ipcRenderer.invoke('backup:info'),
  exportBackup: (kind: BackupKind): Promise<BackupExportResult | null> => ipcRenderer.invoke('backup:export', kind),
  openBackupLocation: (location: BackupLocation): Promise<void> => ipcRenderer.invoke('backup:open-location', location),
  getDefaultLegacySource: (): Promise<string> => ipcRenderer.invoke('legacy-migration:default-source'),
  previewLegacyMigration: (input: LegacyMigrationInput): Promise<LegacyMigrationPreview> => ipcRenderer.invoke('legacy-migration:preview', input),
  runLegacyMigration: (input: LegacyMigrationInput): Promise<LegacyMigrationResult> => ipcRenderer.invoke('legacy-migration:run', input),
  checkForUpdates: (): Promise<UpdateCheckResult> => ipcRenderer.invoke('update:check'),
  openReleasePage: (): Promise<void> => ipcRenderer.invoke('update:open-release'),
  openProjectPage: (): Promise<void> => ipcRenderer.invoke('app:open-project'),
  detectRuntimes: (): Promise<RuntimeStatus[]> => ipcRenderer.invoke('runtime:detect'),
  runCode: (input: RuntimeExecutionInput): Promise<RuntimeExecutionResult> => ipcRenderer.invoke('runtime:run', input),
  cancelCodeRun: (requestId: string): Promise<boolean> => ipcRenderer.invoke('runtime:cancel', requestId),
  onSystemThemeChange: (callback: (theme: 'light' | 'dark') => void) => subscribe('theme:system-changed', callback),
  onSettingsChange: (callback: (settings: AppSettings) => void) => subscribe('settings:changed', callback),
  onNavigate: (callback: (event: AppNavigationEvent) => void) => subscribe('app:navigate', callback),
  onJsonVaultChange: (callback: (relativePath: string) => void) => subscribe('json-vault:changed', callback),
  onQuickNoteVaultChange: (callback: (relativePath: string) => void) => subscribe('quick-note:vault-changed', callback),
  onRuntimeOutput: (callback: (event: RuntimeOutputEvent) => void) => subscribe('runtime:output', callback),
  onUpdateCheck: (callback: (event: UpdateCheckEvent) => void) => subscribe('update:checked', callback)
})

function subscribe<T>(channel: string, callback: (payload: T) => void): () => void {
  const listener = (_event: IpcRendererEvent, payload: T) => callback(payload)
  ipcRenderer.on(channel, listener)
  return () => ipcRenderer.removeListener(channel, listener)
}
