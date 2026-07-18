/// <reference types="vite/client" />

import type { AppNavigationEvent, AppPaths, ExternalPageId, RuntimeStatus, ToolId, ToolWindowSnapshot, ToolWindowStatus, ToolWorkspaceBounds, WorkspaceState } from './src/shared/contracts/app'
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
import type { AiDiscoveryInput, AiDoctorSnapshot } from './src/shared/contracts/ai'
import type { AiChangeApplyResult, AiChangePlan, AiChangeRollbackResult } from './src/shared/contracts/aiChanges'
import type { AiSkillInstallApplyInput, AiSkillInstallApplyResult, AiSkillInstallInput, AiSkillInstallPreview, AiSkillInstallRollbackResult } from './src/shared/contracts/aiSkills'
import type { AiInstructionPreview, AiInstructionPreviewInput } from './src/shared/contracts/aiInstructions'
import type { AiMcpCopyInput, AiMcpCopyPreview, AiMcpInventory, AiMcpInventoryInput, AiMcpProbeInput, AiMcpProbeResult } from './src/shared/contracts/aiMcp'
import type { AiMemory, AiMemoryCandidate, AiMemoryCandidateReviewInput, AiMemoryCandidateSaveInput, AiMemoryListInput, AiMemoryPreview, AiMemoryPreviewInput, AiMemorySaveInput, AiMemorySnapshot } from './src/shared/contracts/aiMemory'
import type { AiModelRuntimeDetailInput, AiModelRuntimeModelDetail, AiModelRuntimeSnapshot } from './src/shared/contracts/aiModelRuntime'
import type { AiUsageBudget, AiUsageBudgetInput, AiUsageDashboard, AiUsageDashboardInput, AiUsageExportInput, AiUsageExportResult, AiUsageImportPreview, AiUsageImportPreviewInput, AiUsageImportResult, AiUsageProviderSyncInput, AiUsageProviderSyncResult } from './src/shared/contracts/aiUsage'
import type { AiAgentLaunchPlan, AiAgentManagerInput, AiAgentManagerSnapshot, AiAgentProfile, AiAgentProfileSaveInput } from './src/shared/contracts/aiAgents'
import type { AiContextInspectorInput, AiContextInspectorSnapshot } from './src/shared/contracts/aiContext'
import type { AiPromptLabRunInput, AiPromptLabRunResult, AiPromptLabSuite, AiPromptLabSuiteSaveInput } from './src/shared/contracts/aiPromptLab'
import type { AiAgentProfileShareDocument } from './src/shared/contracts/aiAgentShare'
import type { AiProjectStarterPreview, AiProjectStarterPreviewInput } from './src/shared/contracts/aiProjectStarter'
import type { AiAgentTaskOutputEvent, AiAgentTaskResult, AiAgentTaskStartInput } from './src/shared/contracts/aiAgentTasks'
import type { AiModelRuntimeActionExecuteInput, AiModelRuntimeActionPlan, AiModelRuntimeActionPlanInput, AiModelRuntimeActionProgressEvent, AiModelRuntimeActionResult } from './src/shared/contracts/aiModelRuntimeActions'
import type { AiNativeMemorySnapshot } from './src/shared/contracts/aiNativeMemory'
import type { AiMemoryEmbeddingProgressEvent, AiMemoryEmbeddingRebuildInput, AiMemoryEmbeddingRebuildResult, AiMemoryEmbeddingStatus, AiMemorySemanticPreview, AiMemorySemanticPreviewInput } from './src/shared/contracts/aiMemoryEmbedding'

declare global {
  interface Window {
    mootool: {
      platform: string
      toolWindowsEnabled: boolean
      getAppVersion: () => Promise<string>
      getAppPaths: () => Promise<AppPaths>
      getSystemTheme: () => Promise<'light' | 'dark'>
      getSettings: () => Promise<AppSettings>
      updateSettings: (patch: SettingsPatch) => Promise<AppSettings>
      openSettings: () => Promise<void>
      closeSettings: () => Promise<void>
      dismissWindow: () => Promise<void>
      getSecretStatus: (key: SecretKey) => Promise<SecretStatus>
      setSecret: (key: SecretKey, value: string) => Promise<SecretStatus>
      clearSecret: (key: SecretKey) => Promise<SecretStatus>
      getWorkspaceState: () => Promise<WorkspaceState>
      setWorkspaceState: (state: WorkspaceState) => Promise<WorkspaceState>
      getToolWindowSnapshot: () => Promise<ToolWindowSnapshot>
      activateToolView: (toolId: ToolId) => Promise<ToolWindowSnapshot>
      setToolWorkspaceBounds: (bounds: ToolWorkspaceBounds) => Promise<ToolWindowSnapshot>
      getToolWindowState: (toolId: Exclude<ToolId, 'mootool'>) => Promise<ToolWindowStatus>
      detachToolWindow: (toolId: Exclude<ToolId, 'mootool'>) => Promise<ToolWindowStatus>
      dockToolWindow: (toolId: Exclude<ToolId, 'mootool'>) => Promise<ToolWindowStatus>
      focusToolWindow: (toolId: Exclude<ToolId, 'mootool'>) => Promise<boolean>
      setToolWindowTitle: (toolId: Exclude<ToolId, 'mootool'>, title: string) => Promise<void>
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
      setJsonVaultEditorDirty: (dirty: boolean) => Promise<void>
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
      setQuickNoteEditorDirty: (dirty: boolean) => Promise<void>
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
      scanAiEnvironment: (input?: AiDiscoveryInput) => Promise<AiDoctorSnapshot>
      getAiModelRuntimeSnapshot: () => Promise<AiModelRuntimeSnapshot>
      inspectAiModelRuntimeModel: (input: AiModelRuntimeDetailInput) => Promise<AiModelRuntimeModelDetail>
      planAiModelRuntimeAction: (input: AiModelRuntimeActionPlanInput) => Promise<AiModelRuntimeActionPlan>
      executeAiModelRuntimeAction: (input: AiModelRuntimeActionExecuteInput) => Promise<AiModelRuntimeActionResult>
      cancelAiModelRuntimeAction: (requestId: string) => Promise<boolean>
      listAiPromptLabSuites: () => Promise<AiPromptLabSuite[]>
      saveAiPromptLabSuite: (input: AiPromptLabSuiteSaveInput) => Promise<AiPromptLabSuite>
      deleteAiPromptLabSuite: (id: string) => Promise<void>
      runAiPromptLab: (input: AiPromptLabRunInput) => Promise<AiPromptLabRunResult>
      cancelAiPromptLab: (requestId: string) => Promise<boolean>
      previewAiProjectStarter: (input: AiProjectStarterPreviewInput) => Promise<AiProjectStarterPreview>
      applyAiProjectStarter: (planId: string) => Promise<AiChangeApplyResult>
      rollbackAiProjectStarter: (snapshotId: string) => Promise<AiChangeRollbackResult>
      getAiUsageDashboard: (input: AiUsageDashboardInput) => Promise<AiUsageDashboard>
      chooseAiUsageFiles: () => Promise<string[]>
      previewAiUsageImport: (input: AiUsageImportPreviewInput) => Promise<AiUsageImportPreview>
      applyAiUsageImport: (planId: string, timezoneOffsetMinutes: number) => Promise<AiUsageImportResult>
      saveAiUsageBudget: (input: AiUsageBudgetInput) => Promise<AiUsageBudget>
      syncAiUsageProvider: (input: AiUsageProviderSyncInput) => Promise<AiUsageProviderSyncResult>
      clearAiUsage: () => Promise<number>
      exportAiUsage: (input: AiUsageExportInput) => Promise<AiUsageExportResult | null>
      getAiAgentManagerSnapshot: (input?: AiAgentManagerInput) => Promise<AiAgentManagerSnapshot>
      saveAiAgentProfile: (input: AiAgentProfileSaveInput) => Promise<AiAgentProfile>
      deleteAiAgentProfile: (id: string) => Promise<void>
      getAiAgentLaunchPlan: (id: string) => Promise<AiAgentLaunchPlan>
      runAiAgentTask: (input: AiAgentTaskStartInput) => Promise<AiAgentTaskResult>
      cancelAiAgentTask: (requestId: string) => Promise<boolean>
      exportAiAgentProfile: (id: string) => Promise<string | null>
      importAiAgentProfile: () => Promise<AiAgentProfileShareDocument | null>
      inspectAiContext: (input: AiContextInspectorInput) => Promise<AiContextInspectorSnapshot>
      previewClaudeCompatibilityEntry: (projectRoot: string) => Promise<AiChangePlan>
      applyClaudeCompatibilityEntry: (planId: string) => Promise<AiChangeApplyResult>
      rollbackClaudeCompatibilityEntry: (snapshotId: string) => Promise<AiChangeRollbackResult>
      previewEffectiveInstructions: (input: AiInstructionPreviewInput) => Promise<AiInstructionPreview>
      previewSkillInstall: (input: AiSkillInstallInput) => Promise<AiSkillInstallPreview>
      applySkillInstall: (input: AiSkillInstallApplyInput) => Promise<AiSkillInstallApplyResult>
      rollbackSkillInstall: (snapshotId: string) => Promise<AiSkillInstallRollbackResult>
      getMcpInventory: (input?: AiMcpInventoryInput) => Promise<AiMcpInventory>
      previewMcpCopy: (input: AiMcpCopyInput) => Promise<AiMcpCopyPreview>
      applyMcpCopy: (planId: string) => Promise<AiChangeApplyResult>
      rollbackMcpCopy: (snapshotId: string) => Promise<AiChangeRollbackResult>
      probeMcpServer: (input: AiMcpProbeInput) => Promise<AiMcpProbeResult>
      cancelMcpProbe: (requestId: string) => Promise<boolean>
      getAiMemorySnapshot: (input?: AiMemoryListInput) => Promise<AiMemorySnapshot>
      getAiNativeMemorySnapshot: () => Promise<AiNativeMemorySnapshot>
      getAiMemoryEmbeddingStatus: () => Promise<AiMemoryEmbeddingStatus>
      rebuildAiMemoryEmbeddings: (input: AiMemoryEmbeddingRebuildInput) => Promise<AiMemoryEmbeddingRebuildResult>
      previewAiMemoriesSemantic: (input: AiMemorySemanticPreviewInput) => Promise<AiMemorySemanticPreview>
      cancelAiMemoryEmbedding: (requestId: string) => Promise<boolean>
      saveAiMemory: (input: AiMemorySaveInput) => Promise<AiMemory>
      archiveAiMemory: (id: string) => Promise<AiMemory>
      restoreAiMemory: (id: string) => Promise<AiMemory>
      deleteAiMemory: (id: string) => Promise<void>
      createAiMemoryCandidate: (input: AiMemoryCandidateSaveInput) => Promise<AiMemoryCandidate>
      reviewAiMemoryCandidate: (input: AiMemoryCandidateReviewInput) => Promise<AiMemoryCandidate>
      previewAiMemories: (input: AiMemoryPreviewInput) => Promise<AiMemoryPreview>
      onSystemThemeChange: (callback: (theme: 'light' | 'dark') => void) => () => void
      onSettingsChange: (callback: (settings: AppSettings) => void) => () => void
      onSettingsNavigate: (callback: (category: string) => void) => () => void
      onNavigate: (callback: (event: AppNavigationEvent) => void) => () => void
      onToolWindowSnapshotChange: (callback: (snapshot: ToolWindowSnapshot) => void) => () => void
      onToolWindowStateChange: (callback: (state: ToolWindowStatus) => void) => () => void
      onToolWindowActivityChange: (callback: (active: boolean) => void) => () => void
      onToolWindowControlsVisibilityChange: (callback: (visible: boolean) => void) => () => void
      onJsonVaultChange: (callback: (relativePath: string) => void) => () => void
      onQuickNoteVaultChange: (callback: (relativePath: string) => void) => () => void
      onRuntimeOutput: (callback: (event: RuntimeOutputEvent) => void) => () => void
      onAiAgentTaskOutput: (callback: (event: AiAgentTaskOutputEvent) => void) => () => void
      onAiModelRuntimeActionProgress: (callback: (event: AiModelRuntimeActionProgressEvent) => void) => () => void
      onAiMemoryEmbeddingProgress: (callback: (event: AiMemoryEmbeddingProgressEvent) => void) => () => void
      onUpdateCheck: (callback: (event: UpdateCheckEvent) => void) => () => void
      onUpdateStateChange: (callback: (state: UpdateDownloadState) => void) => () => void
    }
  }
}

export {}
