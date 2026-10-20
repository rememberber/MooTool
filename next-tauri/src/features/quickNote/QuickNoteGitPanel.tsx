import { CloudDownload, CloudUpload, GitBranch, GitCommitHorizontal, RefreshCw, Undo2 } from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { quickNoteGitApi } from '../../platform/api/quickNoteGitApi'
import type { QuickNoteGitAction, QuickNoteGitCommit, QuickNoteGitDiffFile, QuickNoteGitDiffResult, QuickNoteGitStatus } from '../../platform/contracts/quickNoteGit'
import { DiffCodeEditor, type DiffEditorHandle, type DiffEditorScroll } from '../../shared/DiffCodeEditor'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { useSettings } from '../settings/SettingsProvider'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { gitDiffDecorations, gitDiffLanguage } from './gitDiffDecorations'
import { quickNoteMessages } from './quickNoteMessages'

export function QuickNoteGitPanel({ open, onClose, onChanged }: {
  open: boolean
  onClose(): void
  onChanged(): void
}) {
  const { t } = useLocalizedMessages(quickNoteMessages)
  const { settings } = useSettings()
  const dialog = useDesktopDialog()
  const [status, setStatus] = useState<QuickNoteGitStatus | null>(null)
  const [history, setHistory] = useState<QuickNoteGitCommit[]>([])
  const [tab, setTab] = useState<'changes' | 'history'>('changes')
  const [selected, setSelected] = useState('')
  const [diff, setDiff] = useState<QuickNoteGitDiffResult>({ files: [] })
  const [remote, setRemote] = useState('')
  const [message, setMessage] = useState(() => t('git.defaultMessage'))
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')

  const load = useCallback(async () => {
    setBusy(true)
    try {
      const [nextStatus, nextHistory] = await Promise.all([quickNoteGitApi.status(), quickNoteGitApi.history()])
      setStatus(nextStatus)
      setHistory(nextHistory)
      setRemote(nextStatus.remote || settings.vault.gitRemote)
      setNotice('')
    } catch (cause) {
      setNotice(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setBusy(false)
    }
  }, [settings.vault.gitRemote])

  useEffect(() => {
    if (open) void load()
  }, [load, open])

  async function run(action: QuickNoteGitAction, extra: { message?: string; remote?: string; path?: string; strategy?: 'ours' | 'theirs' } = {}): Promise<void> {
    setBusy(true)
    try {
      const result = await quickNoteGitApi.action({ action, ...extra })
      setNotice(result.message)
      if (action === 'pull' || action === 'discard' || action === 'abort-merge' || action === 'resolve-conflict') onChanged()
      await load()
    } catch (cause) {
      setNotice(cause instanceof Error ? cause.message : String(cause))
      setBusy(false)
    }
  }

  async function showDiff(path: string): Promise<void> {
    setSelected(path)
    setBusy(true)
    try { setDiff(await quickNoteGitApi.diff({ path })) } catch (cause) { setNotice(cause instanceof Error ? cause.message : String(cause)); setDiff({ files: [] }) } finally { setBusy(false) }
  }

  async function showCommit(commit: QuickNoteGitCommit): Promise<void> {
    setSelected(commit.hash)
    setBusy(true)
    try { setDiff(await quickNoteGitApi.diff({ commit: commit.hash })) } catch (cause) { setNotice(cause instanceof Error ? cause.message : String(cause)); setDiff({ files: [] }) } finally { setBusy(false) }
  }

  if (!open) return null
  const selectedChange = status?.changes.find((change) => change.path === selected)
  return (
    <div className="quick-note-git-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="quick-note-git" role="dialog" aria-modal="true" aria-label={t('git.title')}>
        <header className="quick-note-git__status">
          <div>
            <GitBranch size={15} />
            <strong>{status?.repository ? t('git.branch', { branch: status.branch }) : t('git.noRepo')}</strong>
            {status?.repository && <span>{t('git.sync', { ahead: String(status.ahead), behind: String(status.behind) })}</span>}
          </div>
          <div>
            <button type="button" disabled={busy} onClick={() => void load()}><RefreshCw size={13} />{t('git.refresh')}</button>
            {!status?.repository && <button type="button" disabled={busy || status?.available === false} onClick={() => void run('init')}><GitBranch size={13} />{t('git.init')}</button>}
            {status?.repository && <button type="button" disabled={busy || !status.remote} onClick={() => void run('fetch')}><CloudDownload size={13} />{t('git.fetch')}</button>}
            {status?.repository && <button type="button" disabled={busy || !status.remote || status.merging} onClick={() => void run('pull')}>{t('git.pull')}</button>}
            {status?.repository && <button type="button" disabled={busy || !status.remote || status.merging} onClick={() => void run('push')}><CloudUpload size={13} />{t('git.push')}</button>}
            <button type="button" onClick={onClose}>{t('git.close')}</button>
          </div>
        </header>
        {notice && <p className="quick-note-git__notice">{notice}</p>}
        {status?.available === false ? <div className="quick-note-git__empty">{t('git.unavailable')}</div> : (
          <>
            <div className="quick-note-git__remote">
              <label htmlFor="quick-note-git-remote">{t('git.remote')}</label>
              <input id="quick-note-git-remote" value={remote} placeholder={t('git.remotePlaceholder')} onChange={(event) => setRemote(event.target.value)} />
              <button type="button" disabled={busy || !status?.repository} onClick={() => void run('configure-remote', { remote })}>{remote.trim() ? t('git.saveRemote') : t('git.removeRemote')}</button>
            </div>
            <div className="quick-note-git__workspace">
              <div>
                <div className="quick-note-git__tabs" role="tablist">
                  <button className={tab === 'changes' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => { setTab('changes'); setSelected(''); setDiff({ files: [] }) }}>{t('git.changes', { count: status?.changes.length ?? 0 })}</button>
                  <button className={tab === 'history' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => { setTab('history'); setSelected(''); setDiff({ files: [] }) }}>{t('git.history')}</button>
                </div>
                <div className="quick-note-git__list">
                  {tab === 'changes' ? (
                    status?.changes.length ? status.changes.map((change) => (
                      <button className={selected === change.path ? 'quick-note-git__item quick-note-git__item--selected' : 'quick-note-git__item'} type="button" key={`${change.status}-${change.path}`} onClick={() => void showDiff(change.path)}>
                        <code>{change.status}</code><span>{change.path}</span>{change.conflict && <em>{t('git.conflict')}</em>}
                      </button>
                    )) : <div className="quick-note-git__empty">{t('git.emptyChanges')}</div>
                  ) : (
                    history.length ? history.map((commit) => (
                      <button className={selected === commit.hash ? 'quick-note-git__item quick-note-git__item--selected' : 'quick-note-git__item'} type="button" key={commit.hash} onClick={() => void showCommit(commit)}>
                        <code>{commit.shortHash}</code><span><strong>{commit.message}</strong><small>{commit.author} · {new Date(commit.date).toLocaleString()}</small></span>
                      </button>
                    )) : <div className="quick-note-git__empty">{t('git.emptyHistory')}</div>
                  )}
                </div>
                {status?.repository && tab === 'changes' && selectedChange && (
                  <div className="quick-note-git__change-actions">
                    <button type="button" disabled={busy} onClick={() => { void dialog.confirm(t('git.confirmDiscard', { path: selectedChange.path }), { dangerous: true }).then((confirmed) => { if (confirmed) return run('discard', { path: selectedChange.path }) }) }}><Undo2 size={13} />{t('git.discard')}</button>
                    {selectedChange.conflict && <>
                      <button type="button" disabled={busy} onClick={() => void run('resolve-conflict', { path: selectedChange.path, strategy: 'ours' })}>{t('git.useOurs')}</button>
                      <button type="button" disabled={busy} onClick={() => void run('resolve-conflict', { path: selectedChange.path, strategy: 'theirs' })}>{t('git.useTheirs')}</button>
                    </>}
                  </div>
                )}
                {status?.repository && tab === 'changes' && (
                  <div className="quick-note-git__commit">
                    <input aria-label={t('git.commitMessage')} value={message} onChange={(event) => setMessage(event.target.value)} />
                    <button type="button" disabled={busy || status.merging || status.conflicts > 0 || !status.changes.length || !message.trim()} onClick={() => void run('commit', { message })}><GitCommitHorizontal size={13} />{t('git.commit')}</button>
                  </div>
                )}
              </div>
              <GitDiffView
                files={diff.files}
                selectedPath={selected}
                onSelectPath={setSelected}
                labels={{
                  title: t('git.diff'),
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
      </section>
    </div>
  )
}

export function GitDiffView({ files, selectedPath, onSelectPath, labels }: {
  files: QuickNoteGitDiffFile[]
  selectedPath: string
  onSelectPath(path: string): void
  labels: { title: string; before: string; after: string; empty: string; binary: string; tooLarge: string; file: string }
}) {
  const file = files.find((item) => item.path === selectedPath) ?? files[0]
  const decorations = useMemo(
    () => file?.preview === 'text' ? gitDiffDecorations(file.before, file.after) : { left: [], right: [] },
    [file]
  )
  const language = useMemo(() => gitDiffLanguage(file?.path ?? ''), [file?.path])
  const leftEditorRef = useRef<DiffEditorHandle>(null)
  const rightEditorRef = useRef<DiffEditorHandle>(null)
  const syncingScroll = useRef(false)

  function syncEditorScroll(scroll: DiffEditorScroll, target: DiffEditorHandle | null): void {
    if (!target || syncingScroll.current) return
    syncingScroll.current = true
    target.syncScroll(scroll.scrollTop, scroll.scrollLeft)
    window.requestAnimationFrame(() => { syncingScroll.current = false })
  }

  return (
    <section className="quick-note-git-diff">
      <header>
        <h3>{labels.title}</h3>
        {files.length > 1 ? (
          <select aria-label={labels.file} value={file?.path ?? ''} onChange={(event) => onSelectPath(event.target.value)}>
            {files.map((item) => <option key={`${item.status}-${item.originalPath ?? ''}-${item.path}`} value={item.path}>{diffFileLabel(item)}</option>)}
          </select>
        ) : file ? <code title={diffFileLabel(file)}>{diffFileLabel(file)}</code> : null}
      </header>
      {!file ? <div className="quick-note-git__empty">{labels.empty}</div> : file.preview !== 'text' ? (
        <div className="quick-note-git__empty">{file.preview === 'binary' ? labels.binary : labels.tooLarge}</div>
      ) : (
        <div className="quick-note-git-diff__columns">
          <div className="quick-note-git-diff__pane">
            <span>{labels.before}</span>
            <DiffCodeEditor
              ref={leftEditorRef}
              ariaLabel={labels.before}
              decorations={decorations.left}
              extensions={language}
              languageKey={file.path}
              value={file.before}
              onScroll={(scroll) => syncEditorScroll(scroll, rightEditorRef.current)}
            />
          </div>
          <div className="quick-note-git-diff__pane">
            <span>{labels.after}</span>
            <DiffCodeEditor
              ref={rightEditorRef}
              ariaLabel={labels.after}
              decorations={decorations.right}
              extensions={language}
              languageKey={file.path}
              value={file.after}
              onScroll={(scroll) => syncEditorScroll(scroll, leftEditorRef.current)}
            />
          </div>
        </div>
      )}
    </section>
  )
}

function diffFileLabel(file: QuickNoteGitDiffFile): string {
  const path = file.originalPath ? `${file.originalPath} → ${file.path}` : file.path
  return `${file.status.trim() || 'M'}  ${path}`
}
