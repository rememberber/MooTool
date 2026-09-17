package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.SessionManager
import com.rememberber.mootool.next.compose.ui.workbench.DetachPolicy
import com.rememberber.mootool.next.compose.ui.workbench.DetachedFocusRequest
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.LegacyMigrationRowRepository
import com.rememberber.mootool.next.compose.storage.DataPathConfig
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.BackupEngine
import com.rememberber.mootool.next.compose.storage.BackupExportResult
import com.rememberber.mootool.next.compose.storage.BackupInfo
import com.rememberber.mootool.next.compose.storage.BackupInfoResolver
import com.rememberber.mootool.next.compose.storage.BackupOpenLocation
import com.rememberber.mootool.next.compose.storage.BackupRestoreResult
import com.rememberber.mootool.next.compose.domain.DisplayWakeLock
import com.rememberber.mootool.next.compose.storage.ColorFavoriteStore
import com.rememberber.mootool.next.compose.storage.ImageLibraryStore
import com.rememberber.mootool.next.compose.storage.CronFavoriteStore
import com.rememberber.mootool.next.compose.storage.HostProfileStore
import com.rememberber.mootool.next.compose.storage.HttpCollectionStore
import com.rememberber.mootool.next.compose.storage.TranslationStore
import com.rememberber.mootool.next.compose.storage.RegexFavoriteStore
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.rememberber.mootool.next.compose.services.RegexWorkerClient
import com.rememberber.mootool.next.compose.domain.GitActionResult
import com.rememberber.mootool.next.compose.domain.GitEngine
import com.rememberber.mootool.next.compose.domain.GitIdentity
import com.rememberber.mootool.next.compose.domain.ElectronNextSettingsImport
import com.rememberber.mootool.next.compose.domain.ImportedLegacyHistory
import com.rememberber.mootool.next.compose.domain.LegacyToolDraftApplier
import com.rememberber.mootool.next.compose.domain.UpdateAutoCheckScheduler
import com.rememberber.mootool.next.compose.domain.VaultGitAutoPull
import com.rememberber.mootool.next.compose.domain.VaultGitCheckpointScheduler
import com.rememberber.mootool.next.compose.domain.VaultGitPullScheduler
import com.rememberber.mootool.next.compose.features.settings.SettingsNavCategory
import com.rememberber.mootool.next.compose.features.settings.settingsNavCategoryFromStorageId
import com.rememberber.mootool.next.compose.ai.AiDataAccessRequest
import com.rememberber.mootool.next.compose.ai.AiIntegrationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

class AppContainer(
    val directories: AppDirectories,
    val settingsRepository: SettingsRepository,
    val database: AppDatabase,
    val history: HistoryRepository,
    val migrationRows: LegacyMigrationRowRepository,
    val sessions: SessionStore
) {
    val regexFavorites: RegexFavoriteStore get() = RegexFavoriteStore(dataDirectories())
    val cronFavorites: CronFavoriteStore get() = CronFavoriteStore(dataDirectories())
    val colorFavorites: ColorFavoriteStore get() = ColorFavoriteStore(dataDirectories())
    val imageLibrary: ImageLibraryStore get() = ImageLibraryStore(dataDirectories())
    val hostProfiles: HostProfileStore get() = HostProfileStore(dataDirectories())
    val httpCollections: HttpCollectionStore get() = HttpCollectionStore(dataDirectories())
    val translations: TranslationStore get() = TranslationStore(dataDirectories())
    val displayWake = DisplayWakeLock()
    val regexWorker = RegexWorkerClient()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val updates = UpdateCoordinator(directories.cacheRoot, scope)
    val translator = Translator(AppLanguage.fromCode(settingsRepository.current.general.language))
    val sessionManager = SessionManager(sessions)
    private val _settings = MutableStateFlow(settingsRepository.current)
    val settings: StateFlow<AppSettings> = _settings
    private val _activeTool = MutableStateFlow(ToolId.fromId(settingsRepository.current.workspace.activeToolId) ?: ToolId.Mootool)
    val activeTool: StateFlow<ToolId> = _activeTool
    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings
    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen
    private val _groupManagerOpen = MutableStateFlow(false)
    val groupManagerOpen: StateFlow<Boolean> = _groupManagerOpen
    /** 设置页左侧当前分类（切工具再回设置时恢复，对照 Electron 会话内 `activeCategory`）。 */
    var settingsNavCategoryId: String = SettingsNavCategory.General.storageId()
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    val toasts = ToastQueue()
    private val _detachedFocus = MutableStateFlow<DetachedFocusRequest?>(null)
    val detachedFocus: StateFlow<DetachedFocusRequest?> = _detachedFocus
    private val _jsonVaultAutoPullTick = MutableStateFlow(0L)
    val jsonVaultAutoPullTick: StateFlow<Long> = _jsonVaultAutoPullTick
    private val _quickNoteVaultAutoPullTick = MutableStateFlow(0L)
    val quickNoteVaultAutoPullTick: StateFlow<Long> = _quickNoteVaultAutoPullTick
    private val _hostProfileMenuRevision = MutableStateFlow(0)
    val hostProfileMenuRevision: StateFlow<Int> = _hostProfileMenuRevision
    val jsonVault: JsonVault
        get() = JsonVault(dataDirectories(), _settings.value.vault.jsonPath)

    fun dataDirectories(): AppDirectories =
        DataPathConfig.withEffectiveDataRoot(directories, _settings.value.data.directory)
    private var gitJob: Job? = null
    private val updateAutoCheckScheduler = UpdateAutoCheckScheduler(
        scope = scope,
        enabled = { _settings.value.general.autoCheckUpdates },
        autoDownload = { _settings.value.general.autoDownloadUpdates },
        check = { autoDownload -> updates.check(autoDownload = autoDownload, automatic = true) },
    )
    private val noteGitScheduler = VaultGitCheckpointScheduler(
        enabled = { _settings.value.vault.autoCommit },
        hasUnsavedEditorChanges = { sessionManager.quickNoteSession().isVaultEditorDirty() },
        idleMilliseconds = { _settings.value.vault.autoCommitIdleSeconds.coerceIn(5, 3600) * 1_000L },
        inactiveMilliseconds = { _settings.value.vault.autoCommitInactiveSeconds.coerceIn(5, 3600) * 1_000L },
                checkpoint = { message -> checkpointVault(noteVault().root(), message) }
    )
    private val jsonGitScheduler = VaultGitCheckpointScheduler(
        enabled = { _settings.value.vault.autoCommit },
        hasUnsavedEditorChanges = { sessionManager.jsonSession().isVaultEditorDirty() },
        idleMilliseconds = { _settings.value.vault.autoCommitIdleSeconds.coerceIn(5, 3600) * 1_000L },
        inactiveMilliseconds = { _settings.value.vault.autoCommitInactiveSeconds.coerceIn(5, 3600) * 1_000L },
        checkpoint = { message -> checkpointVault(jsonVault.root(), message) }
    )
    private val notePullScheduler = VaultGitPullScheduler(
        enabled = { _settings.value.vault.autoPullMinutes > 0 },
        hasUnsavedEditorChanges = { sessionManager.quickNoteSession().isVaultEditorDirty() },
        intervalMilliseconds = { _settings.value.vault.autoPullMinutes.coerceIn(0, 1440) * 60_000L },
        pull = { runScheduledVaultPull(noteVault().root(), forJson = false) }
    )
    private val jsonPullScheduler = VaultGitPullScheduler(
        enabled = { _settings.value.vault.autoPullMinutes > 0 },
        hasUnsavedEditorChanges = { sessionManager.jsonSession().isVaultEditorDirty() },
        intervalMilliseconds = { _settings.value.vault.autoPullMinutes.coerceIn(0, 1440) * 60_000L },
        pull = { runScheduledVaultPull(jsonVault.root(), forJson = true) }
    )

    val aiIntegration: AiIntegrationService by lazy { AiIntegrationService(this) }

    fun t(key: String, params: Map<String, String> = emptyMap()): String = translator.t(key, params)

    fun themePreference(): ThemePreference = when (settings.value.appearance.theme.lowercase()) {
        "dark" -> ThemePreference.Dark
        "light" -> ThemePreference.Light
        else -> ThemePreference.System
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val previous = _settings.value
        val next = settingsRepository.update(transform)
        translator.setLanguage(AppLanguage.fromCode(next.general.language))
        _settings.value = next
        if (previous.data.directory != next.data.directory) {
            val nextDirs = dataDirectories()
            DataPathConfig.ensureDataRootExists(nextDirs)
            database.rebindDataDirectories(nextDirs)
        }
        if (previous.data.directory != next.data.directory ||
            previous.vault.quickNotePath != next.vault.quickNotePath ||
            previous.vault.jsonPath != next.vault.jsonPath
        ) {
            runCatching {
                aiIntegration.setDataAccess(AiDataAccessRequest(notes = false, json = false))
            }
        }
        if (previous.general.autoCheckUpdates != next.general.autoCheckUpdates) {
            configureAutomaticUpdateChecks()
        }
        if (!previous.general.autoDownloadUpdates && next.general.autoDownloadUpdates) {
            updates.applyAutoDownloadSetting(enabled = true)
        }
        if (previous.vault.autoPullMinutes != next.vault.autoPullMinutes ||
            previous.data.directory != next.data.directory ||
            previous.vault.jsonPath != next.vault.jsonPath ||
            previous.vault.quickNotePath != next.vault.quickNotePath
        ) {
            notePullScheduler.resetIntervalClock()
            jsonPullScheduler.resetIntervalClock()
        }
    }

    fun openTool(id: ToolId) {
        _showSettings.value = false
        _activeTool.value = id
        updateSettings { current ->
            current.copy(
                workspace = current.workspace.copy(
                    activeToolId = id.id,
                    recentToolIds = DetachPolicy.recentToolIds(id, current.workspace.recentToolIds)
                )
            )
        }
        if (DetachPolicy.shouldRequestDetachedFocus(id, sessionManager.detached.value)) {
            requestFocusDetached(id)
            val label = t(ToolRegistry.byId.getValue(id).titleKey)
            _status.value = t("app.tool.detachedTitle", mapOf("tool" to label))
        }
    }

    fun requestFocusDetached(id: ToolId) {
        if (!DetachPolicy.shouldRequestDetachedFocus(id, sessionManager.detached.value)) return
        val next = (_detachedFocus.value?.generation ?: 0) + 1
        _detachedFocus.value = DetachedFocusRequest(id, next)
    }

    fun toggleDetach(id: ToolId) {
        if (!id.detachable) return
        if (sessionManager.isDetached(id)) sessionManager.reattach(id) else sessionManager.detach(id)
    }

    fun openSettings(open: Boolean = true, categoryId: String? = null) {
        if (!categoryId.isNullOrBlank()) {
            settingsNavCategoryId = settingsNavCategoryFromStorageId(categoryId).storageId()
        }
        _showSettings.value = open
    }

    fun setSearchOpen(open: Boolean) {
        _searchOpen.value = open
    }

    fun setGroupManagerOpen(open: Boolean) {
        _groupManagerOpen.value = open
    }

    fun setStatus(value: String) {
        _status.value = value
    }

    fun toastSuccess(message: String) {
        toasts.success(message)
    }

    fun toastError(message: String) {
        toasts.error(message)
    }

    fun toastInfo(message: String) {
        toasts.info(message)
    }

    /** 查找/替换无命中：仅 info toast，不改工具状态栏 notice（对齐 Electron `toast.info(findReplace.noMatches)`）。 */
    fun toastFindNoMatches() {
        toastInfo(t("find.noMatches"))
    }

    fun toastCopied(success: Boolean) {
        if (success) toastSuccess(t("json.notice.copied")) else toastError(t("json.notice.copyFailed"))
    }

    fun copyText(value: String): Boolean {
        return try {
            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
            toastCopied(true)
            true
        } catch (_: Exception) {
            toastCopied(false)
            false
        }
    }

    fun noteVault(): NoteVault = NoteVault(dataDirectories(), _settings.value.vault.quickNotePath)

    fun recordVaultActivity(message: String, json: Boolean = false) {
        if (json) jsonGitScheduler.recordActivity(message) else noteGitScheduler.recordActivity(message)
    }

    fun setWindowActive(active: Boolean) {
        noteGitScheduler.setWindowActive(active)
        jsonGitScheduler.setWindowActive(active)
    }

    fun importedFingerprints(): Set<String> {
        val file = fingerprintFile()
        if (!file.exists()) return emptySet()
        return file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun rememberImport(vararg fingerprints: String) {
        val file = fingerprintFile()
        file.parent.createDirectories()
        val next = importedFingerprints() + fingerprints.filter { it.isNotBlank() }
        file.writeText(next.distinct().joinToString("\n"))
    }

    fun appliedLegacyDraftKeys(): Set<String> {
        val file = appliedDraftKeysFile()
        if (!file.exists()) return emptySet()
        return file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun rememberLegacyDraftKeys(keys: Collection<String>) {
        if (keys.isEmpty()) return
        val file = appliedDraftKeysFile()
        file.parent.createDirectories()
        val next = appliedLegacyDraftKeys() + keys.filter { it.isNotBlank() }
        file.writeText(next.joinToString("\n"))
    }

    private fun fingerprintFile(): Path = dataDirectories().dataRoot.resolve("imports").resolve("fingerprints.txt")

    private fun appliedDraftKeysFile(): Path = dataDirectories().dataRoot.resolve("imports").resolve("applied-tool-drafts.txt")

    private fun checkpointVault(root: Path, message: String) = GitEngine.automaticCheckpoint(
        root,
        message,
        GitEngine.identityFrom(_settings.value.vault.gitUsername),
        token = _settings.value.vault.gitToken
    )

    private fun runScheduledVaultPull(root: Path, forJson: Boolean): GitActionResult {
        val status = GitEngine.status(root)
        if (!VaultGitAutoPull.mayPullCleanWorkingTree(status)) {
            return GitActionResult(true, "skipped")
        }
        val result = GitEngine.pull(root, token = _settings.value.vault.gitToken)
        val after = GitEngine.status(root)
        if (VaultGitAutoPull.shouldNotifyVaultAfterPull(result, after)) {
            if (forJson) notifyJsonVaultTreeChanged() else notifyQuickNoteVaultTreeChanged()
        }
        return result
    }

    /** Electron `broadcast('json-vault:changed')` → Vault 面板 `load()` + 干净编辑器 `reloadSelectedFromDisk`。 */
    fun notifyJsonVaultTreeChanged() {
        _jsonVaultAutoPullTick.value += 1
    }

    /** Electron `broadcast('quick-note-vault:changed')` 同上。 */
    fun notifyQuickNoteVaultTreeChanged() {
        _quickNoteVaultAutoPullTick.value += 1
    }

    fun openExternal(uri: String) {
        runCatching { Desktop.getDesktop().browse(URI(uri)) }
    }

    fun openDirectory(path: Path) {
        runCatching { Desktop.getDesktop().open(path.toFile()) }
    }

    fun chooseDirectory(title: String, initialPath: String = ""): String? =
        DesktopFileDialogs.chooseDirectory(title, initialPath)

    fun chooseExecutable(title: String, initialPath: String = ""): String? =
        DesktopFileDialogs.chooseExecutable(title, initialPath)

    fun defaultLegacyImportSource(): String {
        val legacy = Path.of(System.getProperty("user.home"), ".MooTool")
        return if (java.nio.file.Files.isDirectory(legacy)) legacy.toAbsolutePath().normalize().toString() else ""
    }

    fun revealInFileManager(path: Path) {
        val file = path.toFile()
        val desktop = Desktop.getDesktop()
        runCatching {
            if (file.isFile && desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                desktop.browseFileDirectory(file)
            } else {
                desktop.open(if (file.isDirectory) file else file.parentFile ?: file)
            }
        }.recoverCatching {
            desktop.open(if (file.isDirectory) file else file.parentFile ?: file)
        }
    }

    fun exportBackup(zipPath: Path): BackupExportResult {
        persistWorkspace()
        return BackupEngine.export(dataDirectories(), zipPath, database)
    }

    fun previewBackup(zipPath: Path) = BackupEngine.preview(zipPath)

    fun backupInfo(): BackupInfo =
        BackupInfoResolver.resolve(dataDirectories(), noteVault().root(), jsonVault.root())

    fun openBackupLocation(location: BackupOpenLocation) {
        val target = backupInfo().pathForOpen(location)
        when (location) {
            BackupOpenLocation.DatabaseFile -> {
                runCatching { target.parent?.createDirectories() }
                revealInFileManager(target)
            }
            BackupOpenLocation.SettingsConfig -> {
                runCatching { target.createDirectories() }
                openDirectory(target)
            }
            BackupOpenLocation.Images,
            BackupOpenLocation.QuickNote,
            BackupOpenLocation.JsonVault,
            -> {
                runCatching { target.createDirectories() }
                openDirectory(target)
            }
        }
    }

    /** 备份恢复、跨产品导入等改写磁盘/SQLite 后，在既有会话实例上重载 `tool_sessions` 并递增 `sessionGeneration`。 */
    fun reloadToolSessionsFromStore() {
        sessionManager.reloadAllToolSessionsFromStore()
        notifyJsonVaultTreeChanged()
        notifyQuickNoteVaultTreeChanged()
    }

    fun restoreBackup(zipPath: Path): BackupRestoreResult {
        persistWorkspace()
        val result = BackupEngine.restore(zipPath, dataDirectories(), database)
        val loaded = settingsRepository.load()
        translator.setLanguage(AppLanguage.fromCode(loaded.general.language))
        _settings.value = loaded
        val nextDirs = dataDirectories()
        DataPathConfig.ensureDataRootExists(nextDirs)
        database.rebindDataDirectories(nextDirs)
        runCatching {
            aiIntegration.setDataAccess(AiDataAccessRequest(notes = false, json = false))
        }
        reloadToolSessionsFromStore()
        return result
    }

    fun mergeElectronCodeRunFromStore(store: Path): Boolean {
        val patch = ElectronNextSettingsImport.loadCodeRunPatchFromStore(store) ?: return false
        if (!ElectronNextSettingsImport.hasCodeRunPatch(patch)) return false
        val session = sessionManager.codeRunSession()
        val merged = ElectronNextSettingsImport.mergeCodeRunSnapshots(session.snapshotState(), patch)
        session.restore(merged)
        sessionManager.persistCodeRun()
        return true
    }

    fun applyLegacyToolDrafts(rows: List<ImportedLegacyHistory>): Int =
        LegacyToolDraftApplier.apply(sessionManager, rows, history)

    fun persistWorkspace() {
        sessionManager.persistJson()
        sessionManager.persistQuickNote()
        sessionManager.persistTime()
        sessionManager.persistCalculator()
        sessionManager.persistEncode()
        sessionManager.persistUa()
        sessionManager.persistRegex()
        sessionManager.persistCron()
        sessionManager.persistDiff()
        sessionManager.persistReformat()
        sessionManager.persistConfig()
        sessionManager.persistProtobuf()
        sessionManager.persistCrypto()
        sessionManager.persistQr()
        sessionManager.persistColor()
        sessionManager.persistMessageBoard()
        sessionManager.persistPdf()
        sessionManager.persistImage()
        sessionManager.persistNet()
        sessionManager.persistVariables()
        sessionManager.persistHost()
        sessionManager.persistHttp()
        sessionManager.persistTranslation()
        sessionManager.persistCodeRun()
        sessionManager.persistHardware()
        settingsRepository.save(_settings.value)
    }

    fun configureAutomaticUpdateChecks() {
        updateAutoCheckScheduler.reconfigure()
    }

    fun notifyHostProfileMenuChanged() {
        _hostProfileMenuRevision.value++
    }

    fun startBackgroundTasks() {
        configureAutomaticUpdateChecks()
        if (gitJob != null) return
        gitJob = scope.launch {
            while (isActive) {
                delay(5_000)
                runCatching { noteGitScheduler.evaluate() }
                runCatching { jsonGitScheduler.evaluate() }
                runCatching { notePullScheduler.evaluate() }
                runCatching { jsonPullScheduler.evaluate() }
            }
        }
    }

    fun close() {
        gitJob?.cancel()
        gitJob = null
        updateAutoCheckScheduler.stop()
        updates.cancel()
        regexWorker.cancel()
        displayWake.releaseAll()
        sessionManager.cancelNetCommands()
        sessionManager.cancelHttp()
        sessionManager.cancelTranslation()
        sessionManager.cancelCodeRun()
        persistWorkspace()
        database.close()
    }

    companion object {
        fun create(dataDir: String? = System.getenv(ProductIdentity.DATA_DIR_ENV)): AppContainer {
            val directories = AppPaths.resolve(dataDir)
            directories.ensureCreated()
            val settings = SettingsRepository(directories).also { it.load() }
            val dataDirs = DataPathConfig.withEffectiveDataRoot(directories, settings.current.data.directory)
            DataPathConfig.ensureDataRootExists(dataDirs)
            val database = AppDatabase(dataDirs)
            return AppContainer(
                directories = directories,
                settingsRepository = settings,
                database = database,
                history = HistoryRepository(database),
                migrationRows = LegacyMigrationRowRepository(database),
                sessions = SessionStore(database)
            ).also { it.startBackgroundTasks() }
        }
    }
}
