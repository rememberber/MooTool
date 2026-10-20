import {
  CheckCircle2,
  ClipboardPaste,
  Eraser,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { useOperationRestore } from '../history/operationRestore'
import { parseUserAgent, uaPresets, UaToolError, type UaResult } from './uaTools'
import { uaMessages } from './uaMessages'

type UaMessageKey = LocalizedMessageKey<typeof uaMessages>
type UaError = { key: UaMessageKey } | { raw: string }

const defaultUa = uaPresets[0][1]

export function UaSurface() {
  const { t } = useLocalizedMessages(uaMessages)
  const [source, setSource] = useState<string>(defaultUa)
  const [result, setResult] = useState<UaResult | null>(() => parseUserAgent(defaultUa))
  const [error, setError] = useState<UaError>()
  const errorText = error ? ('raw' in error ? error.raw : t(error.key)) : ''
  const deviceType = result ? displayDeviceType(result.deviceType, t) : ''
  const session = useMemo(() => ({
    digest: JSON.stringify({
      length: source.length,
      hash: contentFingerprint(source),
      browser: result?.browser ?? '',
      os: result?.os ?? '',
      deviceType: result?.deviceType ?? '',
      valid: !error
    }),
    summary: t('session.summary', {
      browser: displayValue(result?.browser ?? '', t),
      os: displayValue(result?.os ?? '', t),
      device: deviceType
    })
  }), [deviceType, error, result, source, t])
  const { sessionId, reportError } = useToolSessionReport('ua', session.digest, session.summary)
  const recordOperation = useOperationHistory('ua')
  useOperationRestore('ua', (entry) => {
    setSource(entry.inputText)
    try { setResult(parseUserAgent(entry.inputText)); setError(undefined) } catch (cause) { setError({ raw: cause instanceof Error ? cause.message : String(cause) }) }
  })

  function analyze(nextSource = source): void {
    try {
      const next = parseUserAgent(nextSource)
      setResult(next)
      setSource(nextSource)
      setError(undefined)
      recordOperation(t('action.parse'), `${next.browser || '—'} · ${next.os || '—'} · ${next.deviceType || '—'}`, 'success', {
        inputText: nextSource, outputText: JSON.stringify(next, null, 2)
      })
    } catch (cause) {
      setError(cause instanceof UaToolError
        ? { key: `error.${cause.code}` }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
      recordOperation(t('action.parse'), cause instanceof Error ? cause.message : String(cause), 'error', { inputText: nextSource })
    }
  }

  async function paste(): Promise<void> {
    try {
      setSource(await navigator.clipboard.readText())
    } catch (cause) {
      setError({ raw: cause instanceof Error ? cause.message : String(cause) })
    }
  }

  return (
    <main className="utility-workbench ua-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="ua-content">
        <section className="utility-editor-card ua-input">
          <header><span>{t('input.label')}</span></header>
          <CodeEditor
            ariaLabel={t('input.label')}
            value={source}
            onChange={setSource}
            className="utility-code-editor"
            lineWrapping
          />
          <div className="ua-actions">
            <select aria-label={t('preset')} defaultValue="" onChange={(event) => { if (event.target.value) setSource(event.target.value) }}>
              <option value="">{t('preset')}</option>
              {uaPresets.map(([label, value]) => <option key={label} value={value}>{label}</option>)}
            </select>
            <button type="button" onClick={() => void paste()}><ClipboardPaste />{t('action.paste')}</button>
            <button type="button" onClick={() => { setSource(''); setResult(null); setError(undefined) }}><Eraser />{t('action.clear')}</button>
            <button className="primary-button" type="button" onClick={() => analyze()}>{t('action.parse')}</button>
          </div>
        </section>
        <section className="ua-result" aria-label={t('title')}>
          {result && ([
            [t('fact.browser'), result.browser],
            [t('fact.browserVersion'), result.browserVersion],
            [t('fact.engine'), result.engine],
            [t('fact.engineVersion'), result.engineVersion],
            [t('fact.os'), result.os],
            [t('fact.osVersion'), result.osVersion],
            [t('fact.deviceType'), result.deviceType],
            [t('fact.deviceBrand'), result.deviceBrand],
            [t('fact.deviceModel'), result.deviceModel],
            [t('fact.mobile'), result.mobile ? t('value.yes') : t('value.no')],
            [t('fact.bot'), result.bot ? t('value.yes') : t('value.no')]
          ] as const).map(([label, value]) => (
            <div className="ua-result-row" key={label}><span>{label}</span><strong>{displayValue(value, t)}</strong></div>
          ))}
        </section>
      </section>

      <footer className={error ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{error ? <TriangleAlert /> : <CheckCircle2 />}
          {errorText || t('status.ready')}
        </span>
        <span>{t('status.local')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function displayValue(value: string, t: (key: UaMessageKey) => string): string {
  return value === 'Unknown' ? t('value.unknown') : value
}

function displayDeviceType(value: string, t: (key: UaMessageKey) => string): string {
  const known: Record<string, UaMessageKey> = {
    bot: 'device.bot', mobile: 'device.mobile', tablet: 'device.tablet', wearable: 'device.wearable',
    console: 'device.console', smarttv: 'device.smarttv', embedded: 'device.embedded', desktop: 'device.desktop'
  }
  return known[value] ? t(known[value]) : displayValue(value, t)
}

