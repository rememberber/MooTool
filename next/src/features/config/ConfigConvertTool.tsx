import { ArrowLeft, ArrowRight, History, WandSparkles } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { TextPane, ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { formatYaml, propertiesToYaml, validateYaml, yamlToProperties } from './configTools'

type ConfigTab = 'convert' | 'validate'

export function ConfigConvertTool() {
  const { t } = useI18n()
  const actions = useToolActions('ymlProperties')
  const [tab, setTab] = useState<ConfigTab>('convert')
  const [properties, setProperties] = useState('server.port=8080\napp.name=MooTool\napp.locales[0]=zh-CN\napp.locales[1]=en-US')
  const [yaml, setYaml] = useState('')
  const [validateSource, setValidateSource] = useState('app:\n  name: MooTool\n  enabled: true\n')
  const [validation, setValidation] = useState('')
  const [valid, setValid] = useState<boolean | null>(null)
  const [historyOpen, setHistoryOpen] = useState(false)

  function convert(direction: 'toYaml' | 'toProperties'): void {
    try {
      const input = direction === 'toYaml' ? properties : yaml
      const output = direction === 'toYaml' ? propertiesToYaml(properties) : yamlToProperties(yaml)
      if (direction === 'toYaml') setYaml(output); else setProperties(output)
      void actions.saveHistory(t(direction === 'toYaml' ? 'config.toYaml' : 'config.toProperties'), input, output, direction)
    } catch (error) { actions.reportError(error) }
  }

  function validate(): void {
    const result = validateYaml(validateSource)
    setValid(result.valid)
    const message = result.valid ? t('config.valid') : t('config.invalid', { message: result.message })
    setValidation(message)
    void actions.saveHistory(t('config.validate'), validateSource, message, 'validate')
  }

  function format(): void {
    try {
      const output = formatYaml(validateSource)
      setValidateSource(output)
      setValid(true)
      setValidation(t('config.valid'))
      void actions.saveHistory(t('config.format'), validateSource, output, 'format')
    } catch (error) { actions.reportError(error) }
  }

  return (
    <section className="tool-page p3-tool">
      <ToolPageHeader title={t('config.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <div className="local-tool-shell config-workspace">
        <ToolTabs tabs={[{ id: 'convert', label: t('config.tab.convert') }, { id: 'validate', label: t('config.tab.validate') }]} active={tab} onChange={setTab} />
        {tab === 'convert' ? <ResizableColumns className="io-workspace" columns={3} defaultSizes={[1, 0.28, 1]} minPaneWidths={[240, 110, 240]} storageKey="config-convert"><TextPane label={t('config.properties')} value={properties} onChange={setProperties} /><div className="io-actions"><button className="io-action-button" type="button" onClick={() => convert('toYaml')}><ArrowRight size={15} />{t('config.toYaml')}</button><button className="io-action-button" type="button" onClick={() => convert('toProperties')}><ArrowLeft size={15} />{t('config.toProperties')}</button></div><TextPane label={t('config.yaml')} value={yaml} onChange={setYaml} /></ResizableColumns> : <ResizableColumns className="yaml-validate-layout" columns={3} defaultSizes={[1, 0.32, 0.6]} minPaneWidths={[240, 110, 220]} storageKey="config-validate"><TextPane label={t('config.yaml')} value={validateSource} onChange={setValidateSource} /><div className="validate-actions"><button className="primary-command" type="button" onClick={validate}>{t('config.validate')}</button><button className="panel-command" type="button" onClick={format}><WandSparkles size={14} />{t('config.format')}</button></div><output className={valid === false ? 'validation-output validation-output--error' : 'validation-output'}>{validation || t('config.validate')}</output></ResizableColumns>}
      </div>
      <HistoryDialog funcType="ymlProperties" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={(value) => setYaml(value)} onApplyRecord={(record) => { if (record.extraData === 'toProperties') { setYaml(record.inputText); setProperties(record.outputText); setTab('convert') } else if (record.extraData === 'validate' || record.extraData === 'format') { setValidateSource(record.inputText); setValidation(record.outputText); setTab('validate') } else { setProperties(record.inputText); setYaml(record.outputText); setTab('convert') } }} />
    </section>
  )
}
