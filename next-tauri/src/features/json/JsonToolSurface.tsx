import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { json } from '@codemirror/lang-json'
import { bracketMatching } from '@codemirror/language'
import { Compartment, EditorState } from '@codemirror/state'
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers
} from '@codemirror/view'
import {
  Braces,
  ArrowLeftRight,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Clipboard,
  CloudDownload,
  CloudUpload,
  CodeXml,
  Copy,
  Download,
  Eraser,
  FileJson,
  FilePlus2,
  FoldVertical,
  FolderPlus,
  FolderGit2,
  FolderOpen,
  GitBranch,
  GitCommitHorizontal,
  LocateFixed,
  LoaderCircle,
  MoreHorizontal,
  PanelRight,
  Pencil,
  RefreshCw,
  Save,
  Search,
  Sparkles,
  Trash2,
  UnfoldHorizontal,
  UnfoldVertical,
  Undo2,
  Unplug,
  Upload,
  WrapText,
  X,
  XCircle
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState, type ReactElement } from 'react'
import { createPortal } from 'react-dom'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { useI18n } from '../../app/i18n'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { vaultApi } from '../../platform/api/vaultApi'
import type {
  VaultDocument,
  VaultGitDetails,
  VaultGitDiffResult,
  VaultGitOperation,
  VaultSnapshot
} from '../../platform/contracts/vault'
import { electronCodeHighlighting } from '../../shared/codeHighlight'
import { codeEditorSearchHighlight } from '../../shared/codeEditorSearchHighlight'
import { errorMessage } from '../../shared/errors'
import {
  defaultFindReplaceOptions,
  findAllMatches,
  findNextMatch,
  findNextMatchWithWrap,
  replaceAllMatches,
  replaceCurrentMatch,
  type FindReplaceOptions
} from '../../shared/findReplace'
import { ancestorDirectoryPaths, ensureAncestorsExpanded, resolveExpandedPaths } from '../../shared/vaultTreeExpand'
import { selectTreePaths, type SelectionGesture } from '../../shared/vaultSelection'
import { selectVaultRows, type VaultListSort } from './jsonVaultList'
import { open } from '@tauri-apps/plugin-dialog'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { canMoveVaultEntry, readDraggedVaultEntry, renamedVaultPath, uniqueDuplicatePath, vaultDragDestination, vaultDragKindType, vaultDragPathType } from './jsonVaultActions'
import { GitDiffView } from '../quickNote/QuickNoteGitPanel'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { ResizableThreeColumns } from '../../shared/ResizableThreeColumns'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import { FontSelect } from '../../shared/components/FontSelect'
import { recordJsonVaultActivity, setJsonVaultEditorDirty } from '../settings/vaultCheckpoints'
import { describeJsonSession } from './jsonSession'
import { jsonMessages } from './jsonMessages'
import {
  analyzeJson,
  escapeJavaString,
  escapeJsonString,
  findDuplicateJsonKeys,
  formatJson,
  javaBeanToJson,
  jsonToJavaBean,
  jsonToXml,
  listJsonPaths,
  minifyJson,
  queryJsonPath,
  swapJsonKeysAndValues,
  unescapeJsonString,
  unescapeJsonText,
  validateJson,
  xmlToJson,
  JsonToolError
} from './jsonTools'

type JsonMessageKey = LocalizedMessageKey<typeof jsonMessages>
type JsonNotice = { key: JsonMessageKey; values?: MessageValues } | { raw: string }

function JsonWorkspace({ inspectorVisible, children }: { inspectorVisible: boolean; children: [ReactElement, ReactElement, ReactElement] }) {
  if (inspectorVisible) {
    return (
      <ResizableThreeColumns id="json-three-pane" legacyId="json-workbench" className="json-workbench__content" initialLeft={190} initialRight={280} defaultSizes={[190, 520, 280]} minLeft={150} minCenter={360} minRight={220}>
        {children[0]}
        {children[1]}
        {children[2]}
      </ResizableThreeColumns>
    )
  }
  return (
    <ResizableColumns id="json-two-pane" className="json-workbench__content" initialPrimary={190} defaultSizes={[190, 800]} minPrimary={150} minSecondary={360}>
      {children[0]}
      {children[1]}
    </ResizableColumns>
  )
}

interface EditorMetrics {
  content: string
  anchor: number
  head: number
  line: number
  scrollTop: number
}

type VaultSelection = { path: string; kind: 'file' | 'folder' }

export function JsonToolSurface() {
  const dialog = useDesktopDialog()
  const { settings, save } = useSettings()
  const { t, locale } = useLocalizedMessages(jsonMessages)
  const { t: settingsText } = useI18n()
  const recordOperation = useOperationHistory('json')
  const sampleJson = useRef(t('sample.json')).current
  const hostRef = useRef<HTMLDivElement>(null)
  const editorRef = useRef<EditorView | undefined>(undefined)
  const sessionId = useRef(crypto.randomUUID())
  const revision = useRef(0)
  const wrapCompartment = useRef(new Compartment())
  const ariaCompartment = useRef(new Compartment())
  const searchCompartment = useRef(new Compartment())
  const findIndexRef = useRef(0)
  const [findOpen, setFindOpen] = useState(false)
  const [findQuery, setFindQuery] = useState('')
  const [replaceText, setReplaceText] = useState('')
  const [findOptions, setFindOptions] = useState<FindReplaceOptions>(defaultFindReplaceOptions)
  const [replacedCount, setReplacedCount] = useState(0)
  const [metrics, setMetrics] = useState<EditorMetrics>({
    content: sampleJson,
    anchor: 0,
    head: 0,
    line: 1,
    scrollTop: 0
  })
  const [wrap, setWrap] = useState(settings.editor.softWrap)
  const [indent, setIndent] = useState<2 | 4 | 8>(2)
  const [sortKeys, setSortKeys] = useState(false)
  const [ignoreCase, setIgnoreCase] = useState(false)
  const [checkDuplicateKeys, setCheckDuplicateKeys] = useState(true)
  const [className, setClassName] = useState('Root')
  const [jsonPath, setJsonPath] = useState('$.release.channel')
  const [pathResult, setPathResult] = useState('"next-tauri"')
  const [notice, setNotice] = useState<JsonNotice>({ key: 'notice.ready' })
  const [copied, setCopied] = useState(false)
  const [compositionStarts, setCompositionStarts] = useState(0)
  const [compositionEnds, setCompositionEnds] = useState(0)
  const [reportError, setReportError] = useState('')
  const [vault, setVault] = useState<VaultSnapshot>()
  const [vaultDocument, setVaultDocument] = useState<VaultDocument>()
  const [inspectorOpen, setInspectorOpen] = useState(false)
  const [vaultSelection, setVaultSelection] = useState<VaultSelection>()
  const [vaultQuery, setVaultQuery] = useState('')
  const [vaultIncludeContent, setVaultIncludeContent] = useState(true)
  const [vaultSort, setVaultSort] = useState<VaultListSort>('name')
  const [contentMatches, setContentMatches] = useState<ReadonlySet<string>>(new Set())
  const [locateVaultRequest, setLocateVaultRequest] = useState(0)
  const [expandedFolders, setExpandedFolders] = useState<ReadonlySet<string> | null>(null)
  const knownFolders = useRef(new Set<string>())
  const vaultListRef = useRef<HTMLDivElement>(null)
  const handledVaultLocate = useRef(0)
  const appliedExpandMode = useRef<string | null>(null)
  const jsonExpandMode = settings.vault.jsonTreeExpandMode === 'collapseAll' ? 'collapseAll' : 'expandAll'
  const vaultSelectionPath = vaultSelection?.path ?? ''
  const [vaultBusy, setVaultBusy] = useState(false)
  const [gitRequestId, setGitRequestId] = useState('')
  const [gitDetailsOpen, setGitDetailsOpen] = useState(false)
  const [gitDetails, setGitDetails] = useState<VaultGitDetails>({ diff: '', commits: [] })
  const [gitDiff, setGitDiff] = useState<VaultGitDiffResult>({ files: [] })
  const [gitDiffPath, setGitDiffPath] = useState('')
  const [gitTab, setGitTab] = useState<'changes' | 'history'>('changes')
  const [gitSelected, setGitSelected] = useState('')
  const [gitRemoteDraft, setGitRemoteDraft] = useState(settings.vault.gitRemote)
  const [gitCommitMessage, setGitCommitMessage] = useState(t('git.defaultMessage'))
  const [vaultMenu, setVaultMenu] = useState<{ entry: VaultSelection; paths: string[]; left: number; top: number } | null>(null)
  const [selectedVaultPaths, setSelectedVaultPaths] = useState<string[]>([])
  const vaultSelectionAnchor = useRef('')
  const [vaultDropTarget, setVaultDropTarget] = useState<string | null>(null)
  const draggedVaultEntry = useRef<VaultSelection | null>(null)
  const [gitDetailsBusy, setGitDetailsBusy] = useState(false)
  const metricsRef = useRef(metrics)
  const vaultDocumentRef = useRef(vaultDocument)
  metricsRef.current = metrics
  vaultDocumentRef.current = vaultDocument
  const vaultDirty = Boolean(vaultDocument && metrics.content !== vaultDocument.content)
  const noticeText = localizeNotice(notice, t)
  const vaultFileKey = vault?.files.map((file) => `${file.relativePath}\0${file.modifiedAt}\0${file.sizeBytes}`).join('\n') ?? ''
  const vaultRows = vault ? selectVaultRows(vault.files, vault.directories, vaultQuery, vaultIncludeContent, contentMatches, vaultSort) : []
  const visibleVaultRows = vaultRows.filter((row) => ancestorDirectoryPaths(row.kind === 'folder' ? row.path : row.file.relativePath).every((parent) => !expandedFolders || expandedFolders.has(parent)))
  const visibleVaultPaths = visibleVaultRows.map((row) => row.kind === 'folder' ? row.path : row.file.relativePath)
  const visibleVaultKey = visibleVaultPaths.join('\n')
  const multipleVaultSelection = selectedVaultPaths.length > 1
  const vaultLocatePath = vaultDocument?.relativePath || (vaultSelection?.kind === 'file' ? vaultSelection.path : '')

  useEffect(() => { setJsonVaultEditorDirty(vaultDirty) }, [vaultDirty])
  useEffect(() => {
    setSelectedVaultPaths((current) => {
      const next = current.filter((path) => visibleVaultPaths.includes(path))
      return next.length === current.length ? current : next
    })
  }, [visibleVaultKey])
  useEffect(() => {
    if (!vault) return
    const folders = vaultFolderPaths(vault)
    if (appliedExpandMode.current !== jsonExpandMode) {
      appliedExpandMode.current = jsonExpandMode
      knownFolders.current = new Set(folders)
      setExpandedFolders(resolveExpandedPaths(folders, jsonExpandMode, vaultSelectionPath))
      return
    }
    const fresh = folders.filter((path) => !knownFolders.current.has(path))
    knownFolders.current = new Set(folders)
    setExpandedFolders((current) => {
      const next = new Set(current ?? [])
      if (jsonExpandMode === 'expandAll') fresh.forEach((path) => next.add(path))
      for (const path of ancestorDirectoryPaths(vaultSelectionPath)) next.add(path)
      return next
    })
  }, [jsonExpandMode, vault, vaultSelectionPath])
  useEffect(() => {
    const keyword = vaultQuery.trim().toLocaleLowerCase()
    if (!vault || !keyword || !vaultIncludeContent) {
      setContentMatches(new Set())
      return
    }
    let cancelled = false
    const candidates = vault.files.filter((file) => file.sizeBytes <= 20 * 1024 * 1024 && !file.relativePath.toLocaleLowerCase().includes(keyword))
    void (async () => {
      const matched = new Set<string>()
      for (const file of candidates) {
        if (cancelled) return
        try {
          const document = await vaultApi.read(file.relativePath)
          if (document.content.toLocaleLowerCase().includes(keyword)) matched.add(file.relativePath)
        } catch {
          // A file that cannot be read does not match the content search.
        }
      }
      if (!cancelled) setContentMatches(matched)
    })()
    return () => { cancelled = true }
  }, [vault, vaultFileKey, vaultIncludeContent, vaultQuery])
  useEffect(() => {
    if (!locateVaultRequest || handledVaultLocate.current === locateVaultRequest || !vaultLocatePath) return
    const row = vaultListRef.current?.querySelector<HTMLElement>(`[data-path="${CSS.escape(vaultLocatePath)}"]`)
    if (!row) return
    handledVaultLocate.current = locateVaultRequest
    row.focus({ preventScroll: true })
    row.scrollIntoView({ block: 'nearest' })
  }, [expandedFolders, locateVaultRequest, vaultFileKey, vaultLocatePath, vaultQuery])

  useOperationRestore('json', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (metadata.indent === 2 || metadata.indent === 4 || metadata.indent === 8) setIndent(metadata.indent)
    if (typeof metadata.sortKeys === 'boolean') setSortKeys(metadata.sortKeys)
    if (typeof metadata.ignoreCase === 'boolean') setIgnoreCase(metadata.ignoreCase)
    if (typeof metadata.checkDuplicateKeys === 'boolean') setCheckDuplicateKeys(metadata.checkDuplicateKeys)
    if (typeof metadata.className === 'string') setClassName(metadata.className)
    if (typeof metadata.wrap === 'boolean') setWrap(metadata.wrap)
    if (typeof metadata.jsonPath === 'string') setJsonPath(metadata.jsonPath)
    replaceDocument(entry.outputText || entry.inputText, { key: 'notice.ready' })
  })

  useEffect(() => {
    setWrap(settings.editor.softWrap)
  }, [settings.editor.softWrap])

  const inspectorVisible = inspectorOpen && Boolean(vaultDocument)

  useEffect(() => {
    const desktopLayout = window.matchMedia('(min-width: 1321px)')
    const syncInspector = () => {
      if (!desktopLayout.matches) setInspectorOpen(false)
    }
    desktopLayout.addEventListener('change', syncInspector)
    return () => desktopLayout.removeEventListener('change', syncInspector)
  }, [])

  useEffect(() => {
    const host = hostRef.current
    if (!host) return
    const length = metrics.content.length

    const view = new EditorView({
      parent: host,
      state: EditorState.create({
        doc: metrics.content,
        selection: { anchor: Math.min(metrics.anchor, length), head: Math.min(metrics.head, length) },
        extensions: [
          history(),
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          bracketMatching(),
          json(),
          electronCodeHighlighting(),
          keymap.of([
            ...defaultKeymap,
            ...historyKeymap,
            indentWithTab
          ]),
          wrapCompartment.current.of(wrap ? EditorView.lineWrapping : []),
          searchCompartment.current.of(findOpen ? codeEditorSearchHighlight(findQuery, findOptions) : []),
          ariaCompartment.current.of(EditorView.contentAttributes.of({
            'aria-label': t('aria.editor'),
            spellcheck: 'false',
            autocapitalize: 'off',
            autocomplete: 'off'
          })),
          EditorView.updateListener.of((update) => {
            if (!update.docChanged && !update.selectionSet) return
            if (update.docChanged) setNotice({ key: 'notice.ready' })
            const selection = update.state.selection.main
            setMetrics((current) => ({
              ...current,
              content: update.state.doc.toString(),
              anchor: selection.anchor,
              head: selection.head,
              line: update.state.doc.lineAt(selection.head).number
            }))
          })
        ]
      })
    })

    const handleScroll = () => {
      setMetrics((current) => ({ ...current, scrollTop: view.scrollDOM.scrollTop }))
    }
    const handleCompositionStart = () => setCompositionStarts((value) => value + 1)
    const handleCompositionEnd = () => setCompositionEnds((value) => value + 1)

    view.scrollDOM.scrollTop = metrics.scrollTop
    view.scrollDOM.addEventListener('scroll', handleScroll, { passive: true })
    view.contentDOM.addEventListener('compositionstart', handleCompositionStart)
    view.contentDOM.addEventListener('compositionend', handleCompositionEnd)
    editorRef.current = view
    if (!inspectorVisible) view.focus()

    return () => {
      view.scrollDOM.removeEventListener('scroll', handleScroll)
      view.contentDOM.removeEventListener('compositionstart', handleCompositionStart)
      view.contentDOM.removeEventListener('compositionend', handleCompositionEnd)
      editorRef.current = undefined
      view.destroy()
    }
  }, [inspectorVisible])

  useEffect(() => {
    let cancelled = false
    let dispose: (() => void) | undefined
    void refreshVault(false).catch((cause: unknown) => {
      if (!cancelled) setNotice({ raw: errorMessage(cause, 'json.vault.snapshot') })
    })
    void vaultApi.subscribe(() => {
      void refreshVault(true).catch((cause: unknown) => {
        if (!cancelled) setNotice({ raw: errorMessage(cause, 'json.vault.watch') })
      })
    }).then((unlisten) => {
      if (cancelled) unlisten()
      else dispose = unlisten
    }).catch((cause: unknown) => {
      if (!cancelled) setNotice({ raw: errorMessage(cause, 'json.vault.subscribe') })
    })
    return () => {
      cancelled = true
      dispose?.()
    }
  }, [settings.vault.hideGitignoredFiles])

  useEffect(() => {
    if (!vaultMenu) return
    const close = () => setVaultMenu(null)
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    const focusFrame = window.requestAnimationFrame(() => document.querySelector<HTMLButtonElement>('.json-vault-menu button:not(:disabled)')?.focus())
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', closeOnEscape)
    window.addEventListener('blur', close)
    return () => {
      window.cancelAnimationFrame(focusFrame)
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', closeOnEscape)
      window.removeEventListener('blur', close)
    }
  }, [vaultMenu])

  useEffect(() => {
    const view = editorRef.current
    if (!view) return
    view.dispatch({
      effects: wrapCompartment.current.reconfigure(wrap ? EditorView.lineWrapping : [])
    })
  }, [wrap])

  useEffect(() => {
    const view = editorRef.current
    if (!view) return
    view.dispatch({
      effects: searchCompartment.current.reconfigure(findOpen ? codeEditorSearchHighlight(findQuery, findOptions) : [])
    })
  }, [findOpen, findOptions, findQuery])

  useEffect(() => {
    const handleFindShortcut = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey) || event.shiftKey || event.altKey) return
      const key = event.key.toLowerCase()
      if (key !== 'f' && key !== 'r') return
      event.preventDefault()
      openJsonFind()
    }
    window.addEventListener('keydown', handleFindShortcut)
    return () => window.removeEventListener('keydown', handleFindShortcut)
  })

  useEffect(() => {
    const view = editorRef.current
    if (!view) return
    view.dispatch({
      effects: ariaCompartment.current.reconfigure(EditorView.contentAttributes.of({
        'aria-label': t('aria.editor'),
        spellcheck: 'false',
        autocapitalize: 'off',
        autocomplete: 'off'
      }))
    })
  }, [t])

  const validation = useMemo(() => validateJson(metrics.content), [metrics.content])
  const findMatches = useMemo(() => findAllMatches(metrics.content, findQuery, findOptions), [findOptions, findQuery, metrics.content])
  const analysis = useMemo(() => analyzeJson(metrics.content), [metrics.content])
  const duplicates = useMemo(() => {
    try { return findDuplicateJsonKeys(metrics.content, ignoreCase) } catch { return [] }
  }, [ignoreCase, metrics.content])
  const pathEntries = useMemo(() => {
    try { return listJsonPaths(metrics.content) } catch { return [] }
  }, [metrics.content])
  const session = useMemo(() => describeJsonSession({
    ...metrics,
    wrap,
    indent,
    sortKeys,
    jsonPath,
    compositionStarts,
    compositionEnds
  }), [
    compositionEnds,
    compositionStarts,
    indent,
    jsonPath,
    metrics,
    sortKeys,
    wrap
  ])
  const sessionSummary = t('session.summary', {
    length: session.contentLength,
    line: session.line,
    from: session.selectionFrom,
    to: session.selectionTo
  })
  const inspectorShowsValidation = !('raw' in notice) && notice.key === 'notice.ready'
  const validationText = validation.kind === 'idle'
    ? t('validation.idle')
    : validation.kind === 'valid'
      ? t('validation.valid', { type: validation.rootType })
      : jsonErrorText(validation.cause, t)

  useEffect(() => {
    revision.current += 1
    void toolWebviewApis.json.report({
      sessionId: sessionId.current,
      stateRevision: revision.current,
      stateDigest: session.digest,
      stateSummary: sessionSummary
    }).then(() => setReportError('')).catch((cause: unknown) => {
      setReportError(errorMessage(cause, 'json.session.report'))
    })
  }, [session.digest, sessionSummary])

  function replaceDocument(content: string, message: JsonNotice): void {
    const view = editorRef.current
    if (!view) return
    view.dispatch({
      changes: { from: 0, to: view.state.doc.length, insert: content },
      selection: { anchor: 0 },
      scrollIntoView: true
    })
    setNotice(message)
    view.focus()
  }

  function openJsonFind(): void {
    const view = editorRef.current
    const selection = view?.state.selection.main
    const selected = selection && selection.to > selection.from ? metrics.content.slice(selection.from, selection.to) : undefined
    findIndexRef.current = 0
    setReplacedCount(0)
    if (selected !== undefined) setFindQuery(selected)
    setFindOpen(true)
  }

  function selectJsonRange(start: number, end: number): void {
    const view = editorRef.current
    if (!view) return
    view.dispatch({ selection: { anchor: start, head: end }, scrollIntoView: true })
    view.focus()
  }

  function findJsonAround(forward: boolean): void {
    if (!findQuery) return
    const selection = editorRef.current?.state.selection.main
    const fromIndex = forward ? selection?.to ?? findIndexRef.current : selection?.from ?? findIndexRef.current
    const result = findNextMatchWithWrap(metrics.content, findQuery, findOptions, fromIndex, forward)
    if (!result) {
      setNotice({ key: 'find.noMatches' })
      return
    }
    if (result.wrapped) setNotice({ key: forward ? 'find.wrappedStart' : 'find.wrappedEnd' })
    findIndexRef.current = forward ? result.match.end : result.match.start
    selectJsonRange(result.match.start, result.match.end)
  }

  function replaceJson(all: boolean): void {
    const view = editorRef.current
    if (!view || !findQuery) return
    if (all) {
      const result = replaceAllMatches(metrics.content, findQuery, replaceText, findOptions)
      if (result.count === 0) setNotice({ key: 'find.noMatches' })
      else {
        view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: result.content } })
        setReplacedCount(result.count)
      }
      return
    }
    const selection = view.state.selection.main
    const result = replaceCurrentMatch(metrics.content, findQuery, replaceText, findOptions, { start: selection.from, end: selection.to })
    if (!result.replaced) {
      setNotice({ key: 'find.noMatches' })
      return
    }
    view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: result.content } })
    setReplacedCount((count) => count + 1)
    findIndexRef.current = result.nextFrom
    const query = findQuery
    const options = findOptions
    window.requestAnimationFrame(() => {
      const match = findNextMatch(result.content, query, options, result.nextFrom, true)
      if (match) {
        findIndexRef.current = match.end
        selectJsonRange(match.start, match.end)
      }
    })
  }

  function runTransform(transform: () => string, success: JsonMessageKey): void {
    try {
      const input = metrics.content
      const output = transform()
      replaceDocument(output, { key: success })
      recordOperation(t(success), t('operation.content', { length: input.length }), 'success', {
        inputText: input, outputText: output, metadata: { indent, sortKeys, ignoreCase, checkDuplicateKeys, wrap, jsonPath, className }
      })
    } catch (cause) {
      const message = jsonErrorText(cause, t)
      setNotice({ raw: message })
      recordOperation(t(success), message, 'error', {
        inputText: metrics.content, metadata: { indent, sortKeys, ignoreCase, checkDuplicateKeys, wrap, jsonPath, className }
      })
    }
  }

  async function importContent(): Promise<void> {
    try {
      const file = await userFilesApi.pickText()
      if (!file) return
      const content = file.name.toLocaleLowerCase().endsWith('.xml')
        ? xmlToJson(file.content, indent)
        : file.content
      replaceDocument(content, { key: 'notice.imported', values: { name: file.name } })
      recordOperation(t('operation.import'), `${file.name} · ${content.length}`, 'success', {
        outputText: content, metadata: { fileName: file.name, indent, sortKeys, wrap, jsonPath }
      })
    } catch (cause) {
      const message = jsonErrorText(cause, t)
      setNotice({ raw: message })
      recordOperation(t('operation.import'), message, 'error')
    }
  }

  async function exportContent(): Promise<void> {
    try {
      const path = await userFilesApi.exportText(vaultDocument?.relativePath.split('/').at(-1) ?? 'mootool.json', metrics.content)
      if (!path) return
      setNotice({ key: 'notice.exported', values: { path } })
      recordOperation(t('operation.export'), `${path} · ${metrics.content.length}`, 'success')
    } catch (cause) {
      const message = errorMessage(cause, 'json.export')
      setNotice({ raw: message })
      recordOperation(t('operation.export'), message, 'error')
    }
  }

  function runJsonPath(): void {
    try {
      const output = queryJsonPath(metrics.content, jsonPath) ?? t('path.noMatch')
      setPathResult(output)
      setNotice({ key: 'notice.pathDone' })
      recordOperation(t('action.query'), `${jsonPath} · ${output.length}`, 'success', {
        inputText: metrics.content, outputText: output, metadata: { operation: 'jsonPath', jsonPath, indent, sortKeys, wrap }
      })
    } catch (cause) {
      const message = jsonErrorText(cause, t)
      setPathResult(message)
      setNotice({ raw: message })
      recordOperation(t('action.query'), message, 'error', { inputText: metrics.content, metadata: { operation: 'jsonPath', jsonPath } })
    }
  }

  async function copyContent(): Promise<void> {
    try {
      await clipboardApi.writeText(metrics.content)
      setCopied(true)
      setNotice({ key: 'notice.copied' })
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'notice.copyFailed' })
    }
  }

  async function refreshVault(external: boolean): Promise<VaultSnapshot> {
    const snapshot = await vaultApi.snapshot()
    setVault(snapshot)
    setVaultSelection((selected) => {
      if (!selected) return selected
      const exists = selected.kind === 'file'
        ? snapshot.files.some((file) => file.relativePath === selected.path)
        : snapshot.directories.includes(selected.path)
      return exists ? selected : undefined
    })
    const opened = vaultDocumentRef.current
    if (!opened || !snapshot.rootPath) return snapshot
    if (!snapshot.files.some((file) => file.relativePath === opened.relativePath)) {
      if (external) setNotice({ key: 'notice.externalRemoved', values: { path: opened.relativePath } })
      return snapshot
    }
    if (metricsRef.current.content !== opened.content) {
      if (external) setNotice({ key: 'notice.externalDirty' })
      return snapshot
    }
    const latest = await vaultApi.read(opened.relativePath)
    if (latest.fingerprint !== opened.fingerprint) {
      setVaultDocument(latest)
      replaceDocument(latest.content, { key: 'notice.externalLoaded', values: { path: latest.relativePath } })
    }
    return snapshot
  }

  async function chooseVault(): Promise<void> {
    const root = await vaultApi.chooseRootDirectory()
    if (!root) return
    setVaultBusy(true)
    try {
      const snapshot = await vaultApi.configure(root)
      setVault(snapshot)
      setVaultDocument(undefined)
      setVaultSelection(undefined)
      setNotice({ key: 'notice.vaultConnected', values: { root } })
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.configure') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function openVaultDocument(relativePath: string): Promise<void> {
    if (vaultDirty && !await dialog.confirm(t('confirm.openDirty'), { dangerous: true })) return
    setVaultBusy(true)
    try {
      const document = await vaultApi.read(relativePath)
      setVaultDocument(document)
      setVaultSelection({ path: relativePath, kind: 'file' })
      setSelectedVaultPaths([relativePath])
      vaultSelectionAnchor.current = relativePath
      replaceDocument(document.content, { key: 'notice.opened', values: { path: relativePath } })
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.read') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function createVaultDocument(): Promise<void> {
    if (!vault?.rootPath) {
      await chooseVault()
      return
    }
    const relativePath = (await dialog.prompt(t('prompt.newFile'), 'untitled.json'))?.trim()
    if (!relativePath) return
    setVaultBusy(true)
    try {
      const document = await vaultApi.save({
        relativePath,
        content: metrics.content,
        expectedFingerprint: null
      })
      setVaultDocument(document)
      setVaultSelection({ path: document.relativePath, kind: 'file' })
      await refreshVault(false)
      setNotice({ key: 'notice.created', values: { path: document.relativePath } })
      recordJsonVaultActivity('Create JSON snippet')
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.create') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function saveVaultDocument(): Promise<boolean> {
    if (!vaultDocument) {
      await createVaultDocument()
      return false
    }
    setVaultBusy(true)
    try {
      const saved = await vaultApi.save({
        relativePath: vaultDocument.relativePath,
        content: metrics.content,
        expectedFingerprint: vaultDocument.fingerprint
      })
      setVaultDocument(saved)
      await refreshVault(false)
      setNotice({ key: 'notice.saved', values: { path: saved.relativePath } })
      recordJsonVaultActivity('Update JSON snippet')
      return true
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.save') })
      return false
    } finally {
      setVaultBusy(false)
    }
  }

  async function pullVault(): Promise<void> {
    if (vaultDirty && vaultDocument) {
      const saved = await saveVaultDocument()
      if (!saved) return
    }
    await runGit('pull', undefined, false)
  }

  async function createVaultDirectory(): Promise<void> {
    if (!vault?.rootPath) {
      await chooseVault()
      return
    }
    const relativePath = (await dialog.prompt(t('prompt.newFolder'), 'examples'))?.trim()
    if (!relativePath) return
    setVaultBusy(true)
    try {
      const path = await vaultApi.createDirectory(relativePath)
      setVaultSelection({ path, kind: 'folder' })
      await refreshVault(false)
      setNotice({ key: 'notice.folderCreated', values: { path } })
      recordJsonVaultActivity('Create JSON Vault folder')
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.create-directory') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function moveVaultEntry(prepared?: { destinationPath: string; activity: string; notice: 'notice.moved' | 'vault.renamed' }, entry: VaultSelection | undefined = vaultSelection): Promise<void> {
    if (!entry) return
    const destinationPath = prepared?.destinationPath ?? (await dialog.prompt(
      t('prompt.moveEntry', { path: entry.path }),
      entry.path
    ))?.trim()
    if (!destinationPath || destinationPath === entry.path) return
    setVaultBusy(true)
    try {
      let expectedFingerprint: string | null = null
      if (entry.kind === 'file') {
        expectedFingerprint = vaultDocument?.relativePath === entry.path
          ? vaultDocument.fingerprint
          : (await vaultApi.read(entry.path)).fingerprint
      }
      const previousPath = entry.path
      const movedPath = await vaultApi.move(previousPath, destinationPath, expectedFingerprint)
      setVaultSelection({ path: movedPath, kind: entry.kind })
      setVaultDocument((document) => {
        if (!document) return document
        if (entry.kind === 'file' && document.relativePath === previousPath) {
          return { ...document, relativePath: movedPath }
        }
        if (entry.kind === 'folder' && document.relativePath.startsWith(`${previousPath}/`)) {
          return { ...document, relativePath: `${movedPath}${document.relativePath.slice(previousPath.length)}` }
        }
        return document
      })
      await refreshVault(false)
      setNotice({ key: prepared?.notice ?? 'notice.moved', values: { path: movedPath } })
      recordJsonVaultActivity(prepared?.activity ?? 'Move JSON Vault entry')
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.move') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function renameVaultEntry(entry: VaultSelection | undefined = vaultSelection): Promise<void> {
    if (!entry) return
    const leaf = entry.path.split('/').at(-1) ?? entry.path
    const current = entry.kind === 'file' ? leaf.replace(/\.json$/i, '') : leaf
    const name = await dialog.prompt(t('vault.renamePrompt'), current)
    if (name === null) return
    try {
      const destinationPath = renamedVaultPath(entry.path, name, entry.kind === 'file')
      await moveVaultEntry({ destinationPath, activity: 'Rename JSON Vault entry', notice: 'vault.renamed' }, entry)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.rename') })
    }
  }

  async function exportSelectedVaultFile(paths: string[] = vaultFilePaths()): Promise<void> {
    if (!paths.length) return
    if (vaultDirty && vaultDocument && paths.includes(vaultDocument.relativePath)) {
      const saved = await saveVaultDocument()
      if (!saved) return
    }
    const selected = await open({ directory: true, multiple: false, title: t('vault.export') })
    const directory = Array.isArray(selected) ? selected[0] : selected
    if (!directory) return
    setVaultBusy(true)
    try {
      const count = await vaultApi.exportFiles(directory, paths)
      setNotice({ key: 'vault.exported', values: { count } })
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.export') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function revealVaultEntry(entry: VaultSelection): Promise<void> {
    try {
      await vaultApi.revealEntry(entry.path)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.reveal') })
    }
  }

  function openVaultMenu(entry: VaultSelection, clientX: number, clientY: number): void {
    const paths = selectedVaultPaths.includes(entry.path) ? selectedVaultPaths : [entry.path]
    if (!selectedVaultPaths.includes(entry.path)) {
      setSelectedVaultPaths(paths)
      vaultSelectionAnchor.current = entry.path
      setVaultSelection(entry)
    }
    setVaultMenu({
      entry,
      paths,
      left: Math.min(clientX, window.innerWidth - 180),
      top: Math.min(clientY, window.innerHeight - 220)
    })
  }

  function chooseVaultRow(entry: VaultSelection, gesture: SelectionGesture): boolean {
    setSelectedVaultPaths((current) => selectTreePaths(current, entry.path, vaultSelectionAnchor.current, visibleVaultPaths, gesture))
    if (!gesture.shiftKey) vaultSelectionAnchor.current = entry.path
    const plain = !gesture.ctrlKey && !gesture.metaKey && !gesture.shiftKey
    if (plain) setVaultSelection(entry)
    return plain
  }

  function vaultFilePaths(paths = selectedVaultPaths): string[] {
    const files = new Set(vault?.files.map((file) => file.relativePath) ?? [])
    const source = paths.length ? paths : vaultSelection?.kind === 'file' ? [vaultSelection.path] : []
    return source.filter((path) => files.has(path))
  }

  function startVaultDrag(event: React.DragEvent, entry: VaultSelection): void {
    draggedVaultEntry.current = entry
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData(vaultDragPathType, entry.path)
    event.dataTransfer.setData(vaultDragKindType, entry.kind)
  }

  function allowVaultDrop(event: React.DragEvent, targetDirectory: string): void {
    const entry = draggedVaultEntry.current
    if (!entry || !canMoveVaultEntry(entry.path, targetDirectory)) return
    event.preventDefault()
    event.stopPropagation()
    event.dataTransfer.dropEffect = 'move'
    if (vaultDropTarget !== targetDirectory) setVaultDropTarget(targetDirectory)
  }

  function dropVaultEntry(event: React.DragEvent, targetDirectory: string): void {
    event.preventDefault()
    event.stopPropagation()
    const entry = readDraggedVaultEntry(event.dataTransfer) ?? draggedVaultEntry.current
    draggedVaultEntry.current = null
    setVaultDropTarget(null)
    if (!entry || !canMoveVaultEntry(entry.path, targetDirectory)) return
    void moveVaultEntry({
      destinationPath: vaultDragDestination(entry.path, targetDirectory),
      activity: 'Move JSON Vault entry',
      notice: 'notice.moved'
    }, entry)
  }

  function endVaultDrag(): void {
    draggedVaultEntry.current = null
    setVaultDropTarget(null)
  }

  async function openVaultFolder(): Promise<void> {
    try {
      await vaultApi.revealRoot()
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.open') })
    }
  }

  async function duplicateVaultDocument(paths: string[] = vaultFilePaths()): Promise<void> {
    if (!paths.length) return
    if (vaultDirty && vaultDocument && paths.includes(vaultDocument.relativePath)) {
      const saved = await saveVaultDocument()
      if (!saved) return
    }
    setVaultBusy(true)
    try {
      const taken = new Set([...(vault?.files.map((file) => file.relativePath) ?? []), ...(vault?.directories ?? [])])
      let last: VaultDocument | undefined
      for (const path of paths) {
        const source = vaultDocument?.relativePath === path ? vaultDocument : await vaultApi.read(path)
        const destinationPath = uniqueDuplicatePath(path, taken)
        taken.add(destinationPath)
        last = await vaultApi.duplicate(path, destinationPath, source.fingerprint)
      }
      if (last) {
        setVaultDocument(last)
        setVaultSelection({ path: last.relativePath, kind: 'file' })
        setSelectedVaultPaths([last.relativePath])
        vaultSelectionAnchor.current = last.relativePath
        replaceDocument(last.content, { key: 'notice.duplicated', values: { path: last.relativePath } })
      }
      await refreshVault(false)
      recordJsonVaultActivity('Duplicate JSON snippet')
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.duplicate') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function deleteVaultEntry(entry: VaultSelection | undefined = vaultSelection): Promise<void> {
    if (!entry || !await dialog.confirm(t('confirm.remove', { path: entry.path }), { dangerous: true })) return
    setVaultBusy(true)
    try {
      const selected = entry
      const expectedFingerprint = selected.kind === 'file'
        ? vaultDocument?.relativePath === selected.path
          ? vaultDocument.fingerprint
          : (await vaultApi.read(selected.path)).fingerprint
        : null
      const removed = await vaultApi.deleteEntry(
        selected.path,
        expectedFingerprint
      )
      const removesOpenDocument = vaultDocument?.relativePath === selected.path
        || (selected.kind === 'folder' && vaultDocument?.relativePath.startsWith(`${selected.path}/`))
      if (removesOpenDocument) {
        setVaultDocument(undefined)
        replaceDocument('', { key: 'notice.recovered', values: { path: removed.recoveryPath } })
      } else {
        setNotice({ key: 'notice.recovered', values: { path: removed.recoveryPath } })
      }
      setVaultSelection(undefined)
      recordJsonVaultActivity('Delete JSON Vault entry')
      await refreshVault(false)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.delete') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function disconnectVault(): Promise<void> {
    if (vaultDirty && !await dialog.confirm(t('confirm.disconnectDirty'), { dangerous: true })) return
    setVaultBusy(true)
    try {
      await vaultApi.disconnect()
      setVaultDocument(undefined)
      setVaultSelection(undefined)
      setVault(await vaultApi.snapshot())
      setNotice({ key: 'notice.disconnected' })
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.disconnect') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function runGit(
    operation: VaultGitOperation,
    message?: string,
    editorDirty = vaultDirty,
    extra: { path?: string; strategy?: 'ours' | 'theirs' } = {}
  ): Promise<void> {
    const requestId = crypto.randomUUID()
    setGitRequestId(requestId)
    try {
      const result = await vaultApi.runGit({
        requestId,
        operation,
        ...(message ? { message } : {}),
        editorDirty,
        ...extra
      })
      if (!result.success) {
        throw new Error(result.stderr.trim() || result.stdout.trim() || t('notice.gitFailed', { operation }))
      }
      setNotice(result.stdout.trim() ? { raw: result.stdout.trim() } : { key: 'notice.gitDone', values: { operation } })
      const snapshot = await refreshVault(false)
      if (gitDetailsOpen) {
        setGitRemoteDraft(snapshot.git.remote || settings.vault.gitRemote)
        setGitDetails(snapshot.git.repository ? await vaultApi.gitDetails() : { diff: '', commits: [] })
      }
      if (operation === 'discard' || operation === 'abortMerge' || operation === 'resolveConflict') {
        setGitSelected('')
        setGitDiff({ files: [] })
        setGitDiffPath('')
      }
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, `json.vault.git.${operation}`) })
    } finally {
      setGitRequestId('')
    }
  }

  async function cancelGit(): Promise<void> {
    if (gitRequestId) await vaultApi.cancelGit(gitRequestId)
  }

  async function loadGitDetails(open = true): Promise<void> {
    if (!vault?.rootPath) return
    if (open) {
      setGitDetailsOpen(true)
      setGitTab('changes')
      setGitSelected('')
      setGitDiff({ files: [] })
      setGitDiffPath('')
    }
    setGitDetailsBusy(true)
    try {
      const snapshot = await refreshVault(false)
      setGitRemoteDraft(snapshot.git.remote || settings.vault.gitRemote)
      setGitDetails(snapshot.git.repository ? await vaultApi.gitDetails() : { diff: '', commits: [] })
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.git.details') })
    } finally { setGitDetailsBusy(false) }
  }

  async function showGitDiff(input: { path?: string; commit?: string }, selected: string): Promise<void> {
    setGitSelected(selected)
    setGitDiffPath(selected)
    setGitDetailsBusy(true)
    try {
      setGitDiff(await vaultApi.gitDiff(input))
    } catch (cause) {
      setGitDiff({ files: [] })
      setNotice({ raw: errorMessage(cause, 'json.vault.git.diff') })
    } finally {
      setGitDetailsBusy(false)
    }
  }

  function selectGitTab(next: 'changes' | 'history'): void {
    setGitTab(next)
    setGitSelected('')
    setGitDiff({ files: [] })
    setGitDiffPath('')
  }

  async function saveGitRemote(): Promise<void> {
    if (!vault?.git.repository) return
    const value = gitRemoteDraft.trim()
    try {
      const git = await vaultApi.configureGitRemote(value)
      setVault((current) => current ? { ...current, git } : current)
      setGitRemoteDraft(git.remote || value)
      await save((current) => ({ ...current, vault: { ...current.vault, gitRemote: value } }))
      setNotice({ key: value ? 'notice.remoteSaved' : 'notice.remoteRemoved' })
    } catch (cause) { setNotice({ raw: errorMessage(cause, 'json.vault.git.remote') }) }
  }

  const selectedGitChange = vault?.git.changes.find((change) => change.path === gitSelected)
  const gitBusy = Boolean(gitRequestId) || gitDetailsBusy

  return (
    <main className="json-workbench">
      <header className="json-workbench__header">
        <h1 className="visually-hidden">JSON</h1>
        <span className="json-workbench__session">
          {t('session.label')} <code>{sessionId.current}</code>
        </span>
      </header>

      <section className="json-toolbar" aria-label={t('aria.operations')}>
        <button className="secondary-button" type="button" disabled={vaultBusy} onClick={() => void chooseVault()}>
          <FolderOpen />{vault?.rootPath ? t('action.switchVault') : t('action.chooseVault')}
        </button>
        <button className="secondary-button" type="button" disabled={vaultBusy} onClick={() => void createVaultDocument()}>
          <FilePlus2 />{t('action.new')}
        </button>
        <button className="primary-button" type="button" disabled={vaultBusy || Boolean(vaultDocument && !vaultDirty)} onClick={() => void saveVaultDocument()}>
          <Save />{vaultDocument ? t('action.save') : t('action.saveToVault')}
        </button>
        <button className="secondary-button" type="button" onClick={() => void importContent()}>
          <Upload />{t('action.import')}
        </button>
        <button className="secondary-button" type="button" disabled={!metrics.content} onClick={() => void exportContent()}>
          <Download />{t('action.export')}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={() => runTransform(
            () => formatJson(metrics.content, { indent, sortKeys, ignoreCase, checkDuplicateKeys }),
            'notice.formatted'
          )}
        >
          <Sparkles />{t('action.format')}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={() => runTransform(() => minifyJson(metrics.content), 'notice.minified')}
        >
          <UnfoldHorizontal />{t('action.minify')}
        </button>
        <label className="json-toolbar__select">
          {t('option.indent')}
          <select
            value={indent}
            onChange={(event) => setIndent(Number(event.target.value) as 2 | 4 | 8)}
          >
            <option value={2}>2</option>
            <option value={4}>4</option>
            <option value={8}>8</option>
          </select>
        </label>
        <label className="json-toolbar__check">
          <input
            type="checkbox"
            checked={sortKeys}
            onChange={(event) => setSortKeys(event.target.checked)}
          />
          {t('option.sortKeys')}
        </label>
        <label className="json-toolbar__check" title={t('option.ignoreCase')}>
          <input type="checkbox" checked={ignoreCase} onChange={(event) => setIgnoreCase(event.target.checked)} />
          Aa
        </label>
        <label className="json-toolbar__check" title={t('option.duplicateKeys')}>
          <input type="checkbox" checked={checkDuplicateKeys} onChange={(event) => setCheckDuplicateKeys(event.target.checked)} />
          {t('analysis.duplicates')}
        </label>
        <button className="secondary-button" type="button" onClick={() => setWrap((value) => !value)}>
          <WrapText />{wrap ? t('action.wrapOff') : t('action.wrapOn')}
        </button>
        <label className="json-toolbar__select">
          <FontSelect ariaLabel={settingsText('settings.jsonFontFamily')} value={settings.editor.jsonFontName} labels={{ 'ui-monospace': settingsText('settings.interfaceFontMono') }} onChange={(jsonFontName) => { void save({ ...settings, editor: { ...settings.editor, jsonFontName } }) }} />
        </label>
        <button
          className="secondary-button"
          type="button"
          onClick={() => { if (findOpen) { setFindOpen(false); setReplacedCount(0) } else openJsonFind() }}
        >
          <Search />{t('action.find')}
        </button>
        <details className="json-toolbar-more">
          <summary className="secondary-button"><MoreHorizontal />{t('action.more')}</summary>
          <div>
            <button type="button" onClick={() => void copyContent()}>{copied ? <Clipboard /> : <Copy />}{copied ? t('action.copied') : t('action.copy')}</button>
            <button type="button" onClick={() => runTransform(() => escapeJsonString(metrics.content), 'notice.escaped')}><Braces />{t('action.escape')}</button>
            <button type="button" onClick={() => runTransform(() => unescapeJsonString(metrics.content), 'notice.unescaped')}><CodeXml />{t('action.unescape')}</button>
            <button type="button" onClick={() => runTransform(() => escapeJavaString(metrics.content), 'notice.escapedText')}><Braces />{t('action.escapeText')}</button>
            <button type="button" onClick={() => runTransform(() => unescapeJsonText(metrics.content), 'notice.unescapedText')}><CodeXml />{t('action.unescapeText')}</button>
            <button type="button" onClick={() => runTransform(() => swapJsonKeysAndValues(metrics.content, indent), 'notice.swapped')}><ArrowLeftRight />{t('action.swap')}</button>
            <button type="button" onClick={() => runTransform(() => jsonToXml(metrics.content), 'notice.toXml')}><CodeXml />{t('action.toXml')}</button>
            <button type="button" onClick={() => runTransform(() => xmlToJson(metrics.content, indent), 'notice.toJson')}><Braces />{t('action.toJson')}</button>
            <button type="button" onClick={() => runTransform(() => jsonToJavaBean(metrics.content, className), 'notice.toBean')}><CodeXml />{t('action.jsonToBean')}</button>
            <button type="button" onClick={() => runTransform(() => javaBeanToJson(metrics.content), 'notice.beanToJson')}><Braces />{t('action.beanToJson')}</button>
          </div>
        </details>
        <button
          className="icon-button"
          type="button"
          aria-pressed={inspectorOpen}
          aria-label={t('inspector.title')}
          title={t('inspector.title')}
          disabled={!vaultDocument}
          onClick={() => setInspectorOpen((value) => !value)}
        >
          <PanelRight />
        </button>
        <button
          className="icon-button json-toolbar__clear"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => replaceDocument('', { key: 'notice.cleared' })}
        >
          <Eraser />
        </button>
      </section>

      <JsonWorkspace inspectorVisible={inspectorVisible}>
        <aside className="json-vault">
          <div className="json-vault__heading">
            <div><span className="eyebrow">JSON VAULT</span><h2>{t('vault.workspace')}</h2></div>
            <FolderGit2 />
          </div>
          {vault?.rootPath ? (
            <>
              <code className="json-vault__root" title={vault.rootPath}>{vault.rootPath}</code>
              <label className="json-vault__search"><Search /><input aria-label={t('vault.search')} value={vaultQuery} placeholder={t('vault.search')} onChange={(event) => setVaultQuery(event.target.value)} /></label>
              <div className="json-vault__list-controls">
                <label><input type="checkbox" checked={vaultIncludeContent} onChange={(event) => setVaultIncludeContent(event.target.checked)} />{t('vault.searchContent')}</label>
                <select aria-label={t('vault.sort')} value={vaultSort} onChange={(event) => setVaultSort(event.target.value as VaultListSort)}>
                  <option value="name">{t('vault.sortName')}</option>
                  <option value="modified">{t('vault.sortModified')}</option>
                </select>
                <details className="json-toolbar-more json-vault-more">
                  <summary className="icon-button" aria-label={t('vault.more')} title={t('vault.more')}><MoreHorizontal /></summary>
                  <div>
                    <button type="button" disabled={!vaultSelection || multipleVaultSelection || vaultBusy} onClick={() => void renameVaultEntry()}>{t('vault.rename')}</button>
                    <button type="button" disabled={!vaultFilePaths().length || vaultBusy} onClick={() => void exportSelectedVaultFile()}>{t('vault.export')}</button>
                    <button type="button" disabled={vaultBusy} onClick={() => void refreshVault(false)}>{t('vault.refresh')}</button>
                    <button type="button" disabled={vaultBusy} onClick={() => void openVaultFolder()}>{t('vault.openFolder')}</button>
                  </div>
                </details>
              </div>
              <div
                ref={vaultListRef}
                className={vaultDropTarget === '' ? 'json-vault__files json-vault__files--drop' : 'json-vault__files'}
                role="tree"
                aria-multiselectable="true"
                aria-label={t('vault.workspace')}
                onDragOver={(event) => allowVaultDrop(event, '')}
                onDrop={(event) => dropVaultEntry(event, '')}
              >
                {visibleVaultRows.length ? visibleVaultRows.map((row) => row.kind === 'folder' ? (
                  <button
                    key={`folder:${row.path}`}
                    className={['json-vault__folder', selectedVaultPaths.includes(row.path) ? 'json-vault__folder--selected' : '', vaultSelection?.kind === 'folder' && vaultSelection.path === row.path ? 'json-vault__folder--active' : '', vaultDropTarget === row.path ? 'json-vault__folder--drop' : ''].filter(Boolean).join(' ')}
                    type="button"
                    role="treeitem"
                    aria-selected={selectedVaultPaths.includes(row.path)}
                    aria-expanded={expandedFolders ? expandedFolders.has(row.path) : true}
                    aria-level={row.depth + 1}
                    data-path={row.path}
                    title={row.path}
                    style={{ paddingLeft: `${row.depth * 10 + 5}px` }}
                    disabled={vaultBusy}
                    draggable={!vaultBusy && !multipleVaultSelection}
                    onDragStart={(event) => startVaultDrag(event, { path: row.path, kind: 'folder' })}
                    onDragEnd={endVaultDrag}
                    onDragOver={(event) => allowVaultDrop(event, row.path)}
                    onDrop={(event) => dropVaultEntry(event, row.path)}
                    onContextMenu={(event) => { event.preventDefault(); openVaultMenu({ path: row.path, kind: 'folder' }, event.clientX, event.clientY) }}
                    onClick={(event) => {
                      const entry = { path: row.path, kind: 'folder' as const }
                      if (!chooseVaultRow(entry, event)) return
                      setExpandedFolders((current) => {
                        const next = new Set(current ?? resolveExpandedPaths(vaultFolderPaths(vault), jsonExpandMode, row.path))
                        if (next.has(row.path)) next.delete(row.path)
                        else next.add(row.path)
                        return next
                      })
                    }}
                  >{(expandedFolders ? expandedFolders.has(row.path) : true) ? <ChevronDown /> : <ChevronRight />}<FolderOpen /><span>{row.name}</span></button>
                ) : (
                  <button
                    key={row.file.relativePath}
                    className={['json-vault__file', selectedVaultPaths.includes(row.file.relativePath) ? 'json-vault__file--selected' : '', vaultSelection?.kind === 'file' && vaultSelection.path === row.file.relativePath ? 'json-vault__file--active' : ''].filter(Boolean).join(' ')}
                    type="button"
                    role="treeitem"
                    aria-selected={selectedVaultPaths.includes(row.file.relativePath)}
                    aria-level={row.depth + 1}
                    data-path={row.file.relativePath}
                    title={row.file.relativePath}
                    style={{ paddingLeft: `${row.depth * 10 + 7}px` }}
                    disabled={vaultBusy}
                    draggable={!vaultBusy && !multipleVaultSelection}
                    onDragStart={(event) => startVaultDrag(event, { path: row.file.relativePath, kind: 'file' })}
                    onDragEnd={endVaultDrag}
                    onContextMenu={(event) => { event.preventDefault(); openVaultMenu({ path: row.file.relativePath, kind: 'file' }, event.clientX, event.clientY) }}
                    onClick={(event) => {
                      const entry = { path: row.file.relativePath, kind: 'file' as const }
                      if (!chooseVaultRow(entry, event)) return
                      void openVaultDocument(entry.path)
                    }}
                  >
                    <span>{row.name}{vaultDocument?.relativePath === row.file.relativePath && vaultDirty && <i className="json-vault__dirty" />}</span><small>{formatBytes(row.file.sizeBytes)}</small>
                  </button>
                )) : <p className="json-vault__empty">{t(vaultQuery.trim() ? 'vault.noMatches' : 'vault.empty')}</p>}
              </div>
              {multipleVaultSelection && <div className="json-vault__selection">{t('vault.selected', { count: selectedVaultPaths.length })}</div>}
              <div className="json-vault__actions">
                <button type="button" title={t('vault.new')} disabled={vaultBusy} onClick={() => void createVaultDocument()}><FilePlus2 /></button>
                <button type="button" title={t('vault.newFolder')} disabled={vaultBusy} onClick={() => void createVaultDirectory()}><FolderPlus /></button>
                <button type="button" title={t(jsonExpandMode === 'expandAll' ? 'vault.collapseAll' : 'vault.expandAll')} disabled={vaultBusy} onClick={() => void save((current) => ({ ...current, vault: { ...current.vault, jsonTreeExpandMode: current.vault.jsonTreeExpandMode === 'expandAll' ? 'collapseAll' : 'expandAll' } }))}>{jsonExpandMode === 'expandAll' ? <FoldVertical /> : <UnfoldVertical />}</button>
                <button type="button" title={t('vault.locate')} disabled={!vaultLocatePath || vaultBusy} onClick={() => { setVaultQuery(''); setVaultSelection({ path: vaultLocatePath, kind: 'file' }); setExpandedFolders((current) => ensureAncestorsExpanded(current ?? new Set(), vaultLocatePath)); setLocateVaultRequest((value) => value + 1) }}><LocateFixed /></button>
                <button type="button" title={t('vault.move')} disabled={!vaultSelection || multipleVaultSelection || vaultBusy} onClick={() => void moveVaultEntry()}><Pencil /></button>
                <button type="button" title={t('vault.duplicate')} disabled={!vaultFilePaths().length || vaultBusy} onClick={() => void duplicateVaultDocument()}><Copy /></button>
                <button type="button" title={t('vault.recover')} disabled={!vaultSelection || multipleVaultSelection || vaultBusy} onClick={() => void deleteVaultEntry()}><Trash2 /></button>
                <button type="button" title={t('git.open')} disabled={vaultBusy} onClick={() => void loadGitDetails()}><GitBranch />{vault.git.changedFiles > 0 && <span className="json-vault-git-badge">{vault.git.changedFiles > 99 ? '99+' : vault.git.changedFiles}</span>}</button>
                <button type="button" title={t('vault.disconnect')} disabled={vaultBusy} onClick={() => void disconnectVault()}><Unplug /></button>
              </div>
            </>
          ) : (
            <div className="json-vault__empty json-vault__empty--setup">
              <FolderOpen /><p>{t('vault.setup')}</p>
              <button className="secondary-button" type="button" onClick={() => void chooseVault()}>{t('action.chooseDirectory')}</button>
            </div>
          )}
        </aside>
        <div className="json-editor-card">
          {findOpen && (
            <div className="json-find" onKeyDown={(event) => { if (event.key === 'Escape') { event.preventDefault(); setFindOpen(false); setReplacedCount(0) } }}>
              <input aria-label={t('find.placeholder')} autoFocus value={findQuery} placeholder={t('find.placeholder')} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindQuery(event.target.value) }} onKeyDown={(event) => { if (event.key === 'Enter' && !event.nativeEvent.isComposing) { event.preventDefault(); findJsonAround(!event.shiftKey) } }} />
              <button type="button" disabled={!findQuery} onClick={() => findJsonAround(false)}>{t('find.previous')}</button>
              <button type="button" disabled={!findQuery} onClick={() => findJsonAround(true)}>{t('find.next')}</button>
              <span>{findQuery ? t('find.found', { count: findMatches.length }) : ''}</span>
              <label><input type="checkbox" checked={findOptions.matchCase} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindOptions({ ...findOptions, matchCase: event.target.checked }) }} />{t('find.matchCase')}</label>
              <label><input type="checkbox" checked={findOptions.wholeWord} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindOptions({ ...findOptions, wholeWord: event.target.checked }) }} />{t('find.wholeWord')}</label>
              <label><input type="checkbox" checked={findOptions.regex} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindOptions({ ...findOptions, regex: event.target.checked }) }} />{t('find.regex')}</label>
              <input aria-label={t('find.replacePlaceholder')} value={replaceText} placeholder={t('find.replacePlaceholder')} onChange={(event) => setReplaceText(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); replaceJson(false) } }} />
              <button type="button" disabled={!findQuery} onClick={() => replaceJson(false)}>{t('find.replace')}</button>
              <button type="button" disabled={!findQuery} onClick={() => replaceJson(true)}>{t('find.replaceAll')}</button>
              <span>{t('find.replaced', { count: replacedCount })}</span>
              <button type="button" aria-label={t('find.close')} onClick={() => { setFindOpen(false); setReplacedCount(0) }}><X /></button>
            </div>
          )}
          <div ref={hostRef} className="json-editor" />
          <footer className="json-statusbar">
            <span title={vaultDocument?.relativePath ?? ''}>{vaultDirty ? '• ' : ''}{vaultDocument?.relativePath ?? ''}</span>
            <span className={validation.kind === 'valid' ? 'json-statusbar__validation json-statusbar--valid' : validation.kind === 'error' ? 'json-statusbar__validation json-statusbar--error' : 'json-statusbar__validation'}>
              {validation.kind === 'valid' ? <CheckCircle2 size={12} /> : validation.kind === 'error' ? <XCircle size={12} /> : <FileJson size={12} />}
              <span>{validationText}</span>
            </span>
          </footer>
        </div>
        <aside className="json-inspector">
          <div className="json-inspector__heading">
            <div>
              <span className="eyebrow">INSPECTOR</span>
              <h2>{t('inspector.title')}</h2>
            </div>
            <button className="icon-button" type="button" aria-label={t('find.close')} onClick={() => setInspectorOpen(false)}><X /></button>
          </div>

          <dl className="json-analysis">
            <div><dt>{t('analysis.rootType')}</dt><dd>{analysis?.rootType ?? '—'}</dd></div>
            <div><dt>{t('analysis.nodes')}</dt><dd>{analysis?.nodes ?? '—'}</dd></div>
            <div><dt>{t('analysis.keys')}</dt><dd>{analysis?.keys ?? '—'}</dd></div>
            <div><dt>{t('analysis.maxDepth')}</dt><dd>{analysis?.maxDepth ?? '—'}</dd></div>
            <div><dt>{t('analysis.duplicates')}</dt><dd className={duplicates.length ? 'json-duplicate-count json-duplicate-count--error' : 'json-duplicate-count'}>{duplicates.length}</dd></div>
            <div><dt>UTF-8</dt><dd>{analysis ? `${analysis.bytes} B` : '—'}</dd></div>
          </dl>

          {!!duplicates.length && <div className="json-duplicate-list" role="alert">{duplicates.map((path) => <code key={path}>{path}</code>)}</div>}

          <label className="json-path-field">
            {t('bean.className')}
            <input value={className} spellCheck={false} onChange={(event) => setClassName(event.target.value)} />
          </label>

          <label className="json-path-field">
            {t('path.picker')}
            <select value={pathEntries.some((entry) => entry.path === jsonPath) ? jsonPath : ''} onChange={(event) => setJsonPath(event.target.value)}>
              <option value="">—</option>
              {pathEntries.map((entry) => <option key={entry.path} value={entry.path}>{`${'· '.repeat(entry.depth)}${entry.label}`}</option>)}
            </select>
          </label>

          <label className="json-path-field">
            JSONPath
            <input
              value={jsonPath}
              spellCheck={false}
              onChange={(event) => setJsonPath(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter') runJsonPath()
              }}
            />
          </label>
          <button className="secondary-button" type="button" onClick={runJsonPath}>
            {t('action.query')}
          </button>
          <pre className="json-path-result">{pathResult}</pre>
          <section className="json-inspector-result">
            <h3>{t('inspector.result')}</h3>
            <p className={inspectorShowsValidation && validation.kind === 'error' ? 'json-inspector-result--error' : undefined}>{inspectorShowsValidation ? validationText : noticeText}</p>
          </section>
        </aside>
      </JsonWorkspace>

      {gitDetailsOpen && vault && <div className="json-git-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setGitDetailsOpen(false) }}><section className="quick-note-git" role="dialog" aria-modal="true" aria-label={t('git.details')}>
        <header className="quick-note-git__status">
          <div>
            <GitBranch size={15} />
            <strong>{vault.git.repository ? t('git.branch', { branch: vault.git.branch || 'HEAD' }) : t('git.noRepo')}</strong>
            {vault.git.repository && <span>{t('git.sync', { ahead: String(vault.git.ahead), behind: String(vault.git.behind) })}</span>}
          </div>
          <div>
            <button type="button" disabled={gitBusy} onClick={() => void loadGitDetails(false)}><RefreshCw size={13} />{t('git.refresh')}</button>
            {!vault.git.repository && <button type="button" disabled={gitBusy || vault.git.available === false} onClick={() => void runGit('init')}><GitBranch size={13} />{t('git.init')}</button>}
            {vault.git.repository && <button type="button" disabled={gitBusy || !vault.git.remote} onClick={() => void runGit('fetch', undefined, false)}><CloudDownload size={13} />{t('git.fetch')}</button>}
            {vault.git.repository && <button type="button" disabled={gitBusy || vault.git.merging || !vault.git.remote} onClick={() => void pullVault()}>{t('git.pull')}</button>}
            {vault.git.repository && <button type="button" disabled={gitBusy || vault.git.merging || !vault.git.remote} onClick={() => void runGit('push', undefined, false)}><CloudUpload size={13} />{t('git.push')}</button>}
            {vault.git.repository && (vault.git.merging || vault.git.conflicts > 0) && <button type="button" disabled={gitBusy} onClick={() => { void dialog.confirm(t('git.confirmAbort'), { dangerous: true }).then((confirmed) => { if (confirmed) void runGit('abortMerge', undefined, false) }) }}>{t('git.abortMerge')}</button>}
            {vault.git.repository && vault.git.merging && vault.git.conflicts === 0 && <button type="button" disabled={gitBusy} onClick={() => void runGit('continueOperation', undefined, false)}>{t('git.continueOperation')}</button>}
            {gitRequestId && <button type="button" onClick={() => void cancelGit()}><LoaderCircle className="spin" size={13} />{t('git.cancelAction')}</button>}
            <button type="button" onClick={() => setGitDetailsOpen(false)}>{t('git.close')}</button>
          </div>
        </header>
        {vault.git.available === false ? <div className="quick-note-git__empty">{t('git.unavailable')}</div> : (
          <>
            <div className="quick-note-git__remote">
              <label htmlFor="json-git-remote">{t('git.remote')}</label>
              <input id="json-git-remote" value={gitRemoteDraft} placeholder={t('git.remotePlaceholder')} onChange={(event) => setGitRemoteDraft(event.target.value)} />
              <button type="button" disabled={gitBusy || !vault.git.repository || (!gitRemoteDraft.trim() && !vault.git.remote)} onClick={() => void saveGitRemote()}>{gitRemoteDraft.trim() ? t('git.saveRemote') : t('git.removeRemote')}</button>
            </div>
            <div className="quick-note-git__workspace">
              <div>
                <div className="quick-note-git__tabs" role="tablist">
                  <button className={gitTab === 'changes' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" role="tab" aria-selected={gitTab === 'changes'} onClick={() => selectGitTab('changes')}>{t('git.changes')} {vault.git.changes.length}</button>
                  <button className={gitTab === 'history' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" role="tab" aria-selected={gitTab === 'history'} onClick={() => selectGitTab('history')}>{t('git.history')}</button>
                </div>
                <div className="quick-note-git__list">
                  {gitTab === 'changes' ? (
                    vault.git.changes.length ? vault.git.changes.map((change) => (
                      <button className={gitSelected === change.path ? 'quick-note-git__item quick-note-git__item--selected' : 'quick-note-git__item'} type="button" key={`${change.status}-${change.path}`} onClick={() => void showGitDiff({ path: change.path }, change.path)}>
                        <code>{change.status}</code><span>{change.path}</span>{change.conflict && <em>{t('git.conflict')}</em>}
                      </button>
                    )) : <div className="quick-note-git__empty">{t('git.emptyChanges')}</div>
                  ) : (
                    gitDetails.commits.length ? gitDetails.commits.map((commit) => (
                      <button className={gitSelected === commit.hash ? 'quick-note-git__item quick-note-git__item--selected' : 'quick-note-git__item'} type="button" key={commit.hash} onClick={() => void showGitDiff({ commit: commit.hash }, commit.hash)}>
                        <code>{commit.hash.slice(0, 7)}</code><span><strong>{commit.subject}</strong><small>{commit.author} · {new Date(commit.timestamp * 1000).toLocaleString(locale)}</small></span>
                      </button>
                    )) : <div className="quick-note-git__empty">{t('git.emptyHistory')}</div>
                  )}
                </div>
                {vault.git.repository && gitTab === 'changes' && selectedGitChange && (
                  <div className="quick-note-git__change-actions">
                    <button type="button" disabled={gitBusy} onClick={() => { void dialog.confirm(t('git.confirmDiscard', { path: selectedGitChange.path }), { dangerous: true }).then((confirmed) => { if (confirmed) void runGit('discard', undefined, false, { path: selectedGitChange.path }) }) }}><Undo2 size={13} />{t('git.discard')}</button>
                    {selectedGitChange.conflict && <>
                      <button type="button" disabled={gitBusy} onClick={() => void runGit('resolveConflict', undefined, false, { path: selectedGitChange.path, strategy: 'ours' })}>{t('git.useOurs')}</button>
                      <button type="button" disabled={gitBusy} onClick={() => void runGit('resolveConflict', undefined, false, { path: selectedGitChange.path, strategy: 'theirs' })}>{t('git.useTheirs')}</button>
                    </>}
                  </div>
                )}
                {vault.git.repository && gitTab === 'changes' && (
                  <div className="quick-note-git__commit">
                    <input aria-label={t('git.commitMessage')} value={gitCommitMessage} onChange={(event) => setGitCommitMessage(event.target.value)} />
                    <button type="button" disabled={gitBusy || vault.git.merging || vault.git.conflicts > 0 || vault.git.changes.length === 0 || !gitCommitMessage.trim()} onClick={() => void runGit('commit', gitCommitMessage, false)}><GitCommitHorizontal size={13} />{t('git.commit')}</button>
                  </div>
                )}
              </div>
              <GitDiffView
                files={gitDiff.files}
                selectedPath={gitDiffPath}
                onSelectPath={setGitDiffPath}
                labels={{
                  title: gitDetailsBusy ? t('git.loading') : t('git.diff'),
                  before: t('git.diffBefore'),
                  after: t('git.diffAfter'),
                  empty: t('git.noDiff'),
                  binary: t('git.diffBinary'),
                  tooLarge: t('git.diffTooLarge'),
                  file: t('git.diffFile')
                }}
              />
            </div>
          </>
        )}
      </section></div>}

      {vaultMenu && createPortal(
        <div className="quick-note-tree-menu json-vault-menu" role="menu" style={{ left: vaultMenu.left, top: vaultMenu.top }} onPointerDown={(event) => event.stopPropagation()}>
          <button type="button" role="menuitem" disabled={!vaultFilePaths(vaultMenu.paths).length || vaultBusy} onClick={() => { const paths = vaultFilePaths(vaultMenu.paths); setVaultMenu(null); void duplicateVaultDocument(paths) }}>{t('vault.contextDuplicate')}</button>
          <button type="button" role="menuitem" disabled={!vaultFilePaths(vaultMenu.paths).length || vaultBusy} onClick={() => { const paths = vaultFilePaths(vaultMenu.paths); setVaultMenu(null); void exportSelectedVaultFile(paths) }}>{t('vault.export')}</button>
          <button type="button" role="menuitem" disabled={vaultMenu.paths.length > 1 || vaultBusy} onClick={() => { const entry = vaultMenu.entry; setVaultMenu(null); void renameVaultEntry(entry) }}>{t('vault.rename')}</button>
          <button type="button" role="menuitem" disabled={vaultMenu.paths.length > 1 || vaultBusy} onClick={() => { const entry = vaultMenu.entry; setVaultMenu(null); void moveVaultEntry(undefined, entry) }}>{t('vault.contextMove')}</button>
          <button type="button" role="menuitem" disabled={vaultMenu.paths.length > 1 || vaultBusy} onClick={() => { const entry = vaultMenu.entry; setVaultMenu(null); void deleteVaultEntry(entry) }}>{t('vault.contextDelete')}</button>
          <button type="button" role="menuitem" disabled={vaultMenu.paths.length > 1 || vaultBusy} onClick={() => { const entry = vaultMenu.entry; setVaultMenu(null); void revealVaultEntry(entry) }}>{t('vault.reveal')}</button>
        </div>,
        document.body
      )}

      {reportError && (
        <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>
      )}
    </main>
  )
}

type VaultRow =
  | { kind: 'folder'; path: string; name: string; depth: number }
  | { kind: 'file'; file: VaultSnapshot['files'][number]; name: string; depth: number }

function vaultFolderPaths(snapshot: VaultSnapshot): string[] {
  return buildVaultRows(snapshot.files, snapshot.directories)
    .flatMap((row) => row.kind === 'folder' ? [row.path] : [])
}

function buildVaultRows(files: VaultSnapshot['files'], directories: VaultSnapshot['directories']): VaultRow[] {
  const folders = new Set(directories)
  for (const file of files) {
    const segments = file.relativePath.split('/')
    for (let depth = 0; depth < segments.length - 1; depth += 1) {
      folders.add(segments.slice(0, depth + 1).join('/'))
    }
  }
  const rows: VaultRow[] = [...folders].map((path) => {
    const segments = path.split('/')
    return { kind: 'folder', path, name: segments.at(-1) ?? path, depth: segments.length - 1 }
  })
  rows.push(...files.map((file) => {
    const segments = file.relativePath.split('/')
    return { kind: 'file' as const, file, name: segments.at(-1) ?? file.relativePath, depth: segments.length - 1 }
  }))
  return rows.sort((left, right) => {
    const leftPath = left.kind === 'folder' ? left.path : left.file.relativePath
    const rightPath = right.kind === 'folder' ? right.path : right.file.relativePath
    return leftPath.localeCompare(rightPath)
  })
}

function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / (1024 * 1024)).toFixed(1)} MB`
}

function localizeNotice(notice: JsonNotice, t: (key: JsonMessageKey, values?: MessageValues) => string): string {
  return 'raw' in notice ? notice.raw : t(notice.key, notice.values)
}

function jsonErrorText(cause: unknown, t: (key: JsonMessageKey, values?: MessageValues) => string): string {
  if (cause instanceof JsonToolError) return t(`error.${cause.code}`, cause.values)
  return cause instanceof Error ? cause.message : t('error.parse')
}
