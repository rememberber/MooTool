import {
  Bot,
  CheckCircle2,
  Clipboard,
  Copy,
  Cpu,
  Eraser,
  Globe2,
  Laptop,
  MonitorSmartphone,
  Play,
  Smartphone,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { parseUserAgent, uaPresets, UaToolError, type UaResult } from './uaTools'
import { uaMessages } from './uaMessages'

type UaMessageKey = LocalizedMessageKey<typeof uaMessages>
type UaError = { key: UaMessageKey } | { raw: string }

const defaultUa = uaPresets[0][1]

export function UaSurface() {
  const { t } = useLocalizedMessages(uaMessages)
  const [source, setSource] = useState<string>(defaultUa)
  const [result, setResult] = useState<UaResult>(() => parseUserAgent(defaultUa))
  const [error, setError] = useState<UaError>()
  const [copied, setCopied] = useState(false)
  const errorText = error ? ('raw' in error ? error.raw : t(error.key)) : ''
  const deviceType = displayDeviceType(result.deviceType, t)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      length: source.length,
      hash: contentFingerprint(source),
      browser: result.browser,
      os: result.os,
      deviceType: result.deviceType,
      valid: !error
    }),
    summary: t('session.summary', {
      browser: displayValue(result.browser, t),
      os: displayValue(result.os, t),
      device: deviceType
    })
  }), [deviceType, error, result.browser, result.os, source, t])
  const { sessionId, reportError } = useToolSessionReport('ua', session.digest, session.summary)

  function analyze(nextSource = source): void {
    try {
      setResult(parseUserAgent(nextSource))
      setSource(nextSource)
      setError(undefined)
    } catch (cause) {
      setError(cause instanceof UaToolError
        ? { key: `error.${cause.code}` }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
    }
  }

  async function copy(): Promise<void> {
    try {
      await clipboardApi.writeText(source)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setError({ key: 'error.copyFailed' })
    }
  }

  return (
    <main className="utility-workbench ua-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI USER-AGENT ANALYZER</span>
          <h1>{t('title')}</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="ua-presets">
        {uaPresets.map(([label, value]) => (
          <button type="button" key={label} onClick={() => analyze(value)}>{label}</button>
        ))}
      </section>

      <section className="ua-content">
        <section className="utility-editor-card ua-input">
          <header>
            <span>User-Agent</span>
            <div>
              <button className="utility-copy" type="button" onClick={() => void copy()}>
                {copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}
              </button>
              <button
                className="utility-copy ua-clear"
                type="button"
                onClick={() => {
                  setSource('')
                  setError(undefined)
                }}
              >
                <Eraser />{t('action.clear')}
              </button>
            </div>
          </header>
          <CodeEditor
            ariaLabel={t('input.label')}
            value={source}
            onChange={setSource}
            className="utility-code-editor"
            lineWrapping
          />
          <button className="primary-button ua-run" type="button" onClick={() => analyze()}>
            <Play />{t('action.parse')}
          </button>
        </section>

        <section className="ua-result">
          <header className={error ? 'ua-result__hero ua-result__hero--error' : 'ua-result__hero'}>
            {error
              ? <TriangleAlert />
              : result.bot
                ? <Bot />
                : result.mobile
                  ? <Smartphone />
                  : <Laptop />}
            <div>
              <strong>{errorText || displayValue(result.browser, t)}</strong>
              <span>{error ? t('result.correct') : `${displayValue(result.browserVersion, t)} · ${deviceType}`}</span>
            </div>
            {!error && (
              <em>{result.bot ? 'BOT' : result.mobile ? 'MOBILE' : 'DESKTOP'}</em>
            )}
          </header>
          {!error && (
            <dl className="ua-facts">
              <Fact icon={Globe2} label={t('fact.browser')} value={displayValue(result.browser, t)} detail={displayValue(result.browserVersion, t)} />
              <Fact icon={Cpu} label={t('fact.engine')} value={displayValue(result.engine, t)} detail={displayValue(result.engineVersion, t)} />
              <Fact icon={MonitorSmartphone} label={t('fact.os')} value={displayValue(result.os, t)} detail={displayValue(result.osVersion, t)} />
              <Fact icon={Cpu} label={t('fact.cpu')} value={displayValue(result.cpu, t)} />
              <Fact icon={Smartphone} label={t('fact.deviceType')} value={deviceType} />
              <Fact
                icon={Laptop}
                label={t('fact.device')}
                value={displayValue(result.deviceBrand, t)}
                detail={displayValue(result.deviceModel, t)}
              />
            </dl>
          )}
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

function Fact({ icon: Icon, label, value, detail = '' }: {
  icon: typeof Globe2
  label: string
  value: string
  detail?: string
}) {
  return (
    <div>
      <dt><Icon />{label}</dt>
      <dd>{value}</dd>
      {detail && <small>{detail}</small>}
    </div>
  )
}
