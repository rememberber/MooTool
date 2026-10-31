import {
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  Sparkles,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableThreeColumns } from '../../shared/ResizableThreeColumns'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  formatYaml,
  propertiesToYaml,
  ConfigToolError,
  validateYaml,
  yamlToProperties
} from './configTools'
import { configMessages } from './configMessages'

type ConfigMessageKey = LocalizedMessageKey<typeof configMessages>
type ConfigNotice = { key: ConfigMessageKey; values?: MessageValues } | { raw: string }
type ConfigTab = 'convert' | 'validate'

export function ConfigSurface() {
  const { t } = useLocalizedMessages(configMessages)
  const [tab, setTab] = useState<ConfigTab>('convert')
  const [yaml, setYaml] = useState(() => t('sample.yaml'))
  const [properties, setProperties] = useState('')
  const [validateSource, setValidateSource] = useState('app:\n  name: MooTool\n  enabled: true\n')
  const [validationText, setValidationText] = useState('')
  const [valid, setValid] = useState<boolean | null>(null)
  const [notice, setNotice] = useState<ConfigNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState<'yaml' | 'properties' | 'validate' | ''>('')
  const validation = useMemo(() => validateYaml(yaml), [yaml])
  const session = useMemo(() => ({
    digest: JSON.stringify({
      yamlLength: yaml.length,
      yamlHash: contentFingerprint(yaml),
      propertiesLength: properties.length,
      propertiesHash: contentFingerprint(properties),
      valid: validation.valid
    }),
    summary: t('session.summary', { yaml: yaml.length, properties: properties.length })
  }), [properties, t, validation.valid, yaml])
  const { sessionId, reportError } = useToolSessionReport('config', session.digest, session.summary)
  const recordOperation = useOperationHistory('config')

  useOperationRestore('config', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (metadata.direction === 'validate' || metadata.direction === 'format') {
      setTab('validate')
      setValidateSource(entry.inputText)
      setValidationText(entry.outputText)
      setValid(metadata.direction === 'format' ? true : metadata.valid === false ? false : metadata.valid === true ? true : null)
      setFailed(false)
      return
    }
    try {
      const restored = JSON.parse(entry.inputText) as { yaml?: string; properties?: string }
      setYaml(restored.yaml ?? '')
      setProperties(restored.properties ?? '')
      const target = parseOperationMetadata(entry).target
      if (target === 'yaml') setYaml(entry.outputText)
      if (target === 'properties') setProperties(entry.outputText)
    } catch {
      setYaml(entry.outputText || entry.inputText)
    }
    setFailed(false)
  })

  function run(operation: () => string, apply: (value: string) => void, success: ConfigMessageKey): void {
    try {
      const output = operation()
      apply(output)
      setNotice({ key: success })
      setFailed(false)
      const target = success === 'notice.toProperties' ? 'properties' : 'yaml'
      recordOperation(t(success), `${yaml.length} / ${properties.length} → ${output.length}`, 'success', {
        inputText: JSON.stringify({ yaml, properties }), outputText: output, metadata: { target }
      })
    } catch (cause) {
      setNotice(cause instanceof ConfigToolError
        ? { key: `error.${cause.code}`, values: cause.values }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
      recordOperation(t(success), cause instanceof Error ? cause.message : String(cause), 'error', {
        inputText: JSON.stringify({ yaml, properties })
      })
    }
  }

  function validateDocument(): void {
    const result = validateYaml(validateSource)
    const message = result.valid ? t('validation.detailValid') : t('validation.detailInvalid', { message: result.message })
    setValid(result.valid)
    setValidationText(message)
    setNotice(result.valid ? { key: 'validation.detailValid' } : { key: 'validation.detailInvalid', values: { message: result.message } })
    setFailed(!result.valid)
    recordOperation(t('action.validate'), message, result.valid ? 'success' : 'error', {
      inputText: validateSource, outputText: message, metadata: { direction: 'validate', valid: result.valid }
    })
  }

  function formatDocument(): void {
    try {
      const output = formatYaml(validateSource)
      setValidateSource(output)
      setValid(true)
      setValidationText(t('validation.detailValid'))
      setNotice({ key: 'notice.formatted' })
      setFailed(false)
      recordOperation(t('action.format'), `${validateSource.length} → ${output.length}`, 'success', {
        inputText: validateSource, outputText: output, metadata: { direction: 'format' }
      })
    } catch (cause) {
      setNotice(cause instanceof ConfigToolError
        ? { key: `error.${cause.code}`, values: cause.values }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  async function copy(side: 'yaml' | 'properties' | 'validate'): Promise<void> {
    try {
      await clipboardApi.writeText(side === 'yaml' ? yaml : side === 'validate' ? validateSource : properties)
      setCopied(side)
      window.setTimeout(() => setCopied(''), 1200)
    } catch {
      setNotice({ key: 'notice.copyFailed' })
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench config-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">YAML / Properties</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar">
        <div className="protobuf-tabs config-tabs" role="tablist">
          {(['convert', 'validate'] as const).map((id) => (
            <button key={id} type="button" role="tab" aria-selected={tab === id} onClick={() => setTab(id)}>{t(`tab.${id}`)}</button>
          ))}
        </div>
        {tab === 'convert' && <button
          className="icon-button config-clear"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            setYaml('')
            setProperties('')
          }}
        >
          <Eraser />
        </button>}
      </section>

      {tab === 'convert' && <ResizableThreeColumns id="config-convert" className="config-layout" initialLeft={1} initialRight={1} defaultSizes={[1, 0.28, 1]} minLeft={240} minCenter={110} minRight={240}>
        <ConfigPane
          label="Properties"
          value={properties}
          copied={copied === 'properties'}
          onChange={setProperties}
          onCopy={() => void copy('properties')}
        />
        <div className="config-actions">
          <button
            className="primary-button"
            type="button"
            disabled={!properties.trim()}
            onClick={() => run(() => propertiesToYaml(properties), setYaml, 'notice.toYaml')}
          >
            <ArrowRight />{t('action.toYaml')}
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={!yaml.trim()}
            onClick={() => run(() => yamlToProperties(yaml), setProperties, 'notice.toProperties')}
          >
            <ArrowLeft />{t('action.toProperties')}
          </button>
        </div>
        <ConfigPane
          label="YAML"
          value={yaml}
          copied={copied === 'yaml'}
          onChange={setYaml}
          onCopy={() => void copy('yaml')}
        />
      </ResizableThreeColumns>}

      {tab === 'validate' && (
        <ResizableThreeColumns id="config-validate" className="config-layout" initialLeft={1} initialRight={0.6} defaultSizes={[1, 0.32, 0.6]} minLeft={240} minCenter={110} minRight={220}>
          <ConfigPane label="YAML" value={validateSource} copied={copied === 'validate'} onChange={setValidateSource} onCopy={() => void copy('validate')} />
          <div className="config-actions">
            <button className="primary-button" type="button" onClick={validateDocument}>{t('action.validate')}</button>
            <button className="secondary-button" type="button" onClick={formatDocument}><Sparkles />{t('action.format')}</button>
          </div>
          <output className={valid === false ? 'config-validation-output config-validation-output--error' : 'config-validation-output'}>{validationText || t('action.validate')}</output>
        </ResizableThreeColumns>
      )}

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{'raw' in notice ? notice.raw : t(notice.key, notice.values)}</span>
        <span>{t('status.local')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function ConfigPane({ label, value, copied, onChange, onCopy }: {
  label: string
  value: string
  copied: boolean
  onChange(value: string): void
  onCopy(): void
}) {
  const { t } = useLocalizedMessages(configMessages)
  return (
    <section className="utility-editor-card config-pane">
      <header>
        <span>{label}</span>
        <button className="utility-copy" type="button" onClick={onCopy}>
          {copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}
        </button>
      </header>
      <CodeEditor
        ariaLabel={t('editor.label', { type: label })}
        value={value}
        onChange={onChange}
        className="utility-code-editor"
      />
    </section>
  )
}
