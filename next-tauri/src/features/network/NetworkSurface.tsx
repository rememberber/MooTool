import {
  Binary,
  CheckCircle2,
  Clipboard,
  Copy,
  Globe2,
  Network,
  Play,
  Radio,
  RefreshCw,
  ScanLine,
  Server,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { networkToolsApi } from '../../platform/api/networkToolsApi'
import type { NetworkInterfaceInfo, PingResult, PortScanResult } from '../../platform/contracts/networkTools'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { networkMessages } from './networkMessages'
import { analyzeIpv4Cidr, integerToIpv4, NetworkToolError, type Ipv4Category } from './networkTools'

type NetworkMessageKey = LocalizedMessageKey<typeof networkMessages>
type NetworkNotice = { key: NetworkMessageKey; values?: MessageValues } | { raw: string }
type NetworkTab = 'calculator' | 'interfaces' | 'diagnostics'

export function NetworkSurface() {
  const { t, locale } = useLocalizedMessages(networkMessages)
  const [input, setInput] = useState('192.168.1.10/24')
  const [integerInput, setIntegerInput] = useState('3232235777')
  const [result, setResult] = useState(() => analyzeIpv4Cidr('192.168.1.10/24'))
  const [integerResult, setIntegerResult] = useState('192.168.1.1')
  const [notice, setNotice] = useState<NetworkNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState('')
  const [tab, setTab] = useState<NetworkTab>('calculator')
  const [interfaces, setInterfaces] = useState<NetworkInterfaceInfo[]>([])
  const [target, setTarget] = useState('localhost')
  const [dnsResults, setDnsResults] = useState<string[]>([])
  const [pingResult, setPingResult] = useState<PingResult>()
  const [scanResult, setScanResult] = useState<PortScanResult>()
  const [startPort, setStartPort] = useState(1)
  const [endPort, setEndPort] = useState(1024)
  const [busy, setBusy] = useState('')
  const category = t(categoryMessageKey(result.category))
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ input, integerInput, network: result.network }),
    summary: `${result.address}/${result.prefix} · ${category}`
  }), [category, input, integerInput, result])
  const { sessionId, reportError } = useToolSessionReport('network', session.digest, session.summary)
  const recordOperation = useOperationHistory('network')

  useEffect(() => {
    if (tab === 'interfaces' && !interfaces.length) void loadInterfaces()
  }, [tab])

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

  async function loadInterfaces(): Promise<void> {
    setBusy('interfaces')
    try {
      const loaded = await networkToolsApi.interfaces()
      setInterfaces(loaded)
      succeed('notice.interfaces', { count: loaded.length })
    } catch (cause) { fail(cause) } finally { setBusy('') }
  }

  async function resolveTarget(): Promise<void> {
    setBusy('dns')
    try {
      const loaded = await networkToolsApi.resolve(target.trim())
      setDnsResults(loaded)
      succeed('notice.resolved', { count: loaded.length })
      recordOperation(t('operation.dns'), `${target} · ${loaded.join(', ')}`, 'success')
    } catch (cause) { fail(cause); recordOperation(t('operation.dns'), String(cause), 'error') } finally { setBusy('') }
  }

  async function pingTarget(): Promise<void> {
    setBusy('ping')
    try {
      const loaded = await networkToolsApi.ping(target.trim())
      setPingResult(loaded)
      succeed(loaded.success ? 'notice.pingComplete' : 'notice.pingFailed')
      recordOperation(t('operation.ping'), `${target} · ${loaded.durationMs} ms`, loaded.success ? 'success' : 'error')
    } catch (cause) { fail(cause); recordOperation(t('operation.ping'), String(cause), 'error') } finally { setBusy('') }
  }

  async function scanTarget(): Promise<void> {
    setBusy('scan')
    try {
      const loaded = await networkToolsApi.scanPorts(target.trim(), startPort, endPort, 500)
      setScanResult(loaded)
      succeed('notice.scanComplete', { count: loaded.openPorts.length })
      recordOperation(t('operation.scan'), `${target}:${startPort}-${endPort} · ${loaded.openPorts.join(', ') || '—'}`, 'success')
    } catch (cause) { fail(cause); recordOperation(t('operation.scan'), String(cause), 'error') } finally { setBusy('') }
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

      <nav className="utility-segments network-tabs" aria-label={t('tabs.aria')}>
        {(['calculator', 'interfaces', 'diagnostics'] as const).map((value) => <button className={tab === value ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" key={value} onClick={() => setTab(value)}>{t(`tab.${value}`)}</button>)}
      </nav>

      {tab === 'calculator' && <section className="network-tab-content network-calculator"><section className="network-inputs">
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
      </section></section>}

      {tab === 'interfaces' && <section className="network-native-panel">
        <header><div><Server /><strong>{t('interfaces.title')}</strong></div><button className="secondary-button" type="button" disabled={Boolean(busy)} onClick={() => void loadInterfaces()}><RefreshCw />{t('action.refresh')}</button></header>
        <div className="network-interface-list">{interfaces.length ? interfaces.map((item) => <article key={item.name}><header><strong>{item.name}</strong><code>{item.macAddress}</code></header><p>{item.addresses.join(' · ') || '—'}</p><dl><div><dt>MTU</dt><dd>{item.mtu}</dd></div><div><dt>{t('interfaces.received')}</dt><dd>{formatBytes(item.receivedBytes)}</dd></div><div><dt>{t('interfaces.transmitted')}</dt><dd>{formatBytes(item.transmittedBytes)}</dd></div></dl></article>) : <p>{t('interfaces.empty')}</p>}</div>
      </section>}

      {tab === 'diagnostics' && <section className="network-native-panel network-diagnostics">
        <header><div><Radio /><strong>{t('diagnostics.title')}</strong></div></header>
        <label className="network-target"><span>{t('diagnostics.host')}</span><input value={target} spellCheck={false} onChange={(event) => setTarget(event.target.value)} /></label>
        <div className="network-diagnostic-actions"><button className="secondary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void resolveTarget()}><Globe2 />{t('action.dns')}</button><button className="secondary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void pingTarget()}><Radio />{t('action.ping')}</button></div>
        <section className="network-scan"><header><ScanLine /><strong>{t('scan.title')}</strong></header><label>{t('scan.start')}<input type="number" min="1" max="65535" value={startPort} onChange={(event) => setStartPort(event.target.valueAsNumber)} /></label><label>{t('scan.end')}<input type="number" min="1" max="65535" value={endPort} onChange={(event) => setEndPort(event.target.valueAsNumber)} /></label><button className="primary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void scanTarget()}>{t('action.scan')}</button></section>
        <div className="network-diagnostic-results">{dnsResults.length > 0 && <article><strong>DNS</strong><code>{dnsResults.join('\n')}</code></article>}{pingResult && <article><strong>Ping · {pingResult.durationMs} ms</strong><pre>{pingResult.output}</pre></article>}{scanResult && <article><strong>{t('scan.openPorts', { count: scanResult.openPorts.length })}</strong><code>{scanResult.openPorts.join(', ') || '—'}</code><small>{scanResult.resolvedAddress} · {scanResult.durationMs} ms</small></article>}</div>
      </section>}

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

function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`
  if (value < 1024 ** 2) return `${(value / 1024).toFixed(1)} KiB`
  if (value < 1024 ** 3) return `${(value / 1024 ** 2).toFixed(1)} MiB`
  return `${(value / 1024 ** 3).toFixed(2)} GiB`
}
