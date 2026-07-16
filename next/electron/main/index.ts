import {
  app,
  BrowserWindow,
  clipboard,
  desktopCapturer,
  dialog,
  ipcMain,
  Menu,
  nativeImage,
  nativeTheme,
  safeStorage,
  screen,
  shell,
  Tray,
  type MenuItemConstructorOptions,
  type OpenDialogOptions
} from 'electron'
import Store from 'electron-store'
import { execFile } from 'node:child_process'
import { createHash, getHashes } from 'node:crypto'
import { createReadStream, watch, type FSWatcher } from 'node:fs'
import { chmod, mkdir, readFile, stat, writeFile } from 'node:fs/promises'
import { basename, extname, join } from 'node:path'
import {
  defaultWorkspaceState,
  type AppNavigationEvent,
  type AppPaths,
  type RuntimeId,
  type RuntimeStatus,
  type WindowState,
  type WorkspaceState
} from '../../src/shared/contracts/app'
import {
  defaultAppSettings,
  mergeSettings,
  type AppLanguage,
  type AppSettings,
  type SecretKey,
  type SecretStatus,
  type SettingsPatch
} from '../../src/shared/contracts/settings'
import type { HistoryQuery, SaveFuncHistoryInput } from '../../src/shared/contracts/history'
import type { SaveTextFileInput, TextFileKind, TextFileResult } from '../../src/shared/contracts/files'
import type { RenameImageAssetInput, SaveImageAssetInput, ScreenCapture } from '../../src/shared/contracts/images'
import { digestAlgorithmIds, type DigestAlgorithmId, type DigestFileResult, type ImageFilePayload, type SaveBinaryFileInput } from '../../src/shared/contracts/nativeFiles'
import type { PdfMergeSource, PdfSplitTask } from '../../src/shared/contracts/pdf'
import type {
  JsonVaultListInput,
  MoveJsonVaultEntryInput,
  RenameJsonVaultEntryInput,
  SaveJsonVaultFileInput
} from '../../src/shared/contracts/jsonVault'
import type {
  CreateQuickNoteInput,
  MoveQuickNoteEntryInput,
  QuickNoteListInput,
  RenameQuickNoteEntryInput,
  SaveQuickNoteInput
} from '../../src/shared/contracts/quickNote'
import type { VaultGitActionInput, VaultGitDiffInput } from '../../src/shared/contracts/vaultGit'
import { codeRuntimeIds, type RuntimeExecutionInput } from '../../src/shared/contracts/runtime'
import { favoriteKinds, type FavoriteKind, type SaveFavoriteInput } from '../../src/shared/contracts/favorites'
import { backupKinds, type BackupKind, type BackupLocation } from '../../src/shared/contracts/backup'
import type { LegacyMigrationInput } from '../../src/shared/contracts/migration'
import type { UpdateCheckEvent, UpdateCheckResult } from '../../src/shared/contracts/update'
import { BackupService } from './backupService'
import { FavoriteRepository } from './favoriteRepository'
import { HistoryRepository } from './historyRepository'
import { ImageRepository } from './imageRepository'
import { JsonVaultRepository } from './jsonVaultRepository'
import { LegacyMigrationService } from './legacyMigrationService'
import { NetworkService, type ProxyConfiguration } from './networkService'
import { P5Repository } from './p5Repository'
import { QuickNoteVaultRepository } from './quickNoteVaultRepository'
import { RuntimeExecutionService } from './runtimeExecutionService'
import {
  normalizeHostInput,
  normalizeHttpRequest,
  normalizeHttpResponse,
  normalizeHttpSendInput,
  normalizeKeyword,
  normalizeNetworkInput,
  normalizePositiveId,
  normalizeRequestId,
  normalizeTranslationHistoryInput,
  normalizeTranslationInput,
  normalizeTranslationWordInput
} from './p5Validation'
import { inspectPdf, mergePdfs, splitPdfs } from './pdfService'
import { SystemService } from './systemService'
import { defaultReleaseUrl, UpdateService } from './updateService'
import { VaultGitService } from './vaultGitService'

type PersistedStore = {
  settings: AppSettings
  workspace: WorkspaceState
  window: WindowState
  secrets: Partial<Record<SecretKey, string>>
}

const isDev = Boolean(process.env.ELECTRON_RENDERER_URL)
const defaultWindowState: WindowState = {
  bounds: { width: 1440, height: 920 },
  maximized: false
}

let store: Store<PersistedStore>
let mainWindow: BrowserWindow | null = null
let settingsWindow: BrowserWindow | null = null
let tray: Tray | null = null
let isQuitting = false
let closePromptOpen = false
let historyRepository: HistoryRepository
let favoriteRepository: FavoriteRepository
let p5Repository: P5Repository
const networkService = new NetworkService()
let systemService: SystemService
let runtimeExecutionService: RuntimeExecutionService
let gitAskPassPath = ''
let quickNoteWatcher: FSWatcher | null = null
let quickNoteWatchTimer: NodeJS.Timeout | undefined
let quickNotePullTimer: NodeJS.Timeout | undefined
let quickNoteWatcherGeneration = 0
let jsonVaultWatcher: FSWatcher | null = null
let jsonVaultWatchTimer: NodeJS.Timeout | undefined
let jsonVaultPullTimer: NodeJS.Timeout | undefined
let jsonVaultWatcherGeneration = 0
let updateStartupTimer: NodeJS.Timeout | undefined
let updateIntervalTimer: NodeJS.Timeout | undefined
let updateCheckPromise: Promise<UpdateCheckResult> | null = null
const allowedPdfPaths = new Set<string>()
const updateService = new UpdateService(process.env.MOOTOOL_UPDATE_FEED_URL || undefined)

function getDevelopmentIconPath(): string {
  const filename = process.platform === 'darwin' ? 'icon-mac.png' : process.platform === 'win32' ? 'icon.ico' : 'icon.png'
  return join(__dirname, '../../resources', filename)
}

function getIconPath(): string {
  if (!app.isPackaged) {
    return join(__dirname, '../../resources', 'icon.png')
  }
  return join(process.resourcesPath, 'tray-icon.png')
}

function loadRenderer(window: BrowserWindow, target: 'main' | 'settings'): void {
  if (isDev && process.env.ELECTRON_RENDERER_URL) {
    const url = new URL(process.env.ELECTRON_RENDERER_URL)
    if (target === 'settings') {
      url.searchParams.set('window', 'settings')
    }
    void window.loadURL(url.toString())
    return
  }

  void window.loadFile(join(__dirname, '../renderer/index.html'), target === 'settings' ? { query: { window: 'settings' } } : undefined)
}

function createMainWindow(): BrowserWindow {
  const savedWindow = store.get('window', defaultWindowState)
  const settings = store.get('settings')
  const dark = nativeTheme.shouldUseDarkColors
  const window = new BrowserWindow({
    ...savedWindow.bounds,
    minWidth: 1080,
    minHeight: 720,
    show: false,
    backgroundColor: dark ? '#171719' : '#f7f7f8',
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 18, y: 18 },
    vibrancy: process.platform === 'darwin' ? 'sidebar' : undefined,
    visualEffectState: process.platform === 'darwin' ? 'active' : undefined,
    icon: app.isPackaged ? undefined : getDevelopmentIconPath(),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow = window
  installWindowStatePersistence(window)

  window.once('ready-to-show', () => {
    if (settings.general.startMaximized || savedWindow.maximized) {
      window.maximize()
    }
    window.show()
  })

  window.on('close', (event) => {
    if (isQuitting) {
      return
    }

    const behavior = store.get('settings').general.closeBehavior
    if (behavior === 'quit') {
      isQuitting = true
      app.quit()
      return
    }

    event.preventDefault()
    if (behavior === 'hide') {
      window.hide()
      return
    }

    void confirmClose(window)
  })

  window.on('closed', () => {
    mainWindow = null
  })

  loadRenderer(window, 'main')
  return window
}

function createSettingsWindow(): BrowserWindow {
  if (settingsWindow && !settingsWindow.isDestroyed()) {
    settingsWindow.show()
    settingsWindow.focus()
    return settingsWindow
  }

  const dark = nativeTheme.shouldUseDarkColors
  const window = new BrowserWindow({
    width: 920,
    height: 700,
    minWidth: 820,
    minHeight: 620,
    parent: mainWindow ?? undefined,
    modal: true,
    show: false,
    backgroundColor: dark ? '#171719' : '#f7f7f8',
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 18, y: 18 },
    resizable: true,
    minimizable: false,
    maximizable: false,
    icon: app.isPackaged ? undefined : getDevelopmentIconPath(),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: true,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  settingsWindow = window
  window.once('ready-to-show', () => window.show())
  window.on('closed', () => {
    settingsWindow = null
  })
  loadRenderer(window, 'settings')
  return window
}

function installWindowStatePersistence(window: BrowserWindow): void {
  let timer: NodeJS.Timeout | undefined
  const save = () => {
    if (window.isDestroyed()) {
      return
    }
    const bounds = window.isMaximized() ? window.getNormalBounds() : window.getBounds()
    store.set('window', { bounds, maximized: window.isMaximized() })
  }
  const schedule = () => {
    clearTimeout(timer)
    timer = setTimeout(save, 250)
  }

  window.on('move', schedule)
  window.on('resize', schedule)
  window.on('maximize', save)
  window.on('unmaximize', save)
  window.on('closed', () => clearTimeout(timer))
}

async function confirmClose(window: BrowserWindow): Promise<void> {
  if (closePromptOpen) {
    return
  }
  closePromptOpen = true
  const language = store.get('settings').general.language
  const labels = closeDialogLabels[language]

  try {
    const result = await dialog.showMessageBox(window, {
      type: 'question',
      title: 'MooTool',
      message: labels.message,
      buttons: [labels.hide, labels.quit, labels.cancel],
      defaultId: 0,
      cancelId: 2,
      noLink: true
    })
    if (result.response === 0) {
      window.hide()
    } else if (result.response === 1) {
      isQuitting = true
      app.quit()
    }
  } finally {
    closePromptOpen = false
  }
}

function registerIpc(): void {
  ipcMain.handle('app:get-version', () => app.getVersion())
  ipcMain.handle('app:get-paths', (): AppPaths => ({
    userData: app.getPath('userData'),
    documents: app.getPath('documents'),
    downloads: app.getPath('downloads')
  }))
  ipcMain.handle('theme:get-system', () => (nativeTheme.shouldUseDarkColors ? 'dark' : 'light'))
  ipcMain.handle('settings:get', () => store.get('settings'))
  ipcMain.handle('settings:update', (_event, patch: SettingsPatch) => {
    const previous = store.get('settings')
    const settings = mergeSettings(previous, isRecord(patch) ? patch : {})
    store.set('settings', settings)
    if (previous.data.directory !== settings.data.directory) {
      closeDataRepositories()
      try {
        openDataRepositories(settings)
      } catch (error) {
        store.set('settings', previous)
        openDataRepositories(previous)
        throw error
      }
    }
    applySettings(settings)
    broadcast('settings:changed', settings)
    return settings
  })
  ipcMain.handle('settings:open', () => {
    createSettingsWindow()
  })
  ipcMain.handle('settings:close', (event) => {
    BrowserWindow.fromWebContents(event.sender)?.close()
  })
  ipcMain.handle('update:check', () => checkForUpdates())
  ipcMain.handle('update:open-release', async () => {
    await shell.openExternal(defaultReleaseUrl)
  })
  ipcMain.handle('app:open-project', async () => {
    await shell.openExternal('https://github.com/rememberber/MooTool')
  })
  ipcMain.handle('secret:status', (_event, key: SecretKey): SecretStatus => {
    const normalizedKey = normalizeSecretKey(key)
    return {
      key: normalizedKey,
      stored: Boolean(store.get('secrets')[normalizedKey]),
      encryptionAvailable: safeStorage.isEncryptionAvailable()
    }
  })
  ipcMain.handle('secret:set', (_event, key: SecretKey, value: string): SecretStatus => {
    const normalizedKey = normalizeSecretKey(key)
    if (!safeStorage.isEncryptionAvailable()) {
      throw new Error('Secure storage is unavailable')
    }
    if (typeof value !== 'string' || value.length > 8192) {
      throw new Error('Invalid secret value')
    }
    const secrets = store.get('secrets')
    if (value) {
      secrets[normalizedKey] = safeStorage.encryptString(value).toString('base64')
    } else {
      delete secrets[normalizedKey]
    }
    store.set('secrets', secrets)
    return { key: normalizedKey, stored: Boolean(value), encryptionAvailable: true }
  })
  ipcMain.handle('secret:clear', (_event, key: SecretKey): SecretStatus => {
    const normalizedKey = normalizeSecretKey(key)
    const secrets = store.get('secrets')
    delete secrets[normalizedKey]
    store.set('secrets', secrets)
    return { key: normalizedKey, stored: false, encryptionAvailable: safeStorage.isEncryptionAvailable() }
  })
  ipcMain.handle('workspace:get', () => store.get('workspace'))
  ipcMain.handle('workspace:set', (_event, nextState: WorkspaceState) => {
    const state = normalizeWorkspaceState(nextState)
    store.set('workspace', state)
    return state
  })
  ipcMain.handle('history:list', (_event, query: HistoryQuery) => historyRepository.list(normalizeHistoryQuery(query)))
  ipcMain.handle('history:save', (_event, input: SaveFuncHistoryInput) => {
    historyRepository.save(normalizeHistoryInput(input))
  })
  ipcMain.handle('history:delete', (_event, id: number) => {
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Invalid history id')
    historyRepository.delete(id)
  })
  ipcMain.handle('history:clear', (_event, funcType: string) => historyRepository.clear(normalizeFuncType(funcType)))
  ipcMain.handle('favorite:list', (_event, kind: FavoriteKind) => favoriteRepository.list(normalizeFavoriteKind(kind)))
  ipcMain.handle('favorite:save', (_event, input: SaveFavoriteInput) => favoriteRepository.save(normalizeFavoriteInput(input)))
  ipcMain.handle('favorite:delete', (_event, id: number) => {
    if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Invalid favorite id')
    favoriteRepository.delete(id)
  })
  ipcMain.handle('http:list', (_event, keyword?: string) => p5Repository.listHttpRequests(normalizeKeyword(keyword)))
  ipcMain.handle('http:save', (_event, request: unknown, response?: unknown) => p5Repository.saveHttpRequest(normalizeHttpRequest(request), response == null ? undefined : normalizeHttpResponse(response)))
  ipcMain.handle('http:delete', (_event, id: unknown) => p5Repository.deleteHttpRequest(normalizePositiveId(id)))
  ipcMain.handle('http:history-list', (_event, keyword?: string) => p5Repository.listHttpHistory(normalizeKeyword(keyword)))
  ipcMain.handle('http:history-delete', (_event, id: unknown) => p5Repository.deleteHttpHistory(normalizePositiveId(id)))
  ipcMain.handle('http:history-clear', () => p5Repository.clearHttpHistory())
  ipcMain.handle('http:send', async (_event, value: unknown) => {
    const input = normalizeHttpSendInput(value)
    const response = await networkService.sendHttp(input, getProxyConfiguration())
    p5Repository.saveHttpHistory(input.request, response)
    return response
  })
  ipcMain.handle('network:cancel', (_event, requestId: unknown) => networkService.cancel(normalizeRequestId(requestId)))
  ipcMain.handle('translation:send', async (_event, value: unknown) => {
    const input = normalizeTranslationInput(value)
    const result = await networkService.translate(input, getProxyConfiguration())
    p5Repository.saveTranslationHistory({ sourceText: input.text, targetText: result.text, sourceLang: input.sourceLang, targetLang: input.targetLang, translatorType: result.provider })
    return result
  })
  ipcMain.handle('translation:words-list', (_event, keyword?: string) => p5Repository.listTranslationWords(normalizeKeyword(keyword)))
  ipcMain.handle('translation:words-save', (_event, value: unknown) => p5Repository.saveTranslationWord(normalizeTranslationWordInput(value)))
  ipcMain.handle('translation:words-delete', (_event, id: unknown) => p5Repository.deleteTranslationWord(normalizePositiveId(id)))
  ipcMain.handle('translation:history-list', (_event, keyword?: string) => p5Repository.listTranslationHistory(normalizeKeyword(keyword)))
  ipcMain.handle('translation:history-save', (_event, value: unknown) => p5Repository.saveTranslationHistory(normalizeTranslationHistoryInput(value)))
  ipcMain.handle('translation:history-delete', (_event, id: unknown) => p5Repository.deleteTranslationHistory(normalizePositiveId(id)))
  ipcMain.handle('translation:history-clear', () => p5Repository.clearTranslationHistory())
  ipcMain.handle('host:list', (_event, keyword?: string) => p5Repository.listHosts(normalizeKeyword(keyword)))
  ipcMain.handle('host:save', (_event, value: unknown) => p5Repository.saveHost(normalizeHostInput(value)))
  ipcMain.handle('host:delete', (_event, id: unknown) => p5Repository.deleteHost(normalizePositiveId(id)))
  ipcMain.handle('host:read-system', () => systemService.readHosts())
  ipcMain.handle('host:write-system', (_event, content: unknown) => systemService.writeHosts(typeof content === 'string' ? content : ''))
  ipcMain.handle('system:network-command', (_event, value: unknown) => systemService.runNetwork(normalizeNetworkInput(value)))
  ipcMain.handle('system:cancel', (_event, requestId: unknown) => systemService.cancel(normalizeRequestId(requestId)))
  ipcMain.handle('system:environment', () => systemService.getEnvironment())
  ipcMain.handle('system:local-addresses', () => systemService.getLocalAddresses())
  ipcMain.handle('system:info', () => systemService.getSystemInfo())
  ipcMain.handle('runtime:run', (event, value: unknown) => {
    const input = normalizeRuntimeExecutionInput(value)
    const settings = store.get('settings')
    return runtimeExecutionService.run(input, {
      java: settings.runtime.javaPath,
      groovy: settings.runtime.groovyPath,
      python: settings.runtime.pythonPath,
      node: settings.runtime.nodePath
    }, (output) => {
      if (!event.sender.isDestroyed()) event.sender.send('runtime:output', output)
    })
  })
  ipcMain.handle('runtime:cancel', (_event, requestId: unknown) => runtimeExecutionService.cancel(normalizeRequestId(requestId)))
  ipcMain.handle('runtime:detect', () => detectRuntimes(store.get('settings')))
  ipcMain.handle('dialog:choose-directory', async (event, initialPath?: string) => {
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = {
      defaultPath: typeof initialPath === 'string' && initialPath ? initialPath : undefined,
      properties: ['openDirectory', 'createDirectory']
    }
    const result = owner
      ? await dialog.showOpenDialog(owner, options)
      : await dialog.showOpenDialog(options)
    return result.canceled ? null : result.filePaths[0] ?? null
  })
  ipcMain.handle('files:open-text', async (event, kind: TextFileKind): Promise<TextFileResult | null> => {
    const normalizedKind = normalizeTextFileKind(kind)
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = {
      properties: ['openFile'],
      filters: [textFileFilters[normalizedKind]]
    }
    const result = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    if (result.canceled || !result.filePaths[0]) return null
    const path = result.filePaths[0]
    const fileStat = await stat(path)
    if (fileStat.size > 20 * 1024 * 1024) throw new Error('File exceeds 20 MB limit')
    return { path, name: basename(path), content: await readFile(path, 'utf8') }
  })
  ipcMain.handle('files:save-text', async (event, input: SaveTextFileInput): Promise<string | null> => {
    if (!isRecord(input) || typeof input.content !== 'string' || input.content.length > 20 * 1024 * 1024) {
      throw new Error('Invalid text file')
    }
    const kind = normalizeTextFileKind(input.kind)
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const defaultName = sanitizeDefaultFileName(input.defaultName, kind)
    const options = { defaultPath: defaultName, filters: [textFileFilters[kind]] }
    const result = owner ? await dialog.showSaveDialog(owner, options) : await dialog.showSaveDialog(options)
    if (result.canceled || !result.filePath) return null
    await writeFile(result.filePath, input.content, 'utf8')
    return result.filePath
  })
  ipcMain.handle('files:digest', async (event, algorithm: DigestAlgorithmId): Promise<DigestFileResult | null> => {
    const normalizedAlgorithm = normalizeDigestAlgorithm(algorithm)
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = { properties: ['openFile'] }
    const result = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    if (result.canceled || !result.filePaths[0]) return null
    const path = result.filePaths[0]
    const fileStat = await stat(path)
    const digest = await hashFile(path, normalizedAlgorithm)
    return { path, name: basename(path), size: fileStat.size, digest }
  })
  ipcMain.handle('files:choose-image', async (event): Promise<ImageFilePayload | null> => {
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = { properties: ['openFile'], filters: [imageFileFilter] }
    const result = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    if (result.canceled || !result.filePaths[0]) return null
    return readImagePayload(result.filePaths[0])
  })
  ipcMain.handle('files:save-binary', async (event, input: SaveBinaryFileInput): Promise<string | null> => {
    const normalized = normalizeBinaryFileInput(input)
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options = {
      defaultPath: sanitizeBinaryFileName(normalized.defaultName, normalized.kind),
      filters: [binaryFileFilters[normalized.kind]]
    }
    const result = owner ? await dialog.showSaveDialog(owner, options) : await dialog.showSaveDialog(options)
    if (result.canceled || !result.filePath) return null
    await writeFile(result.filePath, decodeDataUrl(normalized.dataUrl))
    return result.filePath
  })
  ipcMain.handle('clipboard:read-image', (): string | null => {
    const image = clipboard.readImage()
    return image.isEmpty() ? null : image.toDataURL()
  })
  ipcMain.handle('clipboard:write-image', (_event, dataUrl: string) => {
    const image = nativeImage.createFromDataURL(normalizeDataUrl(dataUrl))
    if (image.isEmpty()) throw new Error('Invalid image data')
    const clipboardImage = process.platform === 'darwin'
      ? nativeImage.createFromBuffer(image.toJPEG(100))
      : image
    clipboard.writeImage(clipboardImage)
  })
  ipcMain.handle('screen:capture', async (event): Promise<ScreenCapture[]> => {
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const wasVisible = owner?.isVisible() ?? false
    if (wasVisible) owner?.hide()
    try {
      await delay(180)
      const displays = screen.getAllDisplays()
      const maxWidth = Math.min(3840, Math.max(...displays.map((display) => Math.round(display.size.width * display.scaleFactor)), 1920))
      const maxHeight = Math.min(2160, Math.max(...displays.map((display) => Math.round(display.size.height * display.scaleFactor)), 1080))
      const sources = await desktopCapturer.getSources({ types: ['screen'], thumbnailSize: { width: maxWidth, height: maxHeight }, fetchWindowIcons: false })
      return sources.map((source) => {
        const size = source.thumbnail.getSize()
        return { id: source.id, name: source.name, width: size.width, height: size.height, dataUrl: source.thumbnail.toDataURL() }
      })
    } finally {
      if (wasVisible && owner && !owner.isDestroyed()) { owner.show(); owner.focus() }
    }
  })
  ipcMain.handle('images:list', () => createImageRepository().list())
  ipcMain.handle('images:read', (_event, name: string) => createImageRepository().read(normalizeImageName(name)))
  ipcMain.handle('images:import', async (event) => {
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = { properties: ['openFile', 'multiSelections'], filters: [imageFileFilter] }
    const result = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    return result.canceled ? [] : createImageRepository().import(result.filePaths)
  })
  ipcMain.handle('images:save', (_event, input: SaveImageAssetInput) => {
    if (!isRecord(input) || typeof input.name !== 'string' || typeof input.dataUrl !== 'string') throw new Error('Invalid image')
    return createImageRepository().save({ name: normalizeImageName(input.name), dataUrl: normalizeDataUrl(input.dataUrl) })
  })
  ipcMain.handle('images:rename', (_event, input: RenameImageAssetInput) => {
    if (!isRecord(input) || typeof input.name !== 'string' || typeof input.nextName !== 'string') throw new Error('Invalid image rename')
    return createImageRepository().rename({ name: normalizeImageName(input.name), nextName: normalizeImageName(input.nextName) })
  })
  ipcMain.handle('images:delete', (_event, names: string[]) => createImageRepository().delete(normalizeImageNames(names)))
  ipcMain.handle('images:export', async (event, names: string[]): Promise<string | null> => {
    const normalizedNames = normalizeImageNames(names)
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const result = owner
      ? await dialog.showOpenDialog(owner, { properties: ['openDirectory', 'createDirectory'] })
      : await dialog.showOpenDialog({ properties: ['openDirectory', 'createDirectory'] })
    if (result.canceled || !result.filePaths[0]) return null
    await createImageRepository().export(normalizedNames, result.filePaths[0])
    return result.filePaths[0]
  })
  ipcMain.handle('images:open', async (_event, name: string) => {
    const error = await shell.openPath(createImageRepository().pathFor(normalizeImageName(name)))
    if (error) throw new Error(error)
  })
  ipcMain.handle('pdf:choose-files', async (event) => {
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = { properties: ['openFile', 'multiSelections'], filters: [pdfFileFilter] }
    const result = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    if (result.canceled) return []
    const files = []
    for (const path of result.filePaths.slice(0, 20)) {
      const info = await inspectPdf(path)
      allowedPdfPaths.add(path)
      files.push(info)
    }
    return files
  })
  ipcMain.handle('pdf:merge', async (event, sources: PdfMergeSource[]) => {
    const normalizedSources = normalizePdfMergeSources(sources)
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options = { defaultPath: join(app.getPath('desktop'), 'merge.pdf'), filters: [pdfFileFilter] }
    const result = owner ? await dialog.showSaveDialog(owner, options) : await dialog.showSaveDialog(options)
    if (result.canceled || !result.filePath) return null
    return mergePdfs(normalizedSources, result.filePath)
  })
  ipcMain.handle('pdf:split', (_event, tasks: PdfSplitTask[]) => splitPdfs(normalizePdfSplitTasks(tasks)))
  ipcMain.handle('json-vault:list', (_event, input?: JsonVaultListInput) => createJsonVaultRepository().list(normalizeJsonVaultListInput(input)))
  ipcMain.handle('json-vault:read', (_event, relativePath: string) => createJsonVaultRepository().read(relativePath))
  ipcMain.handle('json-vault:save', async (_event, input: SaveJsonVaultFileInput) => {
    if (!isRecord(input) || typeof input.relativePath !== 'string' || typeof input.content !== 'string') {
      throw new Error('Invalid Vault file')
    }
    const result = await createJsonVaultRepository().save({ relativePath: input.relativePath, content: input.content })
    await maybeCommitJsonVault('Update JSON snippet')
    return result
  })
  ipcMain.handle('json-vault:create-folder', async (_event, relativePath: string) => {
    const result = await createJsonVaultRepository().createFolder(relativePath)
    await maybeCommitJsonVault('Create JSON Vault folder')
    return result
  })
  ipcMain.handle('json-vault:rename', async (_event, input: RenameJsonVaultEntryInput) => {
    if (!isRecord(input) || typeof input.relativePath !== 'string' || typeof input.name !== 'string') throw new Error('Invalid JSON Vault rename')
    const result = await createJsonVaultRepository().renameEntry(input)
    await maybeCommitJsonVault('Rename JSON Vault entry')
    return result
  })
  ipcMain.handle('json-vault:move', async (_event, input: MoveJsonVaultEntryInput) => {
    if (!isRecord(input) || typeof input.relativePath !== 'string' || typeof input.targetDirectory !== 'string') throw new Error('Invalid JSON Vault move')
    const result = await createJsonVaultRepository().moveEntry(input)
    await maybeCommitJsonVault('Move JSON Vault entry')
    return result
  })
  ipcMain.handle('json-vault:duplicate', async (_event, relativePath: string) => {
    const result = await createJsonVaultRepository().duplicate(relativePath)
    await maybeCommitJsonVault('Duplicate JSON snippet')
    return result
  })
  ipcMain.handle('json-vault:delete', async (_event, relativePath: string) => {
    await createJsonVaultRepository().delete(relativePath)
    await maybeCommitJsonVault('Delete JSON Vault entry')
  })
  ipcMain.handle('json-vault:open', async () => {
    await mkdir(getJsonVaultRoot(), { recursive: true })
    const error = await shell.openPath(getJsonVaultRoot())
    if (error) throw new Error(error)
  })
  ipcMain.handle('vault-git:status', () => createVaultGitService().status())
  ipcMain.handle('vault-git:history', () => createVaultGitService().history())
  ipcMain.handle('vault-git:diff', (_event, input: VaultGitDiffInput) => {
    if (!isRecord(input)) throw new Error('Invalid Git diff input')
    return createVaultGitService().diff({
      path: typeof input.path === 'string' ? input.path : undefined,
      commit: typeof input.commit === 'string' ? input.commit : undefined
    })
  })
  ipcMain.handle('vault-git:action', (_event, input: VaultGitActionInput) => {
    if (!isRecord(input) || !vaultGitActions.includes(input.action as VaultGitActionInput['action'])) {
      throw new Error('Invalid Git action')
    }
    return createVaultGitService().action({
      action: input.action as VaultGitActionInput['action'],
      message: typeof input.message === 'string' ? input.message : undefined,
      remote: typeof input.remote === 'string' ? input.remote : undefined,
      path: typeof input.path === 'string' ? input.path : undefined,
      strategy: input.strategy === 'ours' || input.strategy === 'theirs' ? input.strategy : undefined
    })
  })
  ipcMain.handle('quick-note:list', (_event, input?: QuickNoteListInput) => createQuickNoteRepository().list(normalizeQuickNoteListInput(input)))
  ipcMain.handle('quick-note:read', (_event, relativePath: string) => createQuickNoteRepository().read(relativePath))
  ipcMain.handle('quick-note:create', async (_event, input: CreateQuickNoteInput) => {
    if (!isRecord(input)) throw new Error('Invalid Quick Note input')
    const note = await createQuickNoteRepository().create({
      title: typeof input.title === 'string' ? input.title : '',
      parentPath: typeof input.parentPath === 'string' ? input.parentPath : undefined,
      fontSize: typeof input.fontSize === 'number' ? input.fontSize : undefined,
      lineWrap: typeof input.lineWrap === 'boolean' ? input.lineWrap : undefined
    })
    await maybeCommitQuickNote('Create Quick Note')
    return note
  })
  ipcMain.handle('quick-note:save', async (_event, input: SaveQuickNoteInput) => {
    if (!isRecord(input) || typeof input.relativePath !== 'string' || typeof input.content !== 'string' || !isRecord(input.metadata)) {
      throw new Error('Invalid Quick Note')
    }
    const note = await createQuickNoteRepository().save(input)
    await maybeCommitQuickNote('Update Quick Note')
    return note
  })
  ipcMain.handle('quick-note:create-folder', async (_event, relativePath: string) => {
    const result = await createQuickNoteRepository().createFolder(relativePath)
    await maybeCommitQuickNote('Create Quick Note folder')
    return result
  })
  ipcMain.handle('quick-note:rename', async (_event, input: RenameQuickNoteEntryInput) => {
    if (!isRecord(input) || typeof input.relativePath !== 'string' || typeof input.name !== 'string') throw new Error('Invalid Quick Note rename')
    const result = await createQuickNoteRepository().renameEntry(input)
    await maybeCommitQuickNote('Rename Quick Note entry')
    return result
  })
  ipcMain.handle('quick-note:move', async (_event, input: MoveQuickNoteEntryInput) => {
    if (!isRecord(input) || typeof input.relativePath !== 'string' || typeof input.targetDirectory !== 'string') throw new Error('Invalid Quick Note move')
    const result = await createQuickNoteRepository().moveEntry(input)
    await maybeCommitQuickNote('Move Quick Note entry')
    return result
  })
  ipcMain.handle('quick-note:duplicate', async (_event, relativePath: string) => {
    const result = await createQuickNoteRepository().duplicate(relativePath)
    await maybeCommitQuickNote('Duplicate Quick Note')
    return result
  })
  ipcMain.handle('quick-note:delete', async (_event, relativePath: string) => {
    await createQuickNoteRepository().delete(relativePath)
    await maybeCommitQuickNote('Delete Quick Note entry')
  })
  ipcMain.handle('quick-note:import-attachment', async (event) => {
    const owner = BrowserWindow.fromWebContents(event.sender) ?? mainWindow
    const options: OpenDialogOptions = { properties: ['openFile'], filters: [imageFileFilter] }
    const result = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    if (result.canceled || !result.filePaths[0]) return null
    const attachment = await createQuickNoteRepository().importAttachment(result.filePaths[0])
    await maybeCommitQuickNote('Add Quick Note attachment')
    return attachment
  })
  ipcMain.handle('quick-note:read-attachment', (_event, relativePath: string) => createQuickNoteRepository().readAttachment(relativePath))
  ipcMain.handle('quick-note:open-vault', async () => {
    await mkdir(getQuickNoteRoot(), { recursive: true })
    const error = await shell.openPath(getQuickNoteRoot())
    if (error) throw new Error(error)
  })
  ipcMain.handle('quick-note-git:status', () => createQuickNoteGitService().status())
  ipcMain.handle('quick-note-git:history', () => createQuickNoteGitService().history())
  ipcMain.handle('quick-note-git:diff', (_event, input: VaultGitDiffInput) => {
    if (!isRecord(input)) throw new Error('Invalid Git diff input')
    return createQuickNoteGitService().diff({
      path: typeof input.path === 'string' ? input.path : undefined,
      commit: typeof input.commit === 'string' ? input.commit : undefined
    })
  })
  ipcMain.handle('quick-note-git:action', (_event, input: VaultGitActionInput) => {
    if (!isRecord(input) || !vaultGitActions.includes(input.action as VaultGitActionInput['action'])) throw new Error('Invalid Git action')
    return createQuickNoteGitService().action({
      action: input.action as VaultGitActionInput['action'],
      message: typeof input.message === 'string' ? input.message : undefined,
      remote: typeof input.remote === 'string' ? input.remote : undefined,
      path: typeof input.path === 'string' ? input.path : undefined,
      strategy: input.strategy === 'ours' || input.strategy === 'theirs' ? input.strategy : undefined
    })
  })
  ipcMain.handle('backup:info', () => createBackupService().getInfo())
  ipcMain.handle('backup:export', async (event, kind: BackupKind) => {
    if (!backupKinds.includes(kind)) throw new Error('Invalid backup kind')
    const owner = BrowserWindow.fromWebContents(event.sender) ?? settingsWindow ?? mainWindow
    const options: OpenDialogOptions = {
      properties: ['openDirectory', 'createDirectory'],
      defaultPath: store.get('settings').tools.exportDirectory || app.getPath('documents')
    }
    const selection = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options)
    if (selection.canceled || !selection.filePaths[0]) return null
    const result = await createBackupService().export(selection.filePaths[0], kind)
    const error = await shell.openPath(result.directory)
    if (error) throw new Error(error)
    return result
  })
  ipcMain.handle('backup:open-location', async (_event, location: BackupLocation) => {
    const info = createBackupService().getInfo()
    const paths: Record<BackupLocation, string> = {
      data: info.dataDirectory,
      images: info.imagesPath,
      quickNote: info.quickNotePath,
      jsonVault: info.jsonVaultPath
    }
    if (!Object.hasOwn(paths, location)) throw new Error('Invalid backup location')
    await mkdir(paths[location], { recursive: true })
    const error = await shell.openPath(paths[location])
    if (error) throw new Error(error)
  })
  ipcMain.handle('legacy-migration:default-source', () => join(app.getPath('home'), '.MooTool'))
  ipcMain.handle('legacy-migration:preview', (_event, input: LegacyMigrationInput) => {
    return createLegacyMigrationService().preview(normalizeLegacyMigrationInput(input))
  })
  ipcMain.handle('legacy-migration:run', async (_event, input: LegacyMigrationInput) => {
    const normalized = normalizeLegacyMigrationInput(input)
    const service = createLegacyMigrationService()
    const preview = await service.preview(normalized)
    if (preview.alreadyMigrated) return service.migrate(normalized, '')
    const dataDirectory = getDataDirectory()
    const backupTarget = join(dataDirectory, 'migration-backups')
    closeDataRepositories()
    let backupDirectory = ''
    try {
      const backup = await createBackupService().export(backupTarget, 'all')
      backupDirectory = backup.directory
      const result = await service.migrate(normalized, backup.directory)
      const settings = mergeSettings(store.get('settings'), result.settingsPatch)
      store.set('settings', settings)
      applySettings(settings)
      broadcast('settings:changed', settings)
      return result
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      throw new Error(backupDirectory ? `${message} (backup: ${backupDirectory})` : message)
    } finally {
      openDataRepositories()
    }
  })
}

function createJsonVaultRepository(): JsonVaultRepository {
  return new JsonVaultRepository(getJsonVaultRoot())
}

function createImageRepository(): ImageRepository {
  const settings = store.get('settings')
  const dataDirectory = settings.data.directory || app.getPath('userData')
  return new ImageRepository(join(dataDirectory, 'images'))
}

function createQuickNoteRepository(): QuickNoteVaultRepository {
  return new QuickNoteVaultRepository(getQuickNoteRoot())
}

function createBackupService(): BackupService {
  const settings = store.get('settings')
  const dataDirectory = getDataDirectory(settings)
  return new BackupService({
    dataDirectory,
    databasePath: join(dataDirectory, 'MooToolNext.db'),
    settingsPath: store.path,
    imagesPath: join(dataDirectory, 'images'),
    quickNotePath: getQuickNoteRoot(),
    jsonVaultPath: getJsonVaultRoot()
  })
}

function createLegacyMigrationService(): LegacyMigrationService {
  return new LegacyMigrationService({
    databasePath: join(getDataDirectory(), 'MooToolNext.db'),
    quickNotePath: getQuickNoteRoot(),
    jsonVaultPath: getJsonVaultRoot()
  })
}

function normalizeLegacyMigrationInput(value: unknown): LegacyMigrationInput {
  if (!isRecord(value) || typeof value.sourceDirectory !== 'string' || !value.sourceDirectory.trim()) {
    throw new Error('Invalid legacy migration source')
  }
  return { sourceDirectory: value.sourceDirectory.trim().slice(0, 4096) }
}

function getDataDirectory(settings: AppSettings = store.get('settings')): string {
  return settings.data.directory || app.getPath('userData')
}

function openDataRepositories(settings: AppSettings = store.get('settings')): void {
  const databasePath = join(getDataDirectory(settings), 'MooToolNext.db')
  historyRepository = new HistoryRepository(databasePath)
  favoriteRepository = new FavoriteRepository(databasePath)
  p5Repository = new P5Repository(databasePath)
}

function closeDataRepositories(): void {
  historyRepository?.close()
  favoriteRepository?.close()
  p5Repository?.close()
}

function createVaultGitService(): VaultGitService {
  const settings = store.get('settings')
  return new VaultGitService(getJsonVaultRoot(), {
    username: settings.vault.gitUsername,
    token: readSecret('gitToken'),
    askPassPath: gitAskPassPath
  })
}

function createQuickNoteGitService(): VaultGitService {
  const settings = store.get('settings')
  return new VaultGitService(getQuickNoteRoot(), {
    username: settings.vault.gitUsername,
    token: readSecret('gitToken'),
    askPassPath: gitAskPassPath
  })
}

function getJsonVaultRoot(): string {
  const settings = store.get('settings')
  const dataDirectory = settings.data.directory || app.getPath('userData')
  return settings.vault.jsonPath || join(dataDirectory, 'json-vault')
}

function getQuickNoteRoot(): string {
  const settings = store.get('settings')
  const dataDirectory = settings.data.directory || app.getPath('userData')
  return settings.vault.quickNotePath || join(dataDirectory, 'quick-notes')
}

async function maybeCommitQuickNote(message: string): Promise<void> {
  if (!store.get('settings').vault.autoCommit) return
  const service = createQuickNoteGitService()
  let status = await service.status()
  if (!status.available) return
  if (!status.repository) {
    const initialized = await service.action({ action: 'init' })
    if (!initialized.success) return
    status = await service.status()
  }
  if (status.changes.length === 0) return
  await service.action({ action: 'commit', message })
}

async function maybeCommitJsonVault(message: string): Promise<void> {
  if (!store.get('settings').vault.autoCommit) return
  const service = createVaultGitService()
  let status = await service.status()
  if (!status.available) return
  if (!status.repository) {
    const initialized = await service.action({ action: 'init' })
    if (!initialized.success) return
    status = await service.status()
  }
  if (status.changes.length === 0) return
  await service.action({ action: 'commit', message })
}

function normalizeJsonVaultListInput(value: unknown): JsonVaultListInput {
  if (value == null) return {}
  if (!isRecord(value)) throw new Error('Invalid JSON Vault list input')
  return {
    hideIgnored: typeof value.hideIgnored === 'boolean' ? value.hideIgnored : undefined,
    sort: value.sort === 'name' || value.sort === 'modified' ? value.sort : undefined
  }
}

function normalizeQuickNoteListInput(value: unknown): QuickNoteListInput {
  if (value == null) return {}
  if (!isRecord(value)) throw new Error('Invalid Quick Note list input')
  const sort = value.sort === 'name' || value.sort === 'created' || value.sort === 'modified' ? value.sort : undefined
  return {
    keyword: typeof value.keyword === 'string' ? value.keyword.slice(0, 300) : undefined,
    includeContent: typeof value.includeContent === 'boolean' ? value.includeContent : undefined,
    hideIgnored: typeof value.hideIgnored === 'boolean' ? value.hideIgnored : undefined,
    sort
  }
}

function normalizeRuntimeExecutionInput(value: unknown): RuntimeExecutionInput {
  if (!isRecord(value) || typeof value.requestId !== 'string' || typeof value.code !== 'string' || !codeRuntimeIds.includes(value.runtime as RuntimeExecutionInput['runtime'])) {
    throw new Error('Invalid runtime request')
  }
  if (value.arguments !== undefined && (!Array.isArray(value.arguments) || value.arguments.some((item) => typeof item !== 'string'))) {
    throw new Error('Invalid runtime arguments')
  }
  return {
    requestId: normalizeRequestId(value.requestId),
    runtime: value.runtime as RuntimeExecutionInput['runtime'],
    code: value.code,
    timeoutMs: typeof value.timeoutMs === 'number' ? value.timeoutMs : undefined,
    arguments: value.arguments as string[] | undefined,
    workingDirectory: typeof value.workingDirectory === 'string' ? value.workingDirectory : undefined
  }
}

function configureQuickNoteWatcher(): void {
  quickNoteWatcherGeneration += 1
  const generation = quickNoteWatcherGeneration
  quickNoteWatcher?.close()
  quickNoteWatcher = null
  clearTimeout(quickNoteWatchTimer)
  const root = getQuickNoteRoot()
  void mkdir(root, { recursive: true }).then(() => {
    if (generation !== quickNoteWatcherGeneration || isQuitting) return
    try {
      quickNoteWatcher = watch(root, { recursive: true }, (_event, filename) => {
        clearTimeout(quickNoteWatchTimer)
        quickNoteWatchTimer = setTimeout(() => broadcast('quick-note:vault-changed', typeof filename === 'string' ? filename : ''), 180)
      })
      quickNoteWatcher.on('error', () => {
        quickNoteWatcher?.close()
        quickNoteWatcher = null
      })
    } catch {
      quickNoteWatcher = null
    }
  })
}

function configureJsonVaultWatcher(): void {
  jsonVaultWatcherGeneration += 1
  const generation = jsonVaultWatcherGeneration
  jsonVaultWatcher?.close()
  jsonVaultWatcher = null
  clearTimeout(jsonVaultWatchTimer)
  const root = getJsonVaultRoot()
  void mkdir(root, { recursive: true }).then(() => {
    if (generation !== jsonVaultWatcherGeneration || isQuitting) return
    try {
      jsonVaultWatcher = watch(root, { recursive: true }, (_event, filename) => {
        clearTimeout(jsonVaultWatchTimer)
        jsonVaultWatchTimer = setTimeout(() => broadcast('json-vault:changed', typeof filename === 'string' ? filename : ''), 180)
      })
      jsonVaultWatcher.on('error', () => {
        jsonVaultWatcher?.close()
        jsonVaultWatcher = null
      })
    } catch {
      jsonVaultWatcher = null
    }
  })
}

function configureQuickNoteAutoPull(settings: AppSettings): void {
  clearInterval(quickNotePullTimer)
  quickNotePullTimer = undefined
  if (settings.vault.autoPullMinutes <= 0) return
  quickNotePullTimer = setInterval(() => {
    void pullQuickNoteVault()
  }, settings.vault.autoPullMinutes * 60_000)
}

function configureJsonVaultAutoPull(settings: AppSettings): void {
  clearInterval(jsonVaultPullTimer)
  jsonVaultPullTimer = undefined
  if (settings.vault.autoPullMinutes <= 0) return
  jsonVaultPullTimer = setInterval(() => {
    void pullJsonVault()
  }, settings.vault.autoPullMinutes * 60_000)
}

async function pullQuickNoteVault(): Promise<void> {
  const service = createQuickNoteGitService()
  const status = await service.status()
  if (!status.repository || !status.remote || status.conflicts > 0 || status.changes.length > 0) return
  const result = await service.action({ action: 'pull' })
  if (result.success) broadcast('quick-note:vault-changed', '')
}

async function pullJsonVault(): Promise<void> {
  const service = createVaultGitService()
  const status = await service.status()
  if (!status.repository || !status.remote || status.conflicts > 0 || status.changes.length > 0) return
  const result = await service.action({ action: 'pull' })
  if (result.success) broadcast('json-vault:changed', '')
}

function readSecret(key: SecretKey): string {
  const encoded = store.get('secrets')[key]
  if (!encoded || !safeStorage.isEncryptionAvailable()) return ''
  try {
    return safeStorage.decryptString(Buffer.from(encoded, 'base64'))
  } catch {
    return ''
  }
}

function getProxyConfiguration(): ProxyConfiguration {
  const network = store.get('settings').network
  return {
    enabled: network.proxyEnabled,
    host: network.proxyHost,
    port: network.proxyPort,
    username: network.proxyUsername,
    password: readSecret('proxyPassword')
  }
}

async function prepareGitAskPass(): Promise<string> {
  const directory = join(app.getPath('userData'), 'runtime')
  await mkdir(directory, { recursive: true })
  const windows = process.platform === 'win32'
  const path = join(directory, windows ? 'mootool-git-askpass.cmd' : 'mootool-git-askpass.sh')
  const script = windows
    ? '@echo off\r\necho %1 | findstr /I "Username" >nul\r\nif %errorlevel%==0 (echo %MOOTOOL_GIT_USERNAME%) else (echo %MOOTOOL_GIT_TOKEN%)\r\n'
    : '#!/bin/sh\ncase "$1" in\n  *Username*) printf "%s" "$MOOTOOL_GIT_USERNAME" ;;\n  *) printf "%s" "$MOOTOOL_GIT_TOKEN" ;;\nesac\n'
  await writeFile(path, script, { encoding: 'utf8', mode: 0o700 })
  if (!windows) await chmod(path, 0o700)
  return path
}

async function detectRuntimes(settings: AppSettings): Promise<RuntimeStatus[]> {
  const definitions: Array<{ id: RuntimeId; command: string; args: string[] }> = [
    { id: 'java', command: settings.runtime.javaPath || 'java', args: ['-version'] },
    { id: 'groovy', command: settings.runtime.groovyPath || 'groovy', args: ['--version'] },
    { id: 'python', command: settings.runtime.pythonPath || (process.platform === 'win32' ? 'python' : 'python3'), args: ['--version'] },
    { id: 'node', command: settings.runtime.nodePath || 'node', args: ['--version'] }
  ]
  return Promise.all(definitions.map((definition) => detectRuntime(definition.id, definition.command, definition.args)))
}

function detectRuntime(id: RuntimeId, command: string, args: string[]): Promise<RuntimeStatus> {
  return new Promise((resolve) => {
    execFile(command, args, { timeout: 3000, windowsHide: true }, (error, stdout, stderr) => {
      const output = `${stdout}\n${stderr}`.trim().split(/\r?\n/)[0] ?? ''
      resolve({
        id,
        available: !error,
        command,
        version: !error ? output : ''
      })
    })
  })
}

function applySettings(settings: AppSettings): void {
  nativeTheme.themeSource = settings.appearance.theme
  rebuildApplicationMenu(settings.general.language)
  updateTray(settings)
  configureQuickNoteWatcher()
  configureQuickNoteAutoPull(settings)
  configureJsonVaultWatcher()
  configureJsonVaultAutoPull(settings)
  configureUpdateChecks(settings)
}

function rebuildApplicationMenu(language: AppLanguage): void {
  const labels = menuLabels[language]
  const settingsItem: MenuItemConstructorOptions = {
    label: labels.settings,
    accelerator: 'CommandOrControl+,',
    click: () => createSettingsWindow()
  }
  const searchItem: MenuItemConstructorOptions = {
    label: labels.search,
    accelerator: 'CommandOrControl+K',
    click: () => navigateMainWindow('focus-search')
  }
  const updateItem: MenuItemConstructorOptions = {
    label: labels.checkUpdates,
    click: () => { void checkForUpdatesAndBroadcast() }
  }

  const template: MenuItemConstructorOptions[] = [
    ...(process.platform === 'darwin' ? [{
      label: app.name,
      submenu: [
        { role: 'about' as const },
        updateItem,
        { type: 'separator' as const },
        settingsItem,
        { type: 'separator' as const },
        { role: 'hide' as const },
        { role: 'hideOthers' as const },
        { role: 'unhide' as const },
        { type: 'separator' as const },
        { role: 'quit' as const }
      ]
    }] : [{
      label: labels.file,
      submenu: [settingsItem, updateItem, { type: 'separator' as const }, { role: 'quit' as const }]
    }]),
    {
      label: labels.edit,
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectAll' },
        { type: 'separator' },
        searchItem
      ]
    },
    {
      label: labels.view,
      submenu: [
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
        ...(isDev ? [{ type: 'separator' as const }, { role: 'reload' as const }, { role: 'toggleDevTools' as const }] : [])
      ]
    },
    { role: 'windowMenu' }
  ]
  Menu.setApplicationMenu(Menu.buildFromTemplate(template))
}

function configureUpdateChecks(settings: AppSettings): void {
  clearTimeout(updateStartupTimer)
  clearInterval(updateIntervalTimer)
  updateStartupTimer = undefined
  updateIntervalTimer = undefined
  if (!settings.general.autoCheckUpdates || process.env.NODE_ENV === 'test') return
  updateStartupTimer = setTimeout(() => { void checkForUpdatesAndBroadcast(true) }, 2500)
  updateIntervalTimer = setInterval(() => { void checkForUpdatesAndBroadcast(true) }, 24 * 60 * 60 * 1000)
}

function checkForUpdates(): Promise<UpdateCheckResult> {
  if (!updateCheckPromise) {
    updateCheckPromise = updateService.check(app.getVersion()).finally(() => {
      updateCheckPromise = null
    })
  }
  return updateCheckPromise
}

async function checkForUpdatesAndBroadcast(automatic = false): Promise<void> {
  try {
    const result = await checkForUpdates()
    if (!automatic || result.status === 'available') {
      broadcast('update:checked', { type: 'result', result } satisfies UpdateCheckEvent)
    }
  } catch (error) {
    if (!automatic) {
      broadcast('update:checked', { type: 'error', message: error instanceof Error ? error.message : String(error) } satisfies UpdateCheckEvent)
    }
  }
}

function updateTray(settings: AppSettings): void {
  if (!settings.general.trayEnabled) {
    tray?.destroy()
    tray = null
    return
  }

  if (!tray) {
    const trayImage = nativeImage.createFromPath(getIconPath())
    if (trayImage.isEmpty()) return
    tray = new Tray(trayImage.resize({ width: process.platform === 'darwin' ? 18 : 20, height: process.platform === 'darwin' ? 18 : 20 }))
    tray.setToolTip('MooTool')
    tray.on('click', showMainWindow)
  }

  const labels = menuLabels[settings.general.language]
  tray.setContextMenu(Menu.buildFromTemplate([
    { label: labels.open, click: showMainWindow },
    { label: labels.settings, click: () => createSettingsWindow() },
    { type: 'separator' },
    { label: labels.quit, click: () => { isQuitting = true; app.quit() } }
  ]))
}

function showMainWindow(): void {
  const window = mainWindow ?? createMainWindow()
  window.show()
  window.focus()
}

function navigateMainWindow(event: AppNavigationEvent): void {
  showMainWindow()
  mainWindow?.webContents.send('app:navigate', event)
}

function broadcast(channel: string, payload: unknown): void {
  for (const window of BrowserWindow.getAllWindows()) {
    window.webContents.send(channel, payload)
  }
}

function normalizeWorkspaceState(value: WorkspaceState): WorkspaceState {
  if (!isRecord(value)) {
    return defaultWorkspaceState
  }
  return {
    activeToolId: typeof value.activeToolId === 'string' ? value.activeToolId : defaultWorkspaceState.activeToolId,
    recentToolIds: Array.isArray(value.recentToolIds)
      ? value.recentToolIds.filter((item): item is string => typeof item === 'string').slice(0, 5)
      : []
  }
}

function normalizeHistoryQuery(value: HistoryQuery): HistoryQuery {
  if (!isRecord(value)) throw new Error('Invalid history query')
  return {
    funcType: normalizeFuncType(value.funcType),
    keyword: typeof value.keyword === 'string' ? value.keyword.slice(0, 200) : undefined
  }
}

function normalizeHistoryInput(value: SaveFuncHistoryInput): SaveFuncHistoryInput {
  if (!isRecord(value) || typeof value.inputText !== 'string' || typeof value.outputText !== 'string') {
    throw new Error('Invalid history input')
  }
  return {
    funcType: normalizeFuncType(value.funcType),
    summary: typeof value.summary === 'string' ? value.summary.slice(0, 200) : undefined,
    inputText: value.inputText.slice(0, 20 * 1024 * 1024),
    outputText: value.outputText.slice(0, 20 * 1024 * 1024),
    extraData: typeof value.extraData === 'string' ? value.extraData.slice(0, 100_000) : null
  }
}

function normalizeFuncType(value: unknown): string {
  if (typeof value !== 'string' || !/^[a-zA-Z][a-zA-Z0-9_-]{0,63}$/.test(value)) {
    throw new Error('Invalid function type')
  }
  return value
}

function normalizeFavoriteKind(value: unknown): FavoriteKind {
  if (favoriteKinds.includes(value as FavoriteKind)) return value as FavoriteKind
  throw new Error('Unsupported favorite kind')
}

function normalizeFavoriteInput(value: SaveFavoriteInput): SaveFavoriteInput {
  if (!isRecord(value) || typeof value.name !== 'string' || typeof value.value !== 'string') {
    throw new Error('Invalid favorite')
  }
  const name = value.name.trim().slice(0, 120)
  const favoriteValue = value.value.trim().slice(0, 100_000)
  if (!name || !favoriteValue) throw new Error('Favorite name and value are required')
  return {
    kind: normalizeFavoriteKind(value.kind),
    name,
    value: favoriteValue,
    description: typeof value.description === 'string' ? value.description.trim().slice(0, 1000) : ''
  }
}

function normalizeTextFileKind(value: unknown): TextFileKind {
  if (value === 'json' || value === 'xml' || value === 'text' || value === 'source') return value
  throw new Error('Unsupported text file type')
}

function sanitizeDefaultFileName(value: unknown, kind: TextFileKind): string {
  const extension = kind === 'json' ? '.json' : kind === 'xml' ? '.xml' : '.txt'
  const raw = typeof value === 'string' ? basename(value) : `untitled${extension}`
  const sanitized = raw.replace(/[^a-zA-Z0-9._-]/g, '_').slice(0, 120) || `untitled${extension}`
  return extname(sanitized) ? sanitized : `${sanitized}${extension}`
}

function normalizeDigestAlgorithm(value: unknown): DigestAlgorithmId {
  if (!digestAlgorithmIds.includes(value as DigestAlgorithmId)) throw new Error('Unsupported digest algorithm')
  return value as DigestAlgorithmId
}

async function hashFile(path: string, algorithm: DigestAlgorithmId): Promise<string> {
  const nodeAlgorithm = algorithm.toLowerCase().replace('-', '')
  if (!getHashes().includes(nodeAlgorithm)) throw new Error(`${algorithm} is unavailable in this runtime`)
  return new Promise((resolve, reject) => {
    const hash = createHash(nodeAlgorithm)
    const stream = createReadStream(path)
    stream.on('error', reject)
    stream.on('data', (chunk) => hash.update(chunk))
    stream.on('end', () => resolve(hash.digest('hex')))
  })
}

async function readImagePayload(path: string): Promise<ImageFilePayload> {
  const fileStat = await stat(path)
  if (fileStat.size > 100 * 1024 * 1024) throw new Error('Image exceeds 100 MB limit')
  const image = nativeImage.createFromPath(path)
  if (image.isEmpty()) throw new Error('Unable to read image')
  const size = image.getSize()
  return { path, name: basename(path), size: fileStat.size, width: size.width, height: size.height, dataUrl: image.toDataURL() }
}

function normalizeBinaryFileInput(value: unknown): SaveBinaryFileInput {
  if (!isRecord(value) || (value.kind !== 'image' && value.kind !== 'pdf' && value.kind !== 'binary') || typeof value.defaultName !== 'string' || typeof value.dataUrl !== 'string') {
    throw new Error('Invalid binary file')
  }
  const dataUrl = normalizeDataUrl(value.dataUrl)
  if (dataUrl.length > 140 * 1024 * 1024) throw new Error('Binary data exceeds limit')
  return { kind: value.kind, defaultName: value.defaultName, dataUrl }
}

function normalizeDataUrl(value: unknown): string {
  if (typeof value !== 'string' || !/^data:[\w.+/-]+;base64,[A-Za-z0-9+/=\r\n]+$/.test(value)) throw new Error('Invalid data URL')
  return value
}

function decodeDataUrl(dataUrl: string): Buffer {
  return Buffer.from(dataUrl.slice(dataUrl.indexOf(',') + 1).replace(/\s+/g, ''), 'base64')
}

function sanitizeBinaryFileName(value: unknown, kind: SaveBinaryFileInput['kind']): string {
  const fallback = kind === 'image' ? 'image.png' : kind === 'pdf' ? 'document.pdf' : 'data.bin'
  const raw = typeof value === 'string' ? basename(value) : fallback
  const name = raw.replace(/[^a-zA-Z0-9._-]/g, '_').slice(0, 140) || fallback
  if (extname(name)) return name
  return kind === 'image' ? `${name}.png` : kind === 'pdf' ? `${name}.pdf` : `${name}.bin`
}

function normalizeImageName(value: unknown): string {
  if (typeof value !== 'string' || value.length > 200 || basename(value) !== value || value === '.' || value === '..') throw new Error('Invalid image name')
  return value
}

function normalizeImageNames(value: unknown): string[] {
  if (!Array.isArray(value) || value.length === 0 || value.length > 100) throw new Error('Invalid image selection')
  return value.map(normalizeImageName)
}

function normalizePdfMergeSources(value: unknown): PdfMergeSource[] {
  if (!Array.isArray(value) || value.length < 2 || value.length > 20) throw new Error('Select between 2 and 20 PDF files')
  return value.map((source) => {
    if (!isRecord(source) || typeof source.path !== 'string' || typeof source.pages !== 'string' || !allowedPdfPaths.has(source.path)) throw new Error('Invalid PDF source')
    return { path: source.path, pages: source.pages.slice(0, 1000) }
  })
}

function normalizePdfSplitTasks(value: unknown): PdfSplitTask[] {
  if (!Array.isArray(value) || value.length === 0 || value.length > 20) throw new Error('Select between 1 and 20 PDF tasks')
  return value.map((task) => {
    if (!isRecord(task) || typeof task.path !== 'string' || typeof task.pageRange !== 'string' || typeof task.customRule !== 'string' || !allowedPdfPaths.has(task.path) || !['odd', 'even', 'custom'].includes(String(task.rule))) throw new Error('Invalid PDF task')
    return { path: task.path, pageRange: task.pageRange.slice(0, 1000), rule: task.rule as PdfSplitTask['rule'], customRule: task.customRule.slice(0, 1000) }
  })
}

function delay(milliseconds: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, milliseconds))
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function normalizeSecretKey(value: unknown): SecretKey {
  if (value === 'proxyPassword' || value === 'gitToken') {
    return value
  }
  throw new Error('Unsupported secret key')
}

const menuLabels: Record<AppLanguage, { file: string; edit: string; view: string; search: string; settings: string; checkUpdates: string; open: string; quit: string }> = {
  'zh-CN': { file: '文件', edit: '编辑', view: '显示', search: '搜索工具', settings: '设置…', checkUpdates: '检查更新…', open: '打开 MooTool', quit: '退出' },
  'en-US': { file: 'File', edit: 'Edit', view: 'View', search: 'Search Tools', settings: 'Settings…', checkUpdates: 'Check for Updates…', open: 'Open MooTool', quit: 'Quit' },
  'ja-JP': { file: 'ファイル', edit: '編集', view: '表示', search: 'ツールを検索', settings: '設定…', checkUpdates: 'アップデートを確認…', open: 'MooTool を開く', quit: '終了' }
}

const closeDialogLabels: Record<AppLanguage, { message: string; hide: string; quit: string; cancel: string }> = {
  'zh-CN': { message: '关闭窗口后如何处理 MooTool？', hide: '隐藏到后台', quit: '退出 MooTool', cancel: '取消' },
  'en-US': { message: 'What should MooTool do when this window closes?', hide: 'Hide', quit: 'Quit MooTool', cancel: 'Cancel' },
  'ja-JP': { message: 'ウィンドウを閉じるときの動作を選択してください。', hide: 'バックグラウンドに隠す', quit: 'MooTool を終了', cancel: 'キャンセル' }
}

const textFileFilters: Record<TextFileKind, { name: string; extensions: string[] }> = {
  json: { name: 'JSON', extensions: ['json'] },
  xml: { name: 'XML', extensions: ['xml'] },
  text: { name: 'Text', extensions: ['txt', 'text', 'log'] },
  source: { name: 'Source files', extensions: ['java', 'xml', 'html', 'htm', 'conf', 'nginx', 'txt', 'text', 'log'] }
}

const imageFileFilter = { name: 'Images', extensions: ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'] }
const pdfFileFilter = { name: 'PDF', extensions: ['pdf'] }
const binaryFileFilters: Record<SaveBinaryFileInput['kind'], { name: string; extensions: string[] }> = {
  image: imageFileFilter,
  pdf: pdfFileFilter,
  binary: { name: 'Binary', extensions: ['bin', 'dat'] }
}

const vaultGitActions: VaultGitActionInput['action'][] = ['init', 'configure-remote', 'commit', 'fetch', 'pull', 'push', 'discard', 'abort-merge', 'resolve-conflict']

app.whenReady().then(async () => {
  store = new Store<PersistedStore>({
    name: 'mootool-next',
    defaults: {
      settings: defaultAppSettings,
      workspace: defaultWorkspaceState,
      window: defaultWindowState,
      secrets: {}
    }
  })
  store.set('settings', mergeSettings(defaultAppSettings, store.get('settings') as SettingsPatch))
  openDataRepositories()
  systemService = new SystemService(app.getPath('temp'))
  runtimeExecutionService = new RuntimeExecutionService(join(app.getPath('temp'), 'mootool-runtime'))
  gitAskPassPath = await prepareGitAskPass()

  if (process.platform === 'darwin' && !app.isPackaged) {
    app.dock?.setIcon(getDevelopmentIconPath())
  }

  registerIpc()
  applySettings(store.get('settings'))

  nativeTheme.on('updated', () => {
    const systemTheme = nativeTheme.shouldUseDarkColors ? 'dark' : 'light'
    for (const window of BrowserWindow.getAllWindows()) {
      window.setBackgroundColor(systemTheme === 'dark' ? '#171719' : '#f7f7f8')
      window.webContents.send('theme:system-changed', systemTheme)
    }
  })

  createMainWindow()

  app.on('activate', showMainWindow)
})

app.on('before-quit', () => {
  isQuitting = true
  quickNoteWatcherGeneration += 1
  jsonVaultWatcherGeneration += 1
  runtimeExecutionService?.cancelAll()
  quickNoteWatcher?.close()
  jsonVaultWatcher?.close()
  clearTimeout(quickNoteWatchTimer)
  clearTimeout(jsonVaultWatchTimer)
  clearInterval(quickNotePullTimer)
  clearInterval(jsonVaultPullTimer)
  clearTimeout(updateStartupTimer)
  clearInterval(updateIntervalTimer)
  closeDataRepositories()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin' && !store?.get('settings').general.trayEnabled) {
    app.quit()
  }
})
