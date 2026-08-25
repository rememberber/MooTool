import { ClipboardCopy, Download, Pencil, Plus, RefreshCw, Search, Trash2 } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import type { EnvironmentEntry, EnvironmentScope, EnvironmentSnapshot } from '@/shared/contracts/system'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

type VariableTab = 'environment' | 'runtime'
type DisplayScope = 'process' | EnvironmentScope

const emptySnapshot: EnvironmentSnapshot = { environment: [], runtime: [], user: [], system: [] }

export function VariablesTool() {
  const { t } = useI18n()
  const actions = useToolActions('variables')
  const desktopDialog = useDesktopDialog()
  const [tab, setTab] = useState<VariableTab>('environment')
  const [scope, setScope] = useState<DisplayScope>('process')
  const [targetScope, setTargetScope] = useState<EnvironmentScope>('user')
  const [snapshot, setSnapshot] = useState<EnvironmentSnapshot>(emptySnapshot)
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState<EnvironmentEntry | null | undefined>(undefined)
  const [keyValue, setKeyValue] = useState('')
  const [value, setValue] = useState('')
  const sourceEntries = tab === 'runtime'
    ? snapshot.runtime
    : scope === 'process' ? snapshot.environment : snapshot[scope]
  const entries = useMemo(
    () => sourceEntries.filter((entry) => !query || `${entry.key}\n${entry.value}`.toLowerCase().includes(query.toLowerCase())),
    [query, sourceEntries]
  )
  const canEdit = tab === 'environment'
  const canDelete = tab === 'environment' && scope !== 'process'

  async function refresh(): Promise<void> {
    setLoading(true)
    try {
      setSnapshot(await window.mootool.getEnvironmentSnapshot())
    } catch (error) {
      actions.reportError(error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void refresh() }, [])

  async function exportValues(): Promise<void> {
    const content = formatEnvironment(snapshot)
    try {
      await window.mootool.saveTextFile({ kind: 'text', defaultName: 'mootool-environment.txt', content })
    } catch (error) {
      actions.reportError(error)
    }
  }

  function openEditor(entry?: EnvironmentEntry): void {
    setEditing(entry ?? null)
    setKeyValue(entry?.key ?? '')
    setValue(entry?.value ?? '')
    setTargetScope(scope === 'process' ? 'user' : scope)
  }

  async function saveVariable(): Promise<void> {
    setSaving(true)
    try {
      await window.mootool.setEnvironmentVariable({ scope: targetScope, key: keyValue.trim(), value })
      setEditing(undefined)
      await refresh()
      actions.toast.success(t('variables.saved'))
    } catch (error) {
      actions.reportError(error)
    } finally {
      setSaving(false)
    }
  }

  async function deleteVariable(entry: EnvironmentEntry): Promise<void> {
    if (scope === 'process' || !await desktopDialog.confirm(t('variables.confirmDelete', { key: entry.key }), { confirmLabel: t('common.action.delete'), danger: true })) return
    setSaving(true)
    try {
      await window.mootool.deleteEnvironmentVariable({ scope, key: entry.key })
      await refresh()
      actions.toast.success(t('variables.deleted'))
    } catch (error) {
      actions.reportError(error)
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="tool-page p5-tool variables-tool-page">
      <ToolPageHeader
        title={t('variables.title')}
        actions={<>
          {tab === 'environment' && <button className="toolbar-button" type="button" disabled={saving} onClick={() => openEditor()}><Plus size={14} />{t('variables.add')}</button>}
          <button className="toolbar-button" type="button" disabled={loading || saving} onClick={() => { void refresh() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button>
          <button className="toolbar-button" type="button" onClick={() => { void exportValues() }}><Download size={14} />{t('common.export')}</button>
        </>}
      />
      <div className="local-tool-shell variables-workspace">
        <header>
          <ToolTabs tabs={(['environment', 'runtime'] as VariableTab[]).map((id) => ({ id, label: t(`variables.tab.${id}` as 'variables.tab.environment') }))} active={tab} onChange={setTab} />
          {tab === 'environment' && (
            <label className="environment-scope">
              <span>{t('variables.scope')}</span>
              <select value={scope} onChange={(event) => setScope(event.target.value as DisplayScope)}>
                <option value="user">{t('variables.scope.user')}</option>
                <option value="system">{t('variables.scope.system')}</option>
                <option value="process">{t('variables.scope.process')}</option>
              </select>
            </label>
          )}
          <div className="compact-search"><Search size={13} /><input value={query} placeholder={t('common.search')} aria-label={t('common.search')} onChange={(event) => setQuery(event.target.value)} /></div>
        </header>
        <EnvironmentTable
          entries={entries}
          canEdit={canEdit}
          canDelete={canDelete}
          onCopy={(entry) => { void actions.copy(`${entry.key}=${entry.value}`) }}
          onEdit={openEditor}
          onDelete={(entry) => { void deleteVariable(entry) }}
        />
        <footer>
          <span>{t('variables.count', { count: String(entries.length), total: String(sourceEntries.length) })}</span>
          {tab === 'environment' && <span>{t(`variables.scopeHint.${scope}` as 'variables.scopeHint.user')}</span>}
        </footer>
      </div>
      <Dialog
        title={t(editing ? 'variables.editTitle' : 'variables.addTitle')}
        open={editing !== undefined}
        width={560}
        onClose={() => setEditing(undefined)}
        footer={<>
          <button className="dialog-button" type="button" disabled={saving} onClick={() => setEditing(undefined)}>{t('common.cancel')}</button>
          <button className="dialog-button dialog-button--primary" type="button" disabled={saving || !keyValue.trim()} onClick={() => { void saveVariable() }}>{t('common.save')}</button>
        </>}
      >
        <div className="environment-editor">
          <label className="dialog-field">
            <span>{t('variables.key')}</span>
            <input autoFocus readOnly={Boolean(editing)} value={keyValue} placeholder="JAVA_HOME" onChange={(event) => setKeyValue(event.target.value)} />
          </label>
          <label className="dialog-field">
            <span>{t('variables.value')}</span>
            <textarea value={value} onChange={(event) => setValue(event.target.value)} />
          </label>
          {scope === 'process' && (
            <label className="dialog-field">
              <span>{t('variables.targetScope')}</span>
              <select value={targetScope} onChange={(event) => setTargetScope(event.target.value as EnvironmentScope)}>
                <option value="user">{t('variables.scope.user')}</option>
                <option value="system">{t('variables.scope.system')}</option>
              </select>
            </label>
          )}
          <p>{t('variables.applyHint')}</p>
        </div>
      </Dialog>
    </section>
  )
}

function EnvironmentTable({ entries, canEdit, canDelete, onCopy, onEdit, onDelete }: {
  entries: EnvironmentEntry[]
  canEdit: boolean
  canDelete: boolean
  onCopy: (entry: EnvironmentEntry) => void
  onEdit: (entry: EnvironmentEntry) => void
  onDelete: (entry: EnvironmentEntry) => void
}) {
  const { t } = useI18n()
  return (
    <div className="environment-table-wrap">
      <table className="environment-table">
        <thead><tr><th>{t('variables.key')}</th><th>{t('variables.value')}</th><th /></tr></thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.key} onDoubleClick={() => { if (canEdit) onEdit(entry) }}>
              <td><code>{entry.key}</code></td>
              <td title={entry.value}>{entry.value}</td>
              <td>
                <div className="environment-row-actions">
                  <button className="icon-button" type="button" aria-label={t('common.action.copy')} onClick={() => onCopy(entry)}><ClipboardCopy size={13} /></button>
                  {canEdit && <button className="icon-button" type="button" aria-label={t('variables.edit')} onClick={() => onEdit(entry)}><Pencil size={13} /></button>}
                  {canDelete && <button className="icon-button" type="button" aria-label={t('common.action.delete')} onClick={() => onDelete(entry)}><Trash2 size={13} /></button>}
                </div>
              </td>
            </tr>
          ))}
          {!entries.length && <tr><td className="environment-empty" colSpan={3}>{t('variables.empty')}</td></tr>}
        </tbody>
      </table>
    </div>
  )
}

function formatEnvironment(snapshot: EnvironmentSnapshot): string {
  return [
    '------------Persistent user environment---------------',
    ...snapshot.user.map((entry) => `${entry.key}=${entry.value}`),
    '',
    '------------Persistent system environment---------------',
    ...snapshot.system.map((entry) => `${entry.key}=${entry.value}`),
    '',
    '------------Current process environment---------------',
    ...snapshot.environment.map((entry) => `${entry.key}=${entry.value}`),
    '',
    '------------Electron runtime---------------',
    ...snapshot.runtime.map((entry) => `${entry.key}=${entry.value}`)
  ].join('\n')
}
