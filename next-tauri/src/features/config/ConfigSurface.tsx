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
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
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

export function ConfigSurface() {
  const { t } = useLocalizedMessages(configMessages)
  const [yaml, setYaml] = useState(() => t('sample.yaml'))
  const [properties, setProperties] = useState('')
  const [notice, setNotice] = useState<ConfigNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState<'yaml' | 'properties' | ''>('')
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

  function run(operation: () => string, apply: (value: string) => void, success: ConfigMessageKey): void {
    try {
      const output = operation()
      apply(output)
      setNotice({ key: success })
      setFailed(false)
      recordOperation(t(success), `${yaml.length} / ${properties.length} → ${output.length}`, 'success')
    } catch (cause) {
      setNotice(cause instanceof ConfigToolError
        ? { key: `error.${cause.code}`, values: cause.values }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
      recordOperation(t(success), cause instanceof Error ? cause.message : String(cause), 'error')
    }
  }

  async function copy(side: 'yaml' | 'properties'): Promise<void> {
    try {
      await clipboardApi.writeText(side === 'yaml' ? yaml : properties)
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
        <div>
          <span className="eyebrow">TAURI CONFIG CONVERTER</span>
          <h1>YAML / Properties</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar">
        <button
          className="secondary-button"
          type="button"
          disabled={!yaml.trim()}
          onClick={() => run(() => formatYaml(yaml), setYaml, 'notice.formatted')}
        >
          <Sparkles />{t('action.formatYaml')}
        </button>
        <span className={validation.valid ? 'config-validation' : 'config-validation config-validation--error'}>
          {validation.valid ? <CheckCircle2 /> : <TriangleAlert />}
          {t(validation.valid ? 'validation.valid' : 'validation.invalid')}
        </span>
        <button
          className="icon-button config-clear"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => {
            setYaml('')
            setProperties('')
          }}
        >
          <Eraser />
        </button>
      </section>

      <section className="config-layout">
        <ConfigPane
          label="YAML"
          value={yaml}
          copied={copied === 'yaml'}
          onChange={setYaml}
          onCopy={() => void copy('yaml')}
        />
        <div className="config-actions">
          <button
            className="primary-button"
            type="button"
            disabled={!yaml.trim()}
            onClick={() => run(() => yamlToProperties(yaml), setProperties, 'notice.toProperties')}
          >
            <ArrowRight />{t('action.toProperties')}
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={!properties.trim()}
            onClick={() => run(() => propertiesToYaml(properties), setYaml, 'notice.toYaml')}
          >
            <ArrowLeft />{t('action.toYaml')}
          </button>
        </div>
        <ConfigPane
          label="Properties"
          value={properties}
          copied={copied === 'properties'}
          onChange={setProperties}
          onCopy={() => void copy('properties')}
        />
      </section>

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
