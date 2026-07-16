import { CloudDownload, CloudUpload, GitBranch, GitCommitHorizontal, GitMerge, RefreshCw, ShieldCheck, Undo2 } from 'lucide-react'
import { useCallback, useEffect, useReducer } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import type { VaultGitAction, VaultGitCommit, VaultGitStatus } from '@/shared/contracts/vaultGit'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

type VaultGitDialogProps = {
  open: boolean
  onClose: () => void
  onVaultChange: () => void
  scope?: 'json' | 'quickNote'
}

type GitPanelState = {
  status: VaultGitStatus | null
  history: VaultGitCommit[]
  tab: 'changes' | 'history'
  diff: string
  selected: string
  remote: string
  commitMessage: string
  busy: boolean
}

function updateState(state: GitPanelState, patch: Partial<GitPanelState>): GitPanelState {
  return { ...state, ...patch }
}

export function VaultGitDialog({ open, onClose, onVaultChange, scope = 'json' }: VaultGitDialogProps) {
  const { t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const toast = useToast()
  const [state, update] = useReducer(updateState, {
    status: null,
    history: [],
    tab: 'changes',
    diff: '',
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
      const result = scope === 'quickNote'
        ? await window.mootool.runQuickNoteGitAction({ action, ...extra })
        : await window.mootool.runVaultGitAction({ action, ...extra })
      if (!result.success) {
        toast.error(result.message)
        update({ busy: false })
        return
      }
      if (action === 'configure-remote') await updateSettings({ vault: { gitRemote: state.remote } })
      toast.success(t('json.git.done'))
      if (action === 'discard' || action === 'abort-merge' || action === 'resolve-conflict') {
        update({ selected: '', diff: '' })
      }
      await load()
      if (action === 'pull' || action === 'discard' || action === 'abort-merge' || action === 'resolve-conflict') onVaultChange()
    } catch (error) {
      update({ busy: false })
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }

  async function showWorkingDiff(path: string): Promise<void> {
    update({ selected: path, diff: '', busy: true })
    try {
      update({ diff: await (scope === 'quickNote' ? window.mootool.getQuickNoteGitDiff({ path }) : window.mootool.getVaultGitDiff({ path })), busy: false })
    } catch (error) {
      update({ busy: false })
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }

  async function showCommitDiff(commit: VaultGitCommit): Promise<void> {
    update({ selected: commit.hash, diff: '', busy: true })
    try {
      update({ diff: await (scope === 'quickNote' ? window.mootool.getQuickNoteGitDiff({ commit: commit.hash }) : window.mootool.getVaultGitDiff({ commit: commit.hash })), busy: false })
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
            {status?.repository && <button type="button" disabled={state.busy || !status.remote} onClick={() => { void runAction('pull') }}>{t('json.git.pull')}</button>}
            {status?.repository && <button type="button" disabled={state.busy || !status.remote} onClick={() => { void runAction('push') }}><CloudUpload size={13} />{t('json.git.push')}</button>}
            {status?.repository && (status.merging || status.conflicts > 0) && <button className="git-danger-button" type="button" disabled={state.busy} onClick={() => {
              if (window.confirm(t('json.git.confirmAbort'))) void runAction('abort-merge')
            }}><GitMerge size={13} />{t('json.git.abortMerge')}</button>}
          </div>
        </header>

        {status?.available === false ? <div className="git-panel__empty">{t('json.git.unavailable')}</div> : (
          <>
            <div className="git-remote-row">
              <label htmlFor="git-remote">{t('json.git.remote')}</label>
              <input id="git-remote" value={state.remote} placeholder={t('json.git.remotePlaceholder')} onChange={(event) => update({ remote: event.target.value })} />
              <button type="button" disabled={state.busy || !status?.repository || !state.remote.trim()} onClick={() => { void runAction('configure-remote', { remote: state.remote }) }}>{t('json.git.saveRemote')}</button>
            </div>

            <div className="git-workspace">
              <div className="git-browser">
                <div className="git-tabs" role="tablist">
                  <button className={state.tab === 'changes' ? 'git-tab git-tab--active' : 'git-tab'} type="button" role="tab" aria-selected={state.tab === 'changes'} onClick={() => update({ tab: 'changes', selected: '', diff: '' })}>{t('json.git.changes')} {status?.changes.length ?? 0}</button>
                  <button className={state.tab === 'history' ? 'git-tab git-tab--active' : 'git-tab'} type="button" role="tab" aria-selected={state.tab === 'history'} onClick={() => update({ tab: 'history', selected: '', diff: '' })}>{t('json.git.history')}</button>
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
                      if (window.confirm(t('json.git.confirmDiscard', { path: selectedChange.path }))) {
                        void runAction('discard', { path: selectedChange.path })
                      }
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
                    <button type="button" disabled={state.busy || !status.changes.length || !state.commitMessage.trim()} onClick={() => { void runAction('commit', { message: state.commitMessage }) }}><GitCommitHorizontal size={13} />{t('json.git.commit')}</button>
                  </div>
                )}
              </div>
              <section className="git-diff">
                <h3>{t('json.git.diff')}</h3>
                <pre>{state.diff || t('json.git.noDiff')}</pre>
              </section>
            </div>
          </>
        )}
      </div>
    </Dialog>
  )
}
