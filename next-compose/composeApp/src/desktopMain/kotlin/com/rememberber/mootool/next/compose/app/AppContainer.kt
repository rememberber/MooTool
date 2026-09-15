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
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.storage.NoteVault
import com.rememberber.mootool.next.compose.storage.BackupEngine
import com.rememberber.mootool.next.compose.storage.BackupExportResult
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
import com.rememberber.mootool.next.compose.domain.GitEngine
import com.rememberber.mootool.next.compose.domain.GitIdentity
import com.rememberber.mootool.next.compose.domain.VaultGitCheckpointScheduler
import com.rememberber.mootool.next.compose.domain.VaultGitPullScheduler
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
    val sessions: SessionStore
) {
    val regexFavorites = RegexFavoriteStore(directories)
    val cronFavorites = CronFavoriteStore(directories)
    val colorFavorites = ColorFavoriteStore(directories)
    val imageLibrary = ImageLibraryStore(directories)
    val hostProfiles = HostProfileStore(directories)
    val httpCollections = HttpCollectionStore(directories)
    val translations = TranslationStore(directories)
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
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status
    private val _detachedFocus = MutableStateFlow<DetachedFocusRequest?>(null)
    val detachedFocus: StateFlow<DetachedFocusRequest?> = _detachedFocus
    val jsonVault: JsonVault
        get() = JsonVault(directories, _settings.value.vault.jsonPath)
    private var gitJob: Job? = null
    private val noteGitScheduler = VaultGitCheckpointScheduler(
        enabled = { _settings.value.vault.autoCommit },
        hasUnsavedEditorChanges = {
            val session = sessionManager.quickNoteSession()
            session.currentFile.isNotBlank() && session.editor.text != session.savedText
        },
        idleMilliseconds = { _settings.value.vault.autoCommitIdleSeconds.coerceIn(5, 3600) * 1_000L },
        inactiveMilliseconds = { _settings.value.vault.autoCommitInactiveSeconds.coerceIn(5, 3600) * 1_000L },
                checkpoint = { message -> checkpointVault(noteVault().root(), message) }
    )
    private val jsonGitScheduler = VaultGitCheckpointScheduler(
        enabled = { _settings.value.vault.autoCommit },
        hasUnsavedEditorChanges = {
            val session = sessionManager.jsonSession()
            session.currentFile.isNotBlank() && session.editor.text != session.savedText
        },
        idleMilliseconds = { _settings.value.vault.autoCommitIdleSeconds.coerceIn(5, 3600) * 1_000L },
        inactiveMilliseconds = { _settings.value.vault.autoCommitInactiveSeconds.coerceIn(5, 3600) * 1_000L },
        checkpoint = { message -> checkpointVault(jsonVault.root(), message) }
    )
    private val notePullScheduler = VaultGitPullScheduler(
        enabled = { _settings.value.vault.autoPullMinutes > 0 },
        hasUnsavedEditorChanges = {
            val session = sessionManager.quickNoteSession()
            session.currentFile.isNotBlank() && session.editor.text != session.savedText
        },
        intervalMilliseconds = { _settings.value.vault.autoPullMinutes.coerceIn(0, 1440) * 60_000L },
        pull = { pullVault(noteVault().root()) }
    )
    private val jsonPullScheduler = VaultGitPullScheduler(
        enabled = { _settings.value.vault.autoPullMinutes > 0 },
        hasUnsavedEditorChanges = {
            val session = sessionManager.jsonSession()
            session.currentFile.isNotBlank() && session.editor.text != session.savedText
        },
        intervalMilliseconds = { _settings.value.vault.autoPullMinutes.coerceIn(0, 1440) * 60_000L },
        pull = { pullVault(jsonVault.root()) }
    )

    fun t(key: String, params: Map<String, String> = emptyMap()): String = translator.t(key, params)

    fun themePreference(): ThemePreference = when (settings.value.appearance.theme.lowercase()) {
        "dark" -> ThemePreference.Dark
        "light" -> ThemePreference.Light
        else -> ThemePreference.System
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = settingsRepository.update(transform)
        translator.setLanguage(AppLanguage.fromCode(next.general.language))
        _settings.value = next
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

    fun openSettings(open: Boolean = true) {
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

    fun noteVault(): NoteVault = NoteVault(directories, _settings.value.vault.quickNotePath)

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

    fun rememberImport(fingerprint: String) {
        val file = fingerprintFile()
        file.parent.createDirectories()
        val next = importedFingerprints() + fingerprint
        file.writeText(next.joinToString("\n"))
    }

    private fun fingerprintFile(): Path = directories.dataRoot.resolve("imports").resolve("fingerprints.txt")

    private fun checkpointVault(root: Path, message: String) = GitEngine.automaticCheckpoint(
        root,
        message,
        GitEngine.identityFrom(_settings.value.vault.gitUsername),
        token = _settings.value.vault.gitToken
    )

    private fun pullVault(root: Path): com.rememberber.mootool.next.compose.domain.GitActionResult {
        val status = GitEngine.status(root)
        if (!status.available || !status.repository || status.remote.isBlank() || status.merging) {
            return com.rememberber.mootool.next.compose.domain.GitActionResult(true, "skipped")
        }
        return GitEngine.pull(root, token = _settings.value.vault.gitToken)
    }

    fun openExternal(uri: String) {
        runCatching { Desktop.getDesktop().browse(URI(uri)) }
    }

    fun openDirectory(path: Path) {
        runCatching { Desktop.getDesktop().open(path.toFile()) }
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
        return BackupEngine.export(directories, zipPath, database)
    }

    fun previewBackup(zipPath: Path) = BackupEngine.preview(zipPath)

    fun restoreBackup(zipPath: Path): BackupRestoreResult {
        persistWorkspace()
        val result = BackupEngine.restore(zipPath, directories, database)
        val loaded = settingsRepository.load()
        translator.setLanguage(AppLanguage.fromCode(loaded.general.language))
        _settings.value = loaded
        return result
    }

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

    fun startBackgroundTasks() {
        if (settings.value.general.autoCheckUpdates) {
            updates.check(autoDownload = settings.value.general.autoDownloadUpdates)
        }
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
            val database = AppDatabase(directories)
            return AppContainer(
                directories = directories,
                settingsRepository = settings,
                database = database,
                history = HistoryRepository(database),
                sessions = SessionStore(database)
            ).also { it.startBackgroundTasks() }
        }
    }
}
