import { CloudDownload, CloudUpload, GitBranch, GitCommitHorizontal, GitMerge, RefreshCw, ShieldCheck, Undo2 } from 'lucide-react'
import { useCallback, useEffect, useMemo, useReducer, useRef } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import {
  TextCodeEditor,
  type TextCodeEditorDecoration,
  type TextCodeEditorHandle,
  type TextCodeEditorScroll
} from '@/shared/components/TextCodeEditor'
import { resolveTextCodeEditorLanguage } from '@/shared/components/codeEditorLanguage'
import type {
  VaultGitAction,
  VaultGitCommit,
  VaultGitDiffFile,
  VaultGitDiffResult,
  VaultGitStatus
} from '@/shared/contracts/vaultGit'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { compareText, type DiffSegment } from '../diff/diffTools'

type VaultGitDialogProps = {
  open: boolean
  onClose: () => void
  onVaultChange: (action: VaultGitAction) => void | Promise<void>
  beforeWorkingTreeChange?: (action: VaultGitAction, path?: string) => boolean | Promise<boolean>
  scope?: 'json' | 'quickNote'
}

type GitPanelState = {
  status: VaultGitStatus | null
  history: VaultGitCommit[]
  tab: 'changes' | 'history'
  diff: VaultGitDiffResult | null
  diffFile: string
  selected: string
  remote: string
  commitMessage: string
  busy: boolean
}

function updateState(state: GitPanelState, patch: Partial<GitPanelState>): GitPanelState {
  return { ...state, ...patch }
}

const workingTreeActions = new Set<VaultGitAction>(['pull', 'discard', 'abort-merge', 'resolve-conflict', 'continue-operation'])

export function VaultGitDialog({ open, onClose, onVaultChange, beforeWorkingTreeChange, scope = 'json' }: VaultGitDialogProps) {
  const { t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const toast = useToast()
  const desktopDialog = useDesktopDialog()
  const [state, update] = useReducer(updateState, {
    status: null,
    history: [],
    tab: 'changes',
    diff: null,
    diffFile: '',
    selected: '',
    remote: settings.vault.gitRemote,
    commitMessage: scope === 'quickNote' ? t('quickNote.git.defaultMessage') : t('json.git.defaultMessage'),
    busy: false
  })

  const load = useCallback(async () => {
    update({ busy: true })
    try {
      const [status, history] = await Promise.all([
        scope === 'quickNote' ? window.mootool.getQuickNoteGitStatus() : window.mootool.getVaultGitStatus(),
        scope === 'quickNote' ? window.mootool.listQuickNoteGitHistory() : window.mootool.listVaultGitHistory()
      ])
      update({ status, history, remote: status.remote || settings.vault.gitRemote, busy: false })
    } catch (error) {
      update({ busy: false })
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }, [scope, settings.vault.gitRemote, t, toast])

  useEffect(() => {
    if (open) void load()
  }, [load, open])

  async function runAction(action: VaultGitAction, extra: { message?: string; remote?: string; path?: string; strategy?: 'ours' | 'theirs' } = {}): Promise<void> {
    update({ busy: true })
    try {
      if (workingTreeActions.has(action) && beforeWorkingTreeChange && !await beforeWorkingTreeChange(action, extra.path)) {
        update({ busy: false })
        return
      }
      const result = scope === 'quickNote'
        ? await window.mootool.runQuickNoteGitAction({ action, ...extra })
        : await window.mootool.runVaultGitAction({ action, ...extra })
      if (!result.success) {
        toast.error(result.message)
        if (action === 'pull') {
          await load()
          await onVaultChange(action)
        } else {
          update({ busy: false })
        }
        return
      }
      const configuredRemote = action === 'configure-remote' ? extra.remote?.trim() ?? '' : undefined
      if (configuredRemote !== undefined) await updateSettings({ vault: { gitRemote: configuredRemote } })
      toast.success(t('json.git.done'))
      if (action === 'discard' || action === 'abort-merge' || action === 'resolve-conflict') {
        update({ selected: '', diff: null, diffFile: '' })
      }
      await load()
      if (configuredRemote !== undefined) update({ remote: configuredRemote })
      if (workingTreeActions.has(action)) await onVaultChange(action)
    } catch (error) {
      update({ busy: false })
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }

  async function showWorkingDiff(path: string): Promise<void> {
    update({ selected: path, diff: null, diffFile: '', busy: true })
    try {
      const diff = await (scope === 'quickNote' ? window.mootool.getQuickNoteGitDiff({ path }) : window.mootool.getVaultGitDiff({ path }))
      update({ diff, diffFile: diff.files[0]?.path ?? '', busy: false })
    } catch (error) {
      update({ busy: false })
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }

  async function showCommitDiff(commit: VaultGitCommit): Promise<void> {
    update({ selected: commit.hash, diff: null, diffFile: '', busy: true })
    try {
      const diff = await (scope === 'quickNote' ? window.mootool.getQuickNoteGitDiff({ commit: commit.hash }) : window.mootool.getVaultGitDiff({ commit: commit.hash }))
      update({ diff, diffFile: diff.files[0]?.path ?? '', busy: false })
    } catch (error) {
      update({ busy: false })
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }

  const status = state.status
  const selectedChange = status?.changes.find((change) => change.path === state.selected)
  return (
    <Dialog
      title={scope === 'quickNote' ? t('quickNote.git.title') : t('json.git.title')}
      open={open}
      width={920}
      onClose={onClose}
      footer={<button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>}
    >
      <div className="git-panel">
        <header className="git-panel__status">
          <div>
            <GitBranch size={15} />
            <strong>{status?.repository ? t('json.git.branch', { branch: status.branch }) : t('json.git.noRepo')}</strong>
            {status?.repository && <span>{t('json.git.sync', { ahead: String(status.ahead), behind: String(status.behind) })}</span>}
          </div>
          <div className="git-panel__actions">
            <button type="button" disabled={state.busy} onClick={() => { void load() }}><RefreshCw size={13} />{t('json.git.refresh')}</button>
            {!status?.repository && <button type="button" disabled={state.busy || status?.available === false} onClick={() => { void runAction('init') }}><GitBranch size={13} />{t('json.git.init')}</button>}
            {status?.repository && <button type="button" disabled={state.busy || !status.remote} onClick={() => { void runAction('fetch') }}><CloudDownload size={13} />{t('json.git.fetch')}</button>}
            {status?.repository && <button type="button" disabled={state.busy || !status.remote || status.merging} onClick={() => { void runAction('pull') }}>{t('json.git.pull')}</button>}
            {status?.repository && <button type="button" disabled={state.busy || !status.remote || status.merging} onClick={() => { void runAction('push') }}><CloudUpload size={13} />{t('json.git.push')}</button>}
            {status?.repository && (status.merging || status.conflicts > 0) && <button className="git-danger-button" type="button" disabled={state.busy} onClick={() => {
              void desktopDialog.confirm(t('json.git.confirmAbort'), { confirmLabel: t('json.git.abortMerge'), danger: true }).then((confirmed) => { if (confirmed) return runAction('abort-merge') })
            }}><GitMerge size={13} />{t('json.git.abortMerge')}</button>}
            {status?.repository && status.merging && status.conflicts === 0 && <button type="button" disabled={state.busy} onClick={() => { void runAction('continue-operation') }}><GitMerge size={13} />{t('json.git.continueOperation')}</button>}
          </div>
        </header>

        {status?.available === false ? <div className="git-panel__empty">{t('json.git.unavailable')}</div> : (
          <>
            <div className="git-remote-row">
              <label htmlFor="git-remote">{t('json.git.remote')}</label>
              <input id="git-remote" value={state.remote} placeholder={t('json.git.remotePlaceholder')} onChange={(event) => update({ remote: event.target.value })} />
              <button type="button" disabled={state.busy || !status?.repository || (!state.remote.trim() && !status.remote)} onClick={() => { void runAction('configure-remote', { remote: state.remote }) }}>{state.remote.trim() ? t('json.git.saveRemote') : t('json.git.removeRemote')}</button>
            </div>

            <div className="git-workspace">
              <div className="git-browser">
                <div className="git-tabs" role="tablist">
                  <button className={state.tab === 'changes' ? 'git-tab git-tab--active' : 'git-tab'} type="button" role="tab" aria-selected={state.tab === 'changes'} onClick={() => update({ tab: 'changes', selected: '', diff: null, diffFile: '' })}>{t('json.git.changes')} {status?.changes.length ?? 0}</button>
                  <button className={state.tab === 'history' ? 'git-tab git-tab--active' : 'git-tab'} type="button" role="tab" aria-selected={state.tab === 'history'} onClick={() => update({ tab: 'history', selected: '', diff: null, diffFile: '' })}>{t('json.git.history')}</button>
                </div>
                <div className="git-list">
                  {state.tab === 'changes' ? (
                    status?.changes.length ? status.changes.map((change) => (
                      <button className={state.selected === change.path ? 'git-list-item git-list-item--selected' : 'git-list-item'} type="button" key={`${change.status}-${change.path}`} onClick={() => { void showWorkingDiff(change.path) }}>
                        <code>{change.status}</code><span>{change.path}</span>{change.conflict && <em>{t('json.git.conflict')}</em>}
                      </button>
                    )) : <div className="git-list-empty">{t('json.git.emptyChanges')}</div>
                  ) : (
                    state.history.length ? state.history.map((commit) => (
                      <button className={state.selected === commit.hash ? 'git-list-item git-list-item--selected' : 'git-list-item'} type="button" key={commit.hash} onClick={() => { void showCommitDiff(commit) }}>
                        <code>{commit.shortHash}</code><span><strong>{commit.message}</strong><small>{commit.author} · {new Date(commit.date).toLocaleString()}</small></span>
                      </button>
                    )) : <div className="git-list-empty">{t('json.git.emptyHistory')}</div>
                  )}
                </div>
                {status?.repository && state.tab === 'changes' && selectedChange && (
                  <div className="git-change-actions">
                    <button type="button" disabled={state.busy} onClick={() => {
                      void desktopDialog.confirm(t('json.git.confirmDiscard', { path: selectedChange.path }), { confirmLabel: t('json.git.discard'), danger: true }).then((confirmed) => { if (confirmed) return runAction('discard', { path: selectedChange.path }) })
                    }}><Undo2 size={13} />{t('json.git.discard')}</button>
                    {selectedChange.conflict && <>
                      <button type="button" disabled={state.busy} onClick={() => { void runAction('resolve-conflict', { path: selectedChange.path, strategy: 'ours' }) }}><ShieldCheck size={13} />{t('json.git.useOurs')}</button>
                      <button type="button" disabled={state.busy} onClick={() => { void runAction('resolve-conflict', { path: selectedChange.path, strategy: 'theirs' }) }}><ShieldCheck size={13} />{t('json.git.useTheirs')}</button>
                    </>}
                  </div>
                )}
                {status?.repository && state.tab === 'changes' && (
                  <div className="git-commit-row">
                    <label htmlFor="git-message">{t('json.git.commitMessage')}</label>
                    <input id="git-message" value={state.commitMessage} onChange={(event) => update({ commitMessage: event.target.value })} />
                    <button type="button" disabled={state.busy || status.merging || status.conflicts > 0 || !status.changes.length || !state.commitMessage.trim()} onClick={() => { void runAction('commit', { message: state.commitMessage }) }}><GitCommitHorizontal size={13} />{t('json.git.commit')}</button>
                  </div>
                )}
              </div>
              <VaultGitDiffView
                result={state.diff}
                selectedPath={state.diffFile}
                onSelectPath={(diffFile) => update({ diffFile })}
              />
            </div>
          </>
        )}
      </div>
    </Dialog>
  )
}

type VaultGitDiffViewProps = {
  result: VaultGitDiffResult | null
  selectedPath: string
  onSelectPath: (path: string) => void
}

function VaultGitDiffView({ result, selectedPath, onSelectPath }: VaultGitDiffViewProps) {
  const { t } = useI18n()
  const files = result?.files ?? []
  const file = files.find((item) => item.path === selectedPath) ?? files[0]
  const comparison = useMemo(
    () => file?.preview === 'text' ? compareText(file.before, file.after, false) : null,
    [file]
  )
  const leftEditorRef = useRef<TextCodeEditorHandle>(null)
  const rightEditorRef = useRef<TextCodeEditorHandle>(null)
  const syncingScroll = useRef(false)
  const language = resolveTextCodeEditorLanguage(file?.path.split('.').pop())

  function syncEditorScroll(scroll: TextCodeEditorScroll, target: TextCodeEditorHandle | null): void {
    if (!target || syncingScroll.current) return
    syncingScroll.current = true
    target.syncScroll(scroll.scrollTop, scroll.scrollLeft)
    window.requestAnimationFrame(() => { syncingScroll.current = false })
  }

  return (
    <section className="git-diff">
      <header className="git-diff__header">
        <h3>{t('json.git.diff')}</h3>
        {files.length > 1 ? (
          <select
            aria-label={t('json.git.diffFile')}
            value={file?.path ?? ''}
            onChange={(event) => onSelectPath(event.target.value)}
          >
            {files.map((item) => <option value={item.path} key={`${item.status}-${item.originalPath ?? ''}-${item.path}`}>{diffFileLabel(item)}</option>)}
          </select>
        ) : file ? <code title={diffFileLabel(file)}>{diffFileLabel(file)}</code> : null}
      </header>
      {!file ? (
        <div className="git-diff__empty">{t('json.git.noDiff')}</div>
      ) : file.preview !== 'text' ? (
        <div className="git-diff__empty">
          {file.preview === 'binary' ? t('json.git.diffBinary') : t('json.git.diffTooLarge')}
        </div>
      ) : (
        <div className="git-diff__comparison">
          <div className="git-diff__pane">
            <span>{t('json.git.diffBefore')}</span>
            <TextCodeEditor
              ref={leftEditorRef}
              ariaLabel={t('json.git.diffBefore')}
              className="diff-editor git-diff-editor"
              decorations={createDiffDecorations(file.before, comparison?.segments ?? [], 'left')}
              language={language}
              readOnly
              value={file.before}
              wrap={false}
              onScroll={(scroll) => syncEditorScroll(scroll, rightEditorRef.current)}
            />
          </div>
          <div className="git-diff__pane">
            <span>{t('json.git.diffAfter')}</span>
            <TextCodeEditor
              ref={rightEditorRef}
              ariaLabel={t('json.git.diffAfter')}
              className="diff-editor git-diff-editor"
              decorations={createDiffDecorations(file.after, comparison?.segments ?? [], 'right')}
              language={language}
              readOnly
              value={file.after}
              wrap={false}
              onScroll={(scroll) => syncEditorScroll(scroll, leftEditorRef.current)}
            />
          </div>
        </div>
      )}
    </section>
  )
}

function diffFileLabel(file: VaultGitDiffFile): string {
  const path = file.originalPath ? `${file.originalPath} → ${file.path}` : file.path
  return `${file.status.trim() || 'M'}  ${path}`
}

function createDiffDecorations(
  text: string,
  segments: DiffSegment[],
  side: 'left' | 'right'
): TextCodeEditorDecoration[] {
  const decorations: TextCodeEditorDecoration[] = []
  for (const segment of segments) {
    const from = side === 'left' ? segment.leftStart : segment.rightStart
    const to = side === 'left' ? segment.leftEnd : segment.rightEnd
    if (from < 0 || to <= from) continue
    const name = segment.type === 'insert' ? 'added' : segment.type === 'delete' ? 'removed' : 'changed'
    decorations.push({
      type: 'line',
      from: lineStartAt(text, from),
      className: `cm-diff-line-${name}`
    })
    decorations.push({
      type: 'mark',
      from,
      to,
      className: `cm-diff-character-${name}`
    })
  }
  return decorations
}

function lineStartAt(text: string, offset: number): number {
  return text.lastIndexOf('\n', Math.max(0, offset - 1)) + 1
}
