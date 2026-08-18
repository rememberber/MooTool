import {
  Binary,
  CheckCircle2,
  Clipboard,
  Copy,
  Globe2,
  Network,
  Play,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { networkMessages } from './networkMessages'
import { analyzeIpv4Cidr, integerToIpv4, NetworkToolError, type Ipv4Category } from './networkTools'

type NetworkMessageKey = LocalizedMessageKey<typeof networkMessages>
type NetworkNotice = { key: NetworkMessageKey; values?: MessageValues } | { raw: string }

export function NetworkSurface() {
  const { t, locale } = useLocalizedMessages(networkMessages)
  const [input, setInput] = useState('192.168.1.10/24')
  const [integerInput, setIntegerInput] = useState('3232235777')
  const [result, setResult] = useState(() => analyzeIpv4Cidr('192.168.1.10/24'))
  const [integerResult, setIntegerResult] = useState('192.168.1.1')
  const [notice, setNotice] = useState<NetworkNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState('')
  const category = t(categoryMessageKey(result.category))
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ input, integerInput, network: result.network }),
    summary: `${result.address}/${result.prefix} · ${category}`
  }), [category, input, integerInput, result])
  const { sessionId, reportError } = useToolSessionReport('network', session.digest, session.summary)

  function analyze(): void {
    try {
      setResult(analyzeIpv4Cidr(input))
      succeed('notice.analyzed')
    } catch (cause) {
      fail(cause)
    }
  }

  function convertInteger(): void {
    try {
      setIntegerResult(integerToIpv4(integerInput))
      succeed('notice.converted')
    } catch (cause) {
      fail(cause)
    }
  }

  async function copy(value: string): Promise<void> {
    try {
      await clipboardApi.writeText(value)
      setCopied(value)
      succeed('notice.copied', { value })
      window.setTimeout(() => setCopied(''), 1200)
    } catch {
      setNotice({ key: 'error.clipboard' })
      setFailed(true)
    }
  }

  function succeed(key: NetworkMessageKey, values?: MessageValues): void {
    setNotice({ key, values })
    setFailed(false)
  }

  function fail(cause: unknown): void {
    setNotice(cause instanceof NetworkToolError ? { key: `error.${cause.code}` } : { raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
  }

  const facts = [
    [t('fact.address'), result.address],
    [t('fact.category'), category],
    [t('fact.netmask'), result.netmask],
    [t('fact.wildcard'), result.wildcard],
    [t('fact.network'), result.network],
    [t('fact.broadcast'), result.broadcast],
    [t('fact.firstHost'), result.firstHost],
    [t('fact.lastHost'), result.lastHost],
    [t('fact.total'), result.totalAddresses.toLocaleString(locale)],
    [t('fact.usable'), result.usableHosts.toLocaleString(locale)],
    [t('fact.integer'), String(result.integer)],
    [t('fact.binary'), result.binary]
  ] as const

  return (
    <main className="utility-workbench network-workbench">
      <header className="utility-header">
        <div><span className="eyebrow">TAURI NETWORK TOOLBOX</span><h1>{t('title')}</h1></div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="network-inputs">
        <label><Network /><span>IPv4 / CIDR</span><input value={input} onChange={(event) => setInput(event.target.value)} />
          <button className="primary-button" type="button" onClick={analyze}><Play />{t('action.calculate')}</button>
        </label>
        <label><Binary /><span>{t('field.integer')}</span><input value={integerInput} onChange={(event) => setIntegerInput(event.target.value)} />
          <button className="secondary-button" type="button" onClick={convertInteger}>{t('action.convert')}</button>
          <code>{integerResult}</code>
        </label>
      </section>

      <section className="network-results">
        <header>
          <div><Globe2 /><strong>{result.address}/{result.prefix}</strong><span>{category}</span></div>
          <button className="utility-copy" type="button" onClick={() => void copy(`${result.address}/${result.prefix}`)}>
            {copied === `${result.address}/${result.prefix}` ? <Clipboard /> : <Copy />}{t('action.copyCidr')}
          </button>
        </header>
        <div>
          {facts.map(([label, value]) => (
            <button type="button" key={label} onClick={() => void copy(value)}>
              <span>{label}</span><code>{value}</code><Copy />
            </button>
          ))}
        </div>
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function categoryMessageKey(category: Ipv4Category): NetworkMessageKey {
  return `category.${category}`
}
