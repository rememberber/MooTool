package com.rememberber.mootool.next.compose.app

import com.rememberber.mootool.next.compose.i18n.Translator
import com.rememberber.mootool.next.compose.model.AppLanguage
import com.rememberber.mootool.next.compose.model.AppSettings
import com.rememberber.mootool.next.compose.model.ThemePreference
import com.rememberber.mootool.next.compose.model.ToolId
import com.rememberber.mootool.next.compose.sessions.SessionManager
import com.rememberber.mootool.next.compose.storage.AppDatabase
import com.rememberber.mootool.next.compose.storage.HistoryRepository
import com.rememberber.mootool.next.compose.storage.JsonVault
import com.rememberber.mootool.next.compose.domain.DisplayWakeLock
import com.rememberber.mootool.next.compose.storage.ColorFavoriteStore
import com.rememberber.mootool.next.compose.storage.ImageLibraryStore
import com.rememberber.mootool.next.compose.storage.CronFavoriteStore
import com.rememberber.mootool.next.compose.storage.RegexFavoriteStore
import com.rememberber.mootool.next.compose.storage.SessionStore
import com.rememberber.mootool.next.compose.storage.SettingsRepository
import com.rememberber.mootool.next.compose.services.RegexWorkerClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path

class AppContainer(
    val directories: AppDirectories,
    val settingsRepository: SettingsRepository,
    val database: AppDatabase,
    val history: HistoryRepository,
    val sessions: SessionStore,
    val jsonVault: JsonVault
) {
    val regexFavorites = RegexFavoriteStore(directories)
    val cronFavorites = CronFavoriteStore(directories)
    val colorFavorites = ColorFavoriteStore(directories)
    val imageLibrary = ImageLibraryStore(directories)
    val displayWake = DisplayWakeLock()
    val regexWorker = RegexWorkerClient()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

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
        if (sessionManager.isDetached(id) && id != ToolId.Mootool) {
            _status.value = t("app.tool.detach")
            return
        }
        _activeTool.value = id
        updateSettings { current ->
            val recent = (listOf(id.id) + current.workspace.recentToolIds.filter { it != id.id }).take(5)
            current.copy(workspace = current.workspace.copy(activeToolId = id.id, recentToolIds = recent))
        }
    }

    fun openSettings(open: Boolean = true) {
        _showSettings.value = open
    }

    fun setSearchOpen(open: Boolean) {
        _searchOpen.value = open
    }

    fun setStatus(value: String) {
        _status.value = value
    }

    fun openExternal(uri: String) {
        runCatching { Desktop.getDesktop().browse(URI(uri)) }
    }

    fun openDirectory(path: Path) {
        runCatching { Desktop.getDesktop().open(path.toFile()) }
    }

    fun persistWorkspace() {
        sessionManager.persistJson()
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
        sessionManager.persistHardware()
        settingsRepository.save(_settings.value)
    }

    fun close() {
        regexWorker.cancel()
        displayWake.releaseAll()
        sessionManager.cancelNetCommands()
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
                sessions = SessionStore(database),
                jsonVault = JsonVault(directories)
            )
        }
    }
}
