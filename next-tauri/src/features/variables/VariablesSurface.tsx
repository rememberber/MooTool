import { Download, Pencil, Plus, RefreshCw, Search, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { invoke } from '@tauri-apps/api/core'
import { useLocalizedMessages } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { formatEnvironment, parseEnvironmentContent, updateEnvironmentContent, type EnvironmentEntry } from './environmentFile'
import { variablesMessages } from './variablesMessages'

type VariableTab = 'environment' | 'runtime'
type DisplayScope = 'process' | 'user' | 'system'
type PersistentScope = 'user' | 'system'

const previewKey = (scope: PersistentScope) => `mootool-next-tauri:environment-${scope}`

export function VariablesSurface() {
  const dialog = useDesktopDialog()
  const { t } = useLocalizedMessages(variablesMessages)
  const [tab, setTab] = useState<VariableTab>('environment')
  const [scope, setScope] = useState<DisplayScope>('process')
  const [targetScope, setTargetScope] = useState<PersistentScope>('user')
  const [processEntries, setProcessEntries] = useState<EnvironmentEntry[]>([])
  const [userContent, setUserContent] = useState('')
  const [systemContent, setSystemContent] = useState('')
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState<EnvironmentEntry | null | undefined>(undefined)
  const [keyValue, setKeyValue] = useState('')
  const [value, setValue] = useState('')
  const [notice, setNotice] = useState('')
  const [failed, setFailed] = useState(false)
  const runtimeEntries = useMemo(() => runtimeProperties(), [])
  const userEntries = useMemo(() => parseEnvironmentContent(userContent), [userContent])
  const systemEntries = useMemo(() => parseEnvironmentContent(systemContent), [systemContent])
  const sourceEntries = tab === 'runtime'
    ? runtimeEntries
    : scope === 'process' ? processEntries : scope === 'user' ? userEntries : systemEntries
  const entries = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle ? sourceEntries.filter((entry) => `${entry.key}\n${entry.value}`.toLowerCase().includes(needle)) : sourceEntries
  }, [query, sourceEntries])
  const canEdit = tab === 'environment'
  const canDelete = tab === 'environment' && scope !== 'process'
  const session = useMemo(() => ({
    digest: JSON.stringify({ tab, scope, count: sourceEntries.length, query: query.length }),
    summary: t('count.line', { count: entries.length, total: sourceEntries.length })
  }), [entries.length, query.length, scope, sourceEntries.length, t, tab])
  const { sessionId, reportError } = useToolSessionReport('variables', session.digest, session.summary, !loading)

  useEffect(() => { void refresh() }, [])

  async function refresh(): Promise<void> {
    setLoading(true)
    try {
      const [processVariables, userFile, systemFile] = await Promise.all([
        diagnosticsApi.environment(true),
        readPersistent('user'),
        readPersistent('system')
      ])
      setProcessEntries(processVariables.map((item) => ({ key: item.name, value: item.value })).sort((left, right) => left.key.localeCompare(right.key)))
      setUserContent(userFile)
      setSystemContent(systemFile)
      setFailed(false)
    } catch (cause) {
      setNotice(cause instanceof Error ? cause.message : String(cause))
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }

  function openEditor(entry?: EnvironmentEntry): void {
    setEditing(entry ?? null)
    setKeyValue(entry?.key ?? '')
    setValue(entry?.value ?? '')
    setTargetScope(scope === 'process' ? 'user' : scope)
  }

  async function saveVariable(): Promise<void> {
    const destination: PersistentScope = scope === 'process' ? targetScope : scope
    setSaving(true)
    try {
      const current = destination === 'user' ? userContent : systemContent
      const next = updateEnvironmentContent(current, keyValue.trim(), value, shellExport(destination))
      await writePersistent(destination, next)
      const nextUser = destination === 'user' ? parseEnvironmentContent(next) : userEntries
      const nextSystem = destination === 'system' ? parseEnvironmentContent(next) : systemEntries
      if (destination === 'user') setUserContent(next)
      else setSystemContent(next)
      syncProcessEntry(keyValue.trim(), nextUser, nextSystem, setProcessEntries)
      setEditing(undefined)
      setNotice(t('notice.saved'))
      setFailed(false)
    } catch (cause) {
      setNotice(cause instanceof Error ? cause.message : String(cause))
      setFailed(true)
    } finally {
      setSaving(false)
    }
  }

  async function deleteVariable(entry: EnvironmentEntry): Promise<void> {
    if (scope === 'process') return
    if (!await dialog.confirm(t('confirm.remove', { key: entry.key }), { dangerous: true })) return
    setSaving(true)
    try {
      const current = scope === 'user' ? userContent : systemContent
      const next = updateEnvironmentContent(current, entry.key, undefined, shellExport(scope))
      await writePersistent(scope, next)
      const nextUser = scope === 'user' ? parseEnvironmentContent(next) : userEntries
      const nextSystem = scope === 'system' ? parseEnvironmentContent(next) : systemEntries
      if (scope === 'user') setUserContent(next)
      else setSystemContent(next)
      syncProcessEntry(entry.key, nextUser, nextSystem, setProcessEntries)
      setNotice(t('notice.removed'))
      setFailed(false)
    } catch (cause) {
      setNotice(cause instanceof Error ? cause.message : String(cause))
      setFailed(true)
    } finally {
      setSaving(false)
    }
  }

  async function exportValues(): Promise<void> {
    const content = formatEnvironment({
      user: userEntries,
      system: systemEntries,
      environment: processEntries,
      runtime: runtimeEntries
    })
    try {
      const path = await userFilesApi.exportText('mootool-environment.txt', content)
      if (path) setNotice(t('notice.exported', { path }))
    } catch (cause) {
      setNotice(cause instanceof Error ? cause.message : String(cause))
      setFailed(true)
    }
  }

  async function copy(entry: EnvironmentEntry): Promise<void> {
    try {
      await clipboardApi.writeText(`${entry.key}=${entry.value}`)
      setNotice(t('notice.copied', { name: entry.key }))
      setFailed(false)
    } catch {
      setNotice(t('error.clipboard'))
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench variables-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <div className="variables-toolbar">
        <div className="variables-tabs utility-segments" role="tablist">
          {(['environment', 'runtime'] as const).map((id) => (
            <button key={id} type="button" role="tab" aria-selected={tab === id} className={tab === id ? 'utility-segment utility-segment--active' : 'utility-segment'} onClick={() => setTab(id)}>{t(`tab.${id}`)}</button>
          ))}
        </div>
        {tab === 'environment' && (
          <label>{t('scope.label')}
            <select aria-label={t('scope.label')} value={scope} onChange={(event) => setScope(event.target.value as DisplayScope)}>
              <option value="user">{t('scope.user')}</option>
              <option value="system">{t('scope.system')}</option>
              <option value="process">{t('scope.current')}</option>
            </select>
          </label>
        )}
        <label><Search /><input aria-label={t('search.placeholder')} value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} /></label>
        {canEdit && <button type="button" disabled={saving} onClick={() => openEditor()}><Plus />{t('action.add')}</button>}
        <button type="button" disabled={loading || saving} onClick={() => void refresh()}><RefreshCw />{t('action.refresh')}</button>
        <button type="button" onClick={() => void exportValues()}><Download />{t('action.export')}</button>
      </div>
      <section className="variables-table">
        <header><span>{t('field.key')}</span><span>{t('field.value')}</span><span>{t('column.action')}</span></header>
        <div>
          {entries.map((entry) => (
            <article key={entry.key} onDoubleClick={() => { if (canEdit) openEditor(entry) }}>
              <strong><code>{entry.key}</code></strong>
              <code title={entry.value}>{entry.value}</code>
              <div className="variables-actions">
                <button type="button" aria-label={t('action.copy')} onClick={() => void copy(entry)}>{t('action.copy')}</button>
                {canEdit && <button type="button" aria-label={t('action.edit')} onClick={() => openEditor(entry)}><Pencil /></button>}
                {canDelete && <button type="button" aria-label={t('action.delete')} onClick={() => void deleteVariable(entry)}><Trash2 /></button>}
              </div>
            </article>
          ))}
          {!entries.length && <p>{t(canEdit ? 'empty.none' : 'empty')}</p>}
        </div>
      </section>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{t('count.line', { count: entries.length, total: sourceEntries.length })}</span>
        {tab === 'environment' && <span>{scopeHint(scope, t)}</span>}
        <span>{notice}</span>
      </footer>
      {editing !== undefined && (
        <form className="desktop-dialog-backdrop variables-editor" onSubmit={(event) => { event.preventDefault(); void saveVariable() }}>
          <div className="desktop-dialog">
            <h2>{t(editing ? 'editor.editTitle' : 'editor.addTitle')}</h2>
            <label>{t('field.key')}<input autoFocus readOnly={Boolean(editing)} value={keyValue} placeholder="JAVA_HOME" onChange={(event) => setKeyValue(event.target.value)} /></label>
            <label>{t('field.value')}<textarea value={value} onChange={(event) => setValue(event.target.value)} /></label>
            {scope === 'process' && (
              <label>{t('targetScope')}
                <select value={targetScope} onChange={(event) => setTargetScope(event.target.value as PersistentScope)}>
                  <option value="user">{t('scope.user')}</option>
                  <option value="system">{t('scope.system')}</option>
                </select>
              </label>
            )}
            <p>{t('applyHint')}</p>
            <footer>
              <button type="button" disabled={saving} onClick={() => setEditing(undefined)}>{t('action.cancel')}</button>
              <button type="submit" disabled={saving || !keyValue.trim()}>{t('action.save')}</button>
            </footer>
          </div>
        </form>
      )}
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function runtimeProperties(): EnvironmentEntry[] {
  const locale = Intl.DateTimeFormat().resolvedOptions()
  return [
    ['process.version', ''],
    ['process.versions.electron', ''],
    ['process.versions.chrome', ''],
    ['process.versions.node', ''],
    ['process.execPath', ''],
    ['process.platform', navigator.platform],
    ['process.arch', navigator.userAgent.includes('arm64') || navigator.userAgent.includes('aarch64') ? 'arm64' : 'unknown'],
    ['process.cwd', ''],
    ['process.locale', locale.locale],
    ['process.timeZone', locale.timeZone ?? '']
  ].map(([key, value]) => ({ key, value })).sort((left, right) => left.key.localeCompare(right.key))
}

function syncProcessEntry(
  key: string,
  user: EnvironmentEntry[],
  system: EnvironmentEntry[],
  setEntries: (update: (current: EnvironmentEntry[]) => EnvironmentEntry[]) => void
): void {
  const effective = user.find((entry) => entry.key === key) ?? system.find((entry) => entry.key === key)
  setEntries((current) => {
    const next = current.filter((entry) => entry.key !== key)
    if (effective) next.push(effective)
    return next.sort((left, right) => left.key.localeCompare(right.key))
  })
}

function scopeHint(scope: DisplayScope, t: (key: 'scopeHint.user' | 'scopeHint.system' | 'scopeHint.process') => string): string {
  if (scope === 'user') return t('scopeHint.user')
  if (scope === 'system') return t('scopeHint.system')
  return t('scopeHint.process')
}

function shellExport(scope: PersistentScope): boolean {
  return scope === 'user' || /Mac|iPhone|iPad/.test(navigator.platform + navigator.userAgent)
}

async function readPersistent(scope: PersistentScope): Promise<string> {
  if (window.__TAURI_INTERNALS__) return invoke<string>('read_persistent_environment', { scope })
  return localStorage.getItem(previewKey(scope)) ?? ''
}

async function writePersistent(scope: PersistentScope, content: string): Promise<void> {
  if (window.__TAURI_INTERNALS__) {
    await invoke('write_persistent_environment', { scope, content })
    return
  }
  localStorage.setItem(previewKey(scope), content)
}
