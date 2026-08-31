import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { json } from '@codemirror/lang-json'
import {
  bracketMatching,
  defaultHighlightStyle,
  syntaxHighlighting
} from '@codemirror/language'
import { Compartment, EditorState } from '@codemirror/state'
import { openSearchPanel, search, searchKeymap } from '@codemirror/search'
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
  Clipboard,
  CodeXml,
  Copy,
  Download,
  Eraser,
  FilePlus2,
  FolderPlus,
  FolderGit2,
  FolderOpen,
  GitBranch,
  LoaderCircle,
  MoreHorizontal,
  Pencil,
  Save,
  Search,
  Sparkles,
  Trash2,
  UnfoldHorizontal,
  Unplug,
  Upload,
  WrapText,
  XCircle
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { vaultApi } from '../../platform/api/vaultApi'
import type {
  VaultDocument,
  VaultGitOperation,
  VaultSnapshot
} from '../../platform/contracts/vault'
import { errorMessage } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { ResizableThreeColumns } from '../../shared/ResizableThreeColumns'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import { useOperationHistory } from '../history/useOperationHistory'
import { useSettings } from '../settings/SettingsProvider'
import { describeJsonSession } from './jsonSession'
import { jsonMessages } from './jsonMessages'
import {
  analyzeJson,
  escapeJsonString,
  formatJson,
  jsonToXml,
  minifyJson,
  queryJsonPath,
  swapJsonKeysAndValues,
  unescapeJsonString,
  validateJson,
  xmlToJson,
  JsonToolError
} from './jsonTools'

type JsonMessageKey = LocalizedMessageKey<typeof jsonMessages>
type JsonNotice = { key: JsonMessageKey; values?: MessageValues } | { raw: string }

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
  const { settings } = useSettings()
  const { t } = useLocalizedMessages(jsonMessages)
  const recordOperation = useOperationHistory('json')
  const sampleJson = useRef(t('sample.json')).current
  const hostRef = useRef<HTMLDivElement>(null)
  const editorRef = useRef<EditorView | undefined>(undefined)
  const sessionId = useRef(crypto.randomUUID())
  const revision = useRef(0)
  const wrapCompartment = useRef(new Compartment())
  const ariaCompartment = useRef(new Compartment())
  const [metrics, setMetrics] = useState<EditorMetrics>({
    content: sampleJson,
    anchor: 0,
    head: 0,
    line: 1,
    scrollTop: 0
  })
  const [wrap, setWrap] = useState(settings.editor.wordWrap)
  const [indent, setIndent] = useState(settings.editor.tabSize)
  const [sortKeys, setSortKeys] = useState(false)
  const [jsonPath, setJsonPath] = useState('$.release.channel')
  const [pathResult, setPathResult] = useState('"next-tauri"')
  const [notice, setNotice] = useState<JsonNotice>({ key: 'notice.ready' })
  const [copied, setCopied] = useState(false)
  const [compositionStarts, setCompositionStarts] = useState(0)
  const [compositionEnds, setCompositionEnds] = useState(0)
  const [reportError, setReportError] = useState('')
  const [vault, setVault] = useState<VaultSnapshot>()
  const [vaultDocument, setVaultDocument] = useState<VaultDocument>()
  const [vaultSelection, setVaultSelection] = useState<VaultSelection>()
  const [vaultBusy, setVaultBusy] = useState(false)
  const [gitRequestId, setGitRequestId] = useState('')
  const metricsRef = useRef(metrics)
  const vaultDocumentRef = useRef(vaultDocument)
  metricsRef.current = metrics
  vaultDocumentRef.current = vaultDocument
  const vaultDirty = Boolean(vaultDocument && metrics.content !== vaultDocument.content)
  const noticeText = localizeNotice(notice, t)

  useEffect(() => {
    setWrap(settings.editor.wordWrap)
    setIndent(settings.editor.tabSize)
  }, [settings.editor.tabSize, settings.editor.wordWrap])

  useEffect(() => {
    const host = hostRef.current
    if (!host) return

    const view = new EditorView({
      parent: host,
      state: EditorState.create({
        doc: sampleJson,
        extensions: [
          history(),
          lineNumbers(),
          drawSelection(),
          highlightActiveLine(),
          highlightActiveLineGutter(),
          bracketMatching(),
          json(),
          search({ top: false }),
          syntaxHighlighting(defaultHighlightStyle, { fallback: true }),
          keymap.of([
            ...searchKeymap,
            ...defaultKeymap,
            ...historyKeymap,
            indentWithTab
          ]),
          wrapCompartment.current.of(EditorView.lineWrapping),
          ariaCompartment.current.of(EditorView.contentAttributes.of({
            'aria-label': t('aria.editor'),
            spellcheck: 'false',
            autocapitalize: 'off',
            autocomplete: 'off'
          })),
          EditorView.updateListener.of((update) => {
            if (!update.docChanged && !update.selectionSet) return
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

    view.scrollDOM.addEventListener('scroll', handleScroll, { passive: true })
    view.contentDOM.addEventListener('compositionstart', handleCompositionStart)
    view.contentDOM.addEventListener('compositionend', handleCompositionEnd)
    editorRef.current = view
    view.focus()

    return () => {
      view.scrollDOM.removeEventListener('scroll', handleScroll)
      view.contentDOM.removeEventListener('compositionstart', handleCompositionStart)
      view.contentDOM.removeEventListener('compositionend', handleCompositionEnd)
      editorRef.current = undefined
      view.destroy()
    }
  }, [])

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
  }, [])

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
      effects: ariaCompartment.current.reconfigure(EditorView.contentAttributes.of({
        'aria-label': t('aria.editor'),
        spellcheck: 'false',
        autocapitalize: 'off',
        autocomplete: 'off'
      }))
    })
  }, [t])

  const validation = useMemo(() => validateJson(metrics.content), [metrics.content])
  const analysis = useMemo(() => analyzeJson(metrics.content), [metrics.content])
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

  function runTransform(transform: () => string, success: JsonMessageKey): void {
    try {
      replaceDocument(transform(), { key: success })
      recordOperation(t(success), t('operation.content', { length: metrics.content.length }), 'success')
    } catch (cause) {
      const message = jsonErrorText(cause, t)
      setNotice({ raw: message })
      recordOperation(t(success), message, 'error')
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
      recordOperation(t('operation.import'), `${file.name} · ${content.length}`, 'success')
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
      setPathResult(queryJsonPath(metrics.content, jsonPath) ?? t('path.noMatch'))
      setNotice({ key: 'notice.pathDone' })
    } catch (cause) {
      const message = jsonErrorText(cause, t)
      setPathResult(message)
      setNotice({ raw: message })
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

  async function refreshVault(external: boolean): Promise<void> {
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
    if (!opened || !snapshot.rootPath) return
    if (!snapshot.files.some((file) => file.relativePath === opened.relativePath)) {
      if (external) setNotice({ key: 'notice.externalRemoved', values: { path: opened.relativePath } })
      return
    }
    if (metricsRef.current.content !== opened.content) {
      if (external) setNotice({ key: 'notice.externalDirty' })
      return
    }
    const latest = await vaultApi.read(opened.relativePath)
    if (latest.fingerprint !== opened.fingerprint) {
      setVaultDocument(latest)
      replaceDocument(latest.content, { key: 'notice.externalLoaded', values: { path: latest.relativePath } })
    }
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
      await autoCommit(document.relativePath)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.create') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function saveVaultDocument(): Promise<void> {
    if (!vaultDocument) {
      await createVaultDocument()
      return
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
      await autoCommit(saved.relativePath)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.save') })
    } finally {
      setVaultBusy(false)
    }
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
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.create-directory') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function moveVaultEntry(): Promise<void> {
    if (!vaultSelection) return
    const destinationPath = (await dialog.prompt(
      t('prompt.moveEntry', { path: vaultSelection.path }),
      vaultSelection.path
    ))?.trim()
    if (!destinationPath || destinationPath === vaultSelection.path) return
    setVaultBusy(true)
    try {
      let expectedFingerprint: string | null = null
      if (vaultSelection.kind === 'file') {
        expectedFingerprint = vaultDocument?.relativePath === vaultSelection.path
          ? vaultDocument.fingerprint
          : (await vaultApi.read(vaultSelection.path)).fingerprint
      }
      const previousPath = vaultSelection.path
      const movedPath = await vaultApi.move(previousPath, destinationPath, expectedFingerprint)
      setVaultSelection({ path: movedPath, kind: vaultSelection.kind })
      setVaultDocument((document) => {
        if (!document) return document
        if (vaultSelection.kind === 'file' && document.relativePath === previousPath) {
          return { ...document, relativePath: movedPath }
        }
        if (vaultSelection.kind === 'folder' && document.relativePath.startsWith(`${previousPath}/`)) {
          return { ...document, relativePath: `${movedPath}${document.relativePath.slice(previousPath.length)}` }
        }
        return document
      })
      await refreshVault(false)
      setNotice({ key: 'notice.moved', values: { path: movedPath } })
      await autoCommit(movedPath)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.move') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function duplicateVaultDocument(): Promise<void> {
    if (vaultSelection?.kind !== 'file') return
    if (vaultDirty && vaultDocument?.relativePath === vaultSelection.path
      && !await dialog.confirm(t('confirm.duplicateDirty'), { dangerous: true })) return
    const destinationPath = (await dialog.prompt(
      t('prompt.duplicateFile'),
      duplicatePath(vaultSelection.path)
    ))?.trim()
    if (!destinationPath) return
    setVaultBusy(true)
    try {
      const source = vaultDocument?.relativePath === vaultSelection.path
        ? vaultDocument
        : await vaultApi.read(vaultSelection.path)
      const document = await vaultApi.duplicate(
        vaultSelection.path,
        destinationPath,
        source.fingerprint
      )
      setVaultDocument(document)
      setVaultSelection({ path: document.relativePath, kind: 'file' })
      replaceDocument(document.content, { key: 'notice.duplicated', values: { path: document.relativePath } })
      await refreshVault(false)
      await autoCommit(document.relativePath)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.duplicate') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function deleteVaultEntry(): Promise<void> {
    if (!vaultSelection || !await dialog.confirm(t('confirm.remove', { path: vaultSelection.path }), { dangerous: true })) return
    setVaultBusy(true)
    try {
      const selected = vaultSelection
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
    editorDirty = vaultDirty
  ): Promise<void> {
    const requestId = crypto.randomUUID()
    setGitRequestId(requestId)
    try {
      const result = await vaultApi.runGit({
        requestId,
        operation,
        ...(message ? { message } : {}),
        editorDirty
      })
      if (!result.success) {
        throw new Error(result.stderr.trim() || result.stdout.trim() || t('notice.gitFailed', { operation }))
      }
      setNotice(result.stdout.trim() ? { raw: result.stdout.trim() } : { key: 'notice.gitDone', values: { operation } })
      await refreshVault(false)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, `json.vault.git.${operation}`) })
    } finally {
      setGitRequestId('')
    }
  }

  async function autoCommit(relativePath: string): Promise<void> {
    if (!settings.vault.autoCommit || !vault?.git.repository) return
    await runGit('commit', `Update ${relativePath} from MooTool Next Tauri`, false)
  }

  async function cancelGit(): Promise<void> {
    if (gitRequestId) await vaultApi.cancelGit(gitRequestId)
  }

  return (
    <main className="json-workbench">
      <header className="json-workbench__header">
        <div>
          <span className="eyebrow">TAURI JSON WORKBENCH</span>
          <h1>JSON</h1>
        </div>
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
            () => formatJson(metrics.content, { indent, sortKeys }),
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
        <button className="secondary-button" type="button" onClick={() => setWrap((value) => !value)}>
          <WrapText />{wrap ? t('action.wrapOff') : t('action.wrapOn')}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={() => openSearchPanel(editorRef.current!)}
        >
          <Search />{t('action.find')}
        </button>
        <details className="json-toolbar-more">
          <summary className="secondary-button"><MoreHorizontal />{t('action.more')}</summary>
          <div>
            <button type="button" onClick={() => void copyContent()}>{copied ? <Clipboard /> : <Copy />}{copied ? t('action.copied') : t('action.copy')}</button>
            <button type="button" onClick={() => runTransform(() => escapeJsonString(metrics.content), 'notice.escaped')}><Braces />{t('action.escape')}</button>
            <button type="button" onClick={() => runTransform(() => unescapeJsonString(metrics.content), 'notice.unescaped')}><CodeXml />{t('action.unescape')}</button>
            <button type="button" onClick={() => runTransform(() => swapJsonKeysAndValues(metrics.content, indent), 'notice.swapped')}><ArrowLeftRight />{t('action.swap')}</button>
            <button type="button" onClick={() => runTransform(() => jsonToXml(metrics.content), 'notice.toXml')}><CodeXml />{t('action.toXml')}</button>
            <button type="button" onClick={() => runTransform(() => xmlToJson(metrics.content, indent), 'notice.toJson')}><Braces />{t('action.toJson')}</button>
          </div>
        </details>
        <button
          className="icon-button json-toolbar__clear"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => replaceDocument('', { key: 'notice.cleared' })}
        >
          <Eraser />
        </button>
      </section>

      <section className={`json-status json-status--${validation.kind}`}>
        {validation.kind === 'valid' ? <CheckCircle2 /> : validation.kind === 'error' ? <XCircle /> : <Braces />}
        <strong>{validationText}</strong>
        <span>{noticeText}</span>
      </section>

      <ResizableThreeColumns id="json-workbench" className="json-workbench__content" initialLeft={210} initialRight={250}>
        <aside className="json-vault">
          <div className="json-vault__heading">
            <div><span className="eyebrow">JSON VAULT</span><h2>{t('vault.workspace')}</h2></div>
            <FolderGit2 />
          </div>
          {vault?.rootPath ? (
            <>
              <code className="json-vault__root" title={vault.rootPath}>{vault.rootPath}</code>
              <div className="json-vault__files" role="tree" aria-label={t('vault.workspace')}>
                {vault.files.length || vault.directories.length ? buildVaultRows(vault.files, vault.directories).map((row) => row.kind === 'folder' ? (
                  <button
                    key={`folder:${row.path}`}
                    className={vaultSelection?.kind === 'folder' && vaultSelection.path === row.path ? 'json-vault__folder json-vault__folder--active' : 'json-vault__folder'}
                    type="button"
                    role="treeitem"
                    aria-level={row.depth + 1}
                    title={row.path}
                    style={{ paddingLeft: `${row.depth * 10 + 5}px` }}
                    disabled={vaultBusy}
                    onClick={() => setVaultSelection({ path: row.path, kind: 'folder' })}
                  ><FolderOpen /><span>{row.name}</span></button>
                ) : (
                  <button
                    key={row.file.relativePath}
                    className={vaultSelection?.kind === 'file' && vaultSelection.path === row.file.relativePath ? 'json-vault__file json-vault__file--active' : 'json-vault__file'}
                    type="button"
                    role="treeitem"
                    aria-level={row.depth + 1}
                    title={row.file.relativePath}
                    style={{ paddingLeft: `${row.depth * 10 + 7}px` }}
                    disabled={vaultBusy}
                    onClick={() => void openVaultDocument(row.file.relativePath)}
                  >
                    <span>{row.name}</span><small>{formatBytes(row.file.sizeBytes)}</small>
                  </button>
                )) : <p className="json-vault__empty">{t('vault.empty')}</p>}
              </div>
              <div className="json-vault__actions">
                <button type="button" title={t('vault.new')} disabled={vaultBusy} onClick={() => void createVaultDocument()}><FilePlus2 /></button>
                <button type="button" title={t('vault.newFolder')} disabled={vaultBusy} onClick={() => void createVaultDirectory()}><FolderPlus /></button>
                <button type="button" title={t('vault.move')} disabled={!vaultSelection || vaultBusy} onClick={() => void moveVaultEntry()}><Pencil /></button>
                <button type="button" title={t('vault.duplicate')} disabled={vaultSelection?.kind !== 'file' || vaultBusy} onClick={() => void duplicateVaultDocument()}><Copy /></button>
                <button type="button" title={t('vault.recover')} disabled={!vaultSelection || vaultBusy} onClick={() => void deleteVaultEntry()}><Trash2 /></button>
                <button type="button" title={t('vault.disconnect')} disabled={vaultBusy} onClick={() => void disconnectVault()}><Unplug /></button>
              </div>
              <div className="json-vault__git">
                <header><GitBranch /><strong>{vault.git.repository ? vault.git.branch || 'HEAD' : 'Git'}</strong>{vault.git.dirty && <span>{vault.git.changedFiles}</span>}</header>
                {vault.git.available ? (
                  <div>
                    {!vault.git.repository
                      ? <button type="button" disabled={Boolean(gitRequestId)} onClick={() => void runGit('init')}>{t('git.init')}</button>
                      : (
                          <>
                            <button type="button" title={t('git.pull')} disabled={Boolean(gitRequestId) || vaultDirty || vault.git.dirty} onClick={() => void runGit('pull')}><Download /></button>
                            <button type="button" title={t('git.commit')} disabled={Boolean(gitRequestId) || vaultDirty || !vault.git.dirty} onClick={() => void runGit('commit')}><GitBranch /></button>
                            <button type="button" title={t('git.push')} disabled={Boolean(gitRequestId)} onClick={() => void runGit('push')}><Upload /></button>
                          </>
                        )}
                    {gitRequestId && <button type="button" title={t('git.cancel')} onClick={() => void cancelGit()}><LoaderCircle className="spin" />{t('git.cancelAction')}</button>}
                  </div>
                ) : <p>{t('git.unavailable')}</p>}
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
          <div ref={hostRef} className="json-editor" />
        </div>
        <aside className="json-inspector">
          <div className="json-inspector__heading">
            <div>
              <span className="eyebrow">INSPECTOR</span>
              <h2>{t('inspector.title')}</h2>
            </div>
            <Braces />
          </div>

          <dl className="json-analysis">
            <div><dt>{t('analysis.rootType')}</dt><dd>{analysis?.rootType ?? '—'}</dd></div>
            <div><dt>{t('analysis.nodes')}</dt><dd>{analysis?.nodes ?? '—'}</dd></div>
            <div><dt>{t('analysis.keys')}</dt><dd>{analysis?.keys ?? '—'}</dd></div>
            <div><dt>{t('analysis.maxDepth')}</dt><dd>{analysis?.maxDepth ?? '—'}</dd></div>
            <div><dt>UTF-8</dt><dd>{analysis ? `${analysis.bytes} B` : '—'}</dd></div>
          </dl>

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
        </aside>
      </ResizableThreeColumns>

      <footer className="json-workbench__footer">
        <span>{vaultDocument ? `${vaultDocument.relativePath} · ${vaultDirty ? t('footer.unsaved') : t('footer.saved')}` : t('footer.shortcuts')}</span>
        <code>{sessionSummary} · IME {compositionStarts}/{compositionEnds}</code>
      </footer>

      {reportError && (
        <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>
      )}
    </main>
  )
}

type VaultRow =
  | { kind: 'folder'; path: string; name: string; depth: number }
  | { kind: 'file'; file: VaultSnapshot['files'][number]; name: string; depth: number }

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

function duplicatePath(relativePath: string): string {
  const slash = relativePath.lastIndexOf('/')
  const parent = slash >= 0 ? relativePath.slice(0, slash + 1) : ''
  const filename = slash >= 0 ? relativePath.slice(slash + 1) : relativePath
  const stem = filename.replace(/\.json$/i, '')
  return `${parent}${stem}-copy.json`
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
