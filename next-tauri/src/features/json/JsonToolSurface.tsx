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
  CheckCircle2,
  Clipboard,
  CodeXml,
  Copy,
  Download,
  Eraser,
  FilePlus2,
  FolderGit2,
  FolderOpen,
  GitBranch,
  LoaderCircle,
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
import { vaultApi } from '../../platform/api/vaultApi'
import type {
  VaultDocument,
  VaultGitOperation,
  VaultSnapshot
} from '../../platform/contracts/vault'
import { errorMessage } from '../../shared/errors'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import { useSettings } from '../settings/SettingsProvider'
import { describeJsonSession } from './jsonSession'
import { jsonMessages } from './jsonMessages'
import {
  analyzeJson,
  escapeJsonString,
  formatJson,
  minifyJson,
  queryJsonPath,
  unescapeJsonString,
  validateJson,
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

export function JsonToolSurface() {
  const { settings } = useSettings()
  const { t } = useLocalizedMessages(jsonMessages)
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
    } catch (cause) {
      setNotice({ raw: jsonErrorText(cause, t) })
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
      setNotice({ key: 'notice.vaultConnected', values: { root } })
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.configure') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function openVaultDocument(relativePath: string): Promise<void> {
    if (vaultDirty && !window.confirm(t('confirm.openDirty'))) return
    setVaultBusy(true)
    try {
      const document = await vaultApi.read(relativePath)
      setVaultDocument(document)
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
    const relativePath = window.prompt(t('prompt.newFile'), 'untitled.json')?.trim()
    if (!relativePath) return
    setVaultBusy(true)
    try {
      const document = await vaultApi.save({
        relativePath,
        content: metrics.content,
        expectedFingerprint: null
      })
      setVaultDocument(document)
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

  async function deleteVaultDocument(): Promise<void> {
    if (!vaultDocument || !window.confirm(t('confirm.remove', { path: vaultDocument.relativePath }))) return
    setVaultBusy(true)
    try {
      const removed = await vaultApi.delete(
        vaultDocument.relativePath,
        vaultDocument.fingerprint
      )
      setVaultDocument(undefined)
      replaceDocument('', { key: 'notice.recovered', values: { path: removed.recoveryPath } })
      await refreshVault(false)
    } catch (cause) {
      setNotice({ raw: errorMessage(cause, 'json.vault.delete') })
    } finally {
      setVaultBusy(false)
    }
  }

  async function disconnectVault(): Promise<void> {
    if (vaultDirty && !window.confirm(t('confirm.disconnectDirty'))) return
    setVaultBusy(true)
    try {
      await vaultApi.disconnect()
      setVaultDocument(undefined)
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
        <button className="secondary-button" type="button" onClick={() => void copyContent()}>
          {copied ? <Clipboard /> : <Copy />}{copied ? t('action.copied') : t('action.copy')}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={() => runTransform(() => escapeJsonString(metrics.content), 'notice.escaped')}
        >
          <Braces />{t('action.escape')}
        </button>
        <button
          className="secondary-button"
          type="button"
          onClick={() => runTransform(() => unescapeJsonString(metrics.content), 'notice.unescaped')}
        >
          <CodeXml />{t('action.unescape')}
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

      <section className={`json-status json-status--${validation.kind}`}>
        {validation.kind === 'valid' ? <CheckCircle2 /> : validation.kind === 'error' ? <XCircle /> : <Braces />}
        <strong>{validationText}</strong>
        <span>{noticeText}</span>
      </section>

      <section className="json-workbench__content">
        <aside className="json-vault">
          <div className="json-vault__heading">
            <div><span className="eyebrow">JSON VAULT</span><h2>{t('vault.workspace')}</h2></div>
            <FolderGit2 />
          </div>
          {vault?.rootPath ? (
            <>
              <code className="json-vault__root" title={vault.rootPath}>{vault.rootPath}</code>
              <div className="json-vault__files">
                {vault.files.length ? vault.files.map((file) => (
                  <button
                    key={file.relativePath}
                    className={vaultDocument?.relativePath === file.relativePath ? 'json-vault__file json-vault__file--active' : 'json-vault__file'}
                    type="button"
                    disabled={vaultBusy}
                    onClick={() => void openVaultDocument(file.relativePath)}
                  >
                    <span>{file.relativePath}</span><small>{formatBytes(file.sizeBytes)}</small>
                  </button>
                )) : <p className="json-vault__empty">{t('vault.empty')}</p>}
              </div>
              <div className="json-vault__actions">
                <button type="button" title={t('vault.new')} disabled={vaultBusy} onClick={() => void createVaultDocument()}><FilePlus2 /></button>
                <button type="button" title={t('vault.recover')} disabled={!vaultDocument || vaultBusy} onClick={() => void deleteVaultDocument()}><Trash2 /></button>
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
      </section>

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
