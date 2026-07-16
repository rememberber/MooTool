import { ClipboardCopy, Download, RefreshCw, Search } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import type { EnvironmentEntry, EnvironmentSnapshot } from '@/shared/contracts/system'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'

type VariableTab = 'environment' | 'runtime'

export function VariablesTool() {
  const { t } = useI18n()
  const actions = useToolActions('variables')
  const [tab, setTab] = useState<VariableTab>('environment')
  const [snapshot, setSnapshot] = useState<EnvironmentSnapshot>({ environment: [], runtime: [] })
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(false)
  const entries = useMemo(() => snapshot[tab].filter((entry) => !query || `${entry.key}\n${entry.value}`.toLowerCase().includes(query.toLowerCase())), [query, snapshot, tab])

  async function refresh(): Promise<void> {
    setLoading(true)
    try { setSnapshot(await window.mootool.getEnvironmentSnapshot()) } catch (error) { actions.reportError(error) } finally { setLoading(false) }
  }

  useEffect(() => { void refresh() }, [])

  async function exportValues(): Promise<void> {
    const content = formatEnvironment(snapshot.environment, snapshot.runtime)
    try { await window.mootool.saveTextFile({ kind: 'text', defaultName: 'mootool-environment.txt', content }) } catch (error) { actions.reportError(error) }
  }

  return (
    <section className="tool-page p5-tool variables-tool-page">
      <ToolPageHeader title={t('variables.title')} actions={<><button className="toolbar-button" type="button" disabled={loading} onClick={() => { void refresh() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}</button><button className="toolbar-button" type="button" onClick={() => { void exportValues() }}><Download size={14} />{t('common.export')}</button></>} />
      <div className="local-tool-shell variables-workspace">
        <header><ToolTabs tabs={(['environment', 'runtime'] as VariableTab[]).map((id) => ({ id, label: t(`variables.tab.${id}` as 'variables.tab.environment') }))} active={tab} onChange={setTab} /><div className="compact-search"><Search size={13} /><input value={query} placeholder={t('common.search')} aria-label={t('common.search')} onChange={(event) => setQuery(event.target.value)} /></div></header>
        <EnvironmentTable entries={entries} onCopy={(entry) => { void actions.copy(`${entry.key}=${entry.value}`) }} />
        <footer>{t('variables.count', { count: String(entries.length), total: String(snapshot[tab].length) })}</footer>
      </div>
    </section>
  )
}

function EnvironmentTable({ entries, onCopy }: { entries: EnvironmentEntry[]; onCopy: (entry: EnvironmentEntry) => void }) {
  const { t } = useI18n()
  return <div className="environment-table-wrap"><table className="environment-table"><thead><tr><th>{t('variables.key')}</th><th>{t('variables.value')}</th><th /></tr></thead><tbody>{entries.map((entry) => <tr key={entry.key}><td><code>{entry.key}</code></td><td>{entry.value}</td><td><button className="icon-button" type="button" aria-label={t('common.action.copy')} onClick={() => onCopy(entry)}><ClipboardCopy size={13} /></button></td></tr>)}</tbody></table></div>
}

function formatEnvironment(environment: EnvironmentEntry[], runtime: EnvironmentEntry[]): string {
  return ['------------System.getenv---------------', ...environment.map((entry) => `${entry.key}=${entry.value}`), '', '------------Electron runtime---------------', ...runtime.map((entry) => `${entry.key}=${entry.value}`)].join('\n')
}
