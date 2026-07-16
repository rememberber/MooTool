import { ClipboardPaste, History, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { parseUserAgent, uaPresets, type UaResult } from './uaTools'

export function UaParseTool() {
  const { t } = useI18n()
  const actions = useToolActions('uaParse')
  const [source, setSource] = useState<string>(uaPresets[0][1])
  const [result, setResult] = useState<UaResult | null>(() => parseUserAgent(uaPresets[0][1]))
  const [historyOpen, setHistoryOpen] = useState(false)

  function parse(): void {
    try {
      const next = parseUserAgent(source)
      setResult(next)
      void actions.saveHistory(t('ua.title'), source, JSON.stringify(next), 'UaParse')
    } catch {
      actions.toast.error(t('ua.empty'))
    }
  }

  async function paste(): Promise<void> {
    try {
      setSource(await navigator.clipboard.readText())
    } catch (error) {
      actions.reportError(error)
    }
  }

  const rows = result ? [
    [t('ua.browser'), result.browser], [t('ua.browserVersion'), result.browserVersion],
    [t('ua.engine'), result.engine], [t('ua.engineVersion'), result.engineVersion],
    [t('ua.os'), result.os], [t('ua.osVersion'), result.osVersion],
    [t('ua.deviceType'), result.deviceType], [t('ua.deviceBrand'), result.deviceBrand],
    [t('ua.deviceModel'), result.deviceModel], [t('ua.mobile'), result.mobile ? t('common.yes') : t('common.no')],
    [t('ua.bot'), result.bot ? t('common.yes') : t('common.no')]
  ] : []

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('ua.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <ResizableColumns className="local-tool-shell ua-workspace" columns={2} defaultSizes={[0.9, 1.1]} minPaneWidths={[300, 300]} storageKey="ua-parser">
        <section className="ua-input-panel">
          <label htmlFor="ua-source">{t('ua.input')}</label>
          <textarea id="ua-source" value={source} spellCheck={false} onChange={(event) => setSource(event.target.value)} />
          <div className="ua-actions">
            <select aria-label={t('ua.preset')} defaultValue="" onChange={(event) => { if (event.target.value) setSource(event.target.value) }}><option value="">{t('ua.preset')}</option>{uaPresets.map(([name, value]) => <option value={value} key={name}>{name}</option>)}</select>
            <button className="panel-command" type="button" onClick={() => { void paste() }}><ClipboardPaste size={14} />{t('common.action.paste')}</button>
            <button className="panel-command" type="button" onClick={() => { setSource(''); setResult(null) }}><Trash2 size={14} />{t('common.action.clear')}</button>
            <button className="primary-command" type="button" onClick={parse}>{t('ua.parse')}</button>
          </div>
        </section>
        <section className="ua-result-panel" aria-label={t('common.result')}>
          {rows.map(([label, value]) => <div className="ua-result-row" key={label}><span>{label}</span><strong>{value || t('ua.unknown')}</strong></div>)}
        </section>
      </ResizableColumns>
      <HistoryDialog funcType="uaParse" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => { try { setResult(JSON.parse(value) as UaResult) } catch { setSource(value) } }} onApplyRecord={(record) => { setSource(record.inputText); try { setResult(JSON.parse(record.outputText) as UaResult) } catch { setResult(null) } }} />
    </section>
  )
}
