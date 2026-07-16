import { ListTree, Sparkles, X } from 'lucide-react'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { JsonFormatOptions, JsonStatus } from './jsonTools'

type JsonInspectorProps = {
  formatOptions: JsonFormatOptions
  className: string
  jsonPath: string
  notice: string
  status: JsonStatus
  onFormatOptionsChange: (options: JsonFormatOptions) => void
  onAdvancedFormat: () => void
  onJsonToXml: () => void
  onXmlToJson: () => void
  onBeanToJson: () => void
  onJsonToBean: () => void
  onSwap: () => void
  onEscapeJson: () => void
  onUnescapeJson: () => void
  onEscapeText: () => void
  onUnescapeText: () => void
  onClassNameChange: (value: string) => void
  onJsonPathChange: (value: string) => void
  onQueryPath: () => void
  onOpenPathPicker: () => void
  onClose: () => void
}

export function JsonInspector(props: JsonInspectorProps) {
  const { t } = useI18n()
  const { formatOptions } = props
  return (
    <aside className="inspector-panel">
      <header className="inspector-mobile-header">
        <strong>{t('json.action.more')}</strong>
        <button type="button" aria-label={t('common.close')} onClick={props.onClose}><X size={14} /></button>
      </header>
      <section className="inspector-section">
        <h2>{t('json.panel.format')}</h2>
        <label className="option-row option-row--select">
          <span>{t('json.format.indent')}</span>
          <select value={formatOptions.spaces} onChange={(event) => props.onFormatOptionsChange({ ...formatOptions, spaces: Number(event.target.value) })}>
            <option value={2}>2</option><option value={4}>4</option>
          </select>
        </label>
        <CheckOption label={t('json.format.sortKeys')} checked={formatOptions.sortKeys} onChange={(sortKeys) => props.onFormatOptionsChange({ ...formatOptions, sortKeys })} />
        <CheckOption label={t('json.format.ignoreCase')} checked={formatOptions.ignoreCase} onChange={(ignoreCase) => props.onFormatOptionsChange({ ...formatOptions, ignoreCase })} />
        <CheckOption label={t('json.format.duplicateKeys')} checked={formatOptions.checkDuplicateKeys} onChange={(checkDuplicateKeys) => props.onFormatOptionsChange({ ...formatOptions, checkDuplicateKeys })} />
        <button className="inspector-action inspector-action--primary" type="button" onClick={props.onAdvancedFormat}><Sparkles size={14} />{t('json.format.apply')}</button>
      </section>

      <section className="inspector-section">
        <h2>{t('json.panel.convert')}</h2>
        <div className="inspector-action-grid">
          <Action label={t('json.action.jsonToXml')} onClick={props.onJsonToXml} />
          <Action label={t('json.action.xmlToJson')} onClick={props.onXmlToJson} />
          <Action label={t('json.action.beanToJson')} onClick={props.onBeanToJson} />
          <Action label={t('json.action.jsonToBean')} onClick={props.onJsonToBean} />
          <Action label={t('json.action.swap')} onClick={props.onSwap} />
          <Action label={t('json.action.escape')} onClick={props.onEscapeJson} />
          <Action label={t('json.action.unescape')} onClick={props.onUnescapeJson} />
          <Action label={t('json.action.escapeText')} onClick={props.onEscapeText} />
          <Action label={t('json.action.unescapeText')} onClick={props.onUnescapeText} />
        </div>
        <label className="class-name-field">
          <span>{t('json.dialog.className')}</span>
          <input value={props.className} onChange={(event) => props.onClassNameChange(event.target.value)} />
        </label>
      </section>

      <section className="inspector-section">
        <h2>{t('json.panel.jsonPath')}</h2>
        <input className="inspector-input" aria-label={t('json.panel.jsonPath')} placeholder={t('json.path.placeholder')} value={props.jsonPath} onChange={(event) => props.onJsonPathChange(event.target.value)} />
        <div className="inspector-inline-actions">
          <button className="inspector-action inspector-action--primary" type="button" onClick={props.onQueryPath}>{t('json.path.query')}</button>
          <button className="inspector-action" type="button" onClick={props.onOpenPathPicker}><ListTree size={14} />{t('json.path.pick')}</button>
        </div>
      </section>

      <section className="inspector-section inspector-section--result">
        <h2>{t('json.panel.result')}</h2>
        <p className={props.status.kind === 'error' ? 'result-text result-text--error' : 'result-text'}>{props.notice || props.status.message}</p>
      </section>
    </aside>
  )
}

function CheckOption({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return <label className="option-row"><input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} /><span>{label}</span></label>
}

function Action({ label, onClick }: { label: string; onClick: () => void }) {
  return <button className="inspector-action" type="button" onClick={onClick}>{label}</button>
}
