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
  Square,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { networkToolsApi } from '../../platform/api/networkToolsApi'
import type { NetworkConnectionInfo, NetworkInterfaceInfo, NetworkRangeScanResult, PingResult, PortScanResult, WhoisResult } from '../../platform/contracts/networkTools'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { networkMessages } from './networkMessages'
import { analyzeIpv4Cidr, integerToIpv4, NetworkToolError, type Ipv4Category } from './networkTools'

type NetworkMessageKey = LocalizedMessageKey<typeof networkMessages>
type NetworkNotice = { key: NetworkMessageKey; values?: MessageValues } | { raw: string }
type NetworkTab = 'calculator' | 'interfaces' | 'diagnostics'

export function NetworkSurface() {
  const dialog = useDesktopDialog()
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
  const [connections, setConnections] = useState<NetworkConnectionInfo[]>([])
  const [connectionQuery, setConnectionQuery] = useState('')
  const [target, setTarget] = useState('localhost')
  const [dnsResults, setDnsResults] = useState<string[]>([])
  const [pingResult, setPingResult] = useState<PingResult>()
  const [scanResult, setScanResult] = useState<PortScanResult>()
  const [whoisResult, setWhoisResult] = useState<WhoisResult>()
  const [rangeCidr, setRangeCidr] = useState('192.168.1.0/24')
  const [rangePorts, setRangePorts] = useState('22, 80, 443')
  const [rangeResult, setRangeResult] = useState<NetworkRangeScanResult>()
  const [startPort, setStartPort] = useState(1)
  const [endPort, setEndPort] = useState(1024)
  const [busy, setBusy] = useState('')
  const [taskId, setTaskId] = useState('')
  const category = t(categoryMessageKey(result.category))
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ input, integerInput, network: result.network }),
    summary: `${result.address}/${result.prefix} · ${category}`
  }), [category, input, integerInput, result])
  const { sessionId, reportError } = useToolSessionReport('network', session.digest, session.summary)
  const recordOperation = useOperationHistory('network')
  useOperationRestore('network', (entry) => {
    const metadata = parseOperationMetadata(entry)
    if (metadata.operation === 'cidr') {
      setTab('calculator'); setInput(entry.inputText); try { setResult(analyzeIpv4Cidr(entry.inputText)); setFailed(false) } catch (cause) { fail(cause) }
    } else if (metadata.operation === 'integer') {
      setTab('calculator'); setIntegerInput(entry.inputText); setIntegerResult(entry.outputText)
    } else {
      setTab('diagnostics')
      if (typeof metadata.startPort === 'number') setStartPort(metadata.startPort)
      if (typeof metadata.endPort === 'number') setEndPort(metadata.endPort)
      if (metadata.operation === 'range') {
        setRangeCidr(entry.inputText)
        if (Array.isArray(metadata.ports)) setRangePorts(metadata.ports.filter((port) => typeof port === 'number').join(', '))
        setRangeResult({
          cidr: entry.inputText,
          scannedHosts: Number(metadata.scannedHosts ?? 0),
          reachableHosts: entry.outputText.split('\n').filter(Boolean).map((line) => {
            const [address, ports = ''] = line.split('\t')
            return { address, openPorts: ports.split(',').map(Number).filter(Number.isFinite) }
          }),
          durationMs: Number(metadata.durationMs ?? 0),
          cancelled: Boolean(metadata.cancelled)
        })
      } else {
        setTarget(entry.inputText)
        if (metadata.operation === 'dns') setDnsResults(entry.outputText ? entry.outputText.split('\n') : [])
        if (metadata.operation === 'ping') setPingResult({ host: entry.inputText, success: Boolean(metadata.success), output: entry.outputText, durationMs: Number(metadata.durationMs ?? 0) })
        if (metadata.operation === 'scan') setScanResult({ host: entry.inputText, resolvedAddress: typeof metadata.resolvedAddress === 'string' ? metadata.resolvedAddress : entry.inputText, startPort: Number(metadata.startPort ?? 1), endPort: Number(metadata.endPort ?? 1), openPorts: entry.outputText ? entry.outputText.split(',').map(Number).filter(Number.isFinite) : [], durationMs: Number(metadata.durationMs ?? 0), cancelled: Boolean(metadata.cancelled) })
        if (metadata.operation === 'whois') setWhoisResult({ query: entry.inputText, server: typeof metadata.server === 'string' ? metadata.server : '', output: entry.outputText, durationMs: Number(metadata.durationMs ?? 0) })
      }
    }
    setFailed(false)
  })

  useEffect(() => {
    if (tab === 'interfaces' && !interfaces.length) void loadInterfaces()
  }, [tab])

  function analyze(): void {
    try {
      const next = analyzeIpv4Cidr(input)
      setResult(next)
      succeed('notice.analyzed')
      recordOperation(t('action.calculate'), `${next.network}/${next.prefix}`, 'success', { inputText: input, outputText: JSON.stringify(next, null, 2), metadata: { operation: 'cidr' } })
    } catch (cause) {
      fail(cause)
    }
  }

  function convertInteger(): void {
    try {
      const next = integerToIpv4(integerInput)
      setIntegerResult(next)
      succeed('notice.converted')
      recordOperation(t('action.convert'), `${integerInput} → ${next}`, 'success', { inputText: integerInput, outputText: next, metadata: { operation: 'integer' } })
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

  async function loadConnections(): Promise<void> {
    setBusy('connections')
    try {
      const loaded = await networkToolsApi.connections()
      setConnections(loaded)
      succeed('notice.connections', { count: loaded.length })
    } catch (cause) { fail(cause) } finally { setBusy('') }
  }

  async function resolveTarget(): Promise<void> {
    setBusy('dns')
    try {
      const loaded = await networkToolsApi.resolve(target.trim())
      setDnsResults(loaded)
      succeed('notice.resolved', { count: loaded.length })
      recordOperation(t('operation.dns'), `${target} · ${loaded.join(', ')}`, 'success', { inputText: target.trim(), outputText: loaded.join('\n'), metadata: { operation: 'dns' } })
    } catch (cause) { fail(cause); recordOperation(t('operation.dns'), String(cause), 'error', { inputText: target.trim(), metadata: { operation: 'dns' } }) } finally { setBusy('') }
  }

  async function pingTarget(): Promise<void> {
    setBusy('ping')
    try {
      const loaded = await networkToolsApi.ping(target.trim())
      setPingResult(loaded)
      succeed(loaded.success ? 'notice.pingComplete' : 'notice.pingFailed')
      recordOperation(t('operation.ping'), `${target} · ${loaded.durationMs} ms`, loaded.success ? 'success' : 'error', { inputText: target.trim(), outputText: loaded.output, metadata: { operation: 'ping', durationMs: loaded.durationMs, success: loaded.success } })
    } catch (cause) { fail(cause); recordOperation(t('operation.ping'), String(cause), 'error', { inputText: target.trim(), metadata: { operation: 'ping' } }) } finally { setBusy('') }
  }

  async function scanTarget(): Promise<void> {
    const requestId = crypto.randomUUID()
    setTaskId(requestId)
    setBusy('scan')
    try {
      const loaded = await networkToolsApi.scanPorts(requestId, target.trim(), startPort, endPort, 500)
      setScanResult(loaded)
      succeed(loaded.cancelled ? 'notice.scanCancelled' : 'notice.scanComplete', { count: loaded.openPorts.length })
      recordOperation(t('operation.scan'), `${target}:${startPort}-${endPort} · ${loaded.openPorts.join(', ') || '—'}`, 'success', { inputText: target.trim(), outputText: loaded.openPorts.join(','), metadata: { operation: 'scan', startPort, endPort, resolvedAddress: loaded.resolvedAddress, durationMs: loaded.durationMs } })
    } catch (cause) { fail(cause); recordOperation(t('operation.scan'), String(cause), 'error', { inputText: target.trim(), metadata: { operation: 'scan', startPort, endPort } }) } finally { setBusy(''); setTaskId('') }
  }

  async function queryWhois(): Promise<void> {
    setBusy('whois')
    try {
      const loaded = await networkToolsApi.whois(target.trim())
      setWhoisResult(loaded)
      succeed('notice.whois', { server: loaded.server })
      recordOperation(t('operation.whois'), `${loaded.query} · ${loaded.server}`, 'success', { inputText: loaded.query, outputText: loaded.output, metadata: { operation: 'whois', server: loaded.server, durationMs: loaded.durationMs } })
    } catch (cause) { fail(cause); recordOperation(t('operation.whois'), String(cause), 'error', { inputText: target.trim(), metadata: { operation: 'whois' } }) } finally { setBusy('') }
  }

  async function scanRange(): Promise<void> {
    const ports = rangePorts.split(/[,\s]+/).filter(Boolean).map(Number)
    const requestId = crypto.randomUUID()
    setTaskId(requestId)
    setBusy('range')
    try {
      const loaded = await networkToolsApi.scanRange(requestId, rangeCidr.trim(), ports, 500)
      setRangeResult(loaded)
      succeed(loaded.cancelled ? 'notice.scanCancelled' : 'notice.rangeComplete', { count: loaded.reachableHosts.length })
      recordOperation(t('operation.range'), `${loaded.cidr} · ${loaded.reachableHosts.length}/${loaded.scannedHosts}`, 'success', {
        inputText: loaded.cidr, outputText: loaded.reachableHosts.map((host) => `${host.address}\t${host.openPorts.join(',')}`).join('\n'),
        metadata: { operation: 'range', ports, scannedHosts: loaded.scannedHosts, durationMs: loaded.durationMs, cancelled: loaded.cancelled }
      })
    } catch (cause) { fail(cause); recordOperation(t('operation.range'), String(cause), 'error', { inputText: rangeCidr.trim(), metadata: { operation: 'range', ports } }) } finally { setBusy(''); setTaskId('') }
  }

  async function cancelScan(): Promise<void> {
    if (!taskId) return
    try { await networkToolsApi.cancelTask(taskId); setNotice({ key: 'notice.cancelling' }) } catch (cause) { fail(cause) }
  }

  async function flushDns(): Promise<void> {
    if (!await dialog.confirm(t('confirm.flushDns'))) return
    setBusy('flush')
    try { await networkToolsApi.flushDns(); succeed('notice.dnsFlushed') } catch (cause) { fail(cause) } finally { setBusy('') }
  }

  const visibleConnections = connections.filter((item) => {
    const needle = connectionQuery.trim().toLowerCase()
    return !needle || `${item.protocol} ${item.localAddress} ${item.remoteAddress} ${item.state} ${item.process}`.toLowerCase().includes(needle)
  })

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
        <header><div><Server /><strong>{t('interfaces.title')}</strong></div><span><button className="secondary-button" type="button" disabled={Boolean(busy)} onClick={() => void loadConnections()}><Radio />{t('action.connections')}</button><button className="secondary-button" type="button" disabled={Boolean(busy)} onClick={() => void flushDns()}><RefreshCw />{t('action.flushDns')}</button><button className="secondary-button" type="button" disabled={Boolean(busy)} onClick={() => void loadInterfaces()}><RefreshCw />{t('action.refresh')}</button></span></header>
        <div className="network-interface-list">{interfaces.length ? interfaces.map((item) => <article key={item.name}><header><strong>{item.name}</strong><code>{item.macAddress}</code></header><p>{item.addresses.join(' · ') || '—'}</p><dl><div><dt>MTU</dt><dd>{item.mtu}</dd></div><div><dt>{t('interfaces.received')}</dt><dd>{formatBytes(item.receivedBytes)}</dd></div><div><dt>{t('interfaces.transmitted')}</dt><dd>{formatBytes(item.transmittedBytes)}</dd></div></dl></article>) : <p>{t('interfaces.empty')}</p>}</div>
        {!!connections.length && <section className="network-connections"><header><strong>{t('connections.title', { count: visibleConnections.length })}</strong><label><input value={connectionQuery} placeholder={t('connections.search')} onChange={(event) => setConnectionQuery(event.target.value)} /></label></header><div>{visibleConnections.map((item, index) => <article key={`${item.protocol}-${item.localAddress}-${item.remoteAddress}-${index}`}><strong>{item.protocol}</strong><code>{item.localAddress}</code><span>→</span><code>{item.remoteAddress}</code><em>{item.state || '—'}</em><small>{item.process}</small></article>)}</div></section>}
      </section>}

      {tab === 'diagnostics' && <section className="network-native-panel network-diagnostics">
        <header><div><Radio /><strong>{t('diagnostics.title')}</strong></div></header>
        <label className="network-target"><span>{t('diagnostics.host')}</span><input value={target} spellCheck={false} onChange={(event) => setTarget(event.target.value)} /></label>
        <div className="network-diagnostic-actions"><button className="secondary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void resolveTarget()}><Globe2 />{t('action.dns')}</button><button className="secondary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void pingTarget()}><Radio />{t('action.ping')}</button><button className="secondary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void queryWhois()}><Server />WHOIS</button></div>
        <section className="network-scan"><header><ScanLine /><strong>{t('scan.title')}</strong></header><label>{t('scan.start')}<input type="number" min="1" max="65535" value={startPort} onChange={(event) => setStartPort(event.target.valueAsNumber)} /></label><label>{t('scan.end')}<input type="number" min="1" max="65535" value={endPort} onChange={(event) => setEndPort(event.target.valueAsNumber)} /></label>{busy === 'scan' ? <button className="primary-button" type="button" onClick={() => void cancelScan()}><Square />{t('action.cancel')}</button> : <button className="primary-button" type="button" disabled={Boolean(busy) || !target.trim()} onClick={() => void scanTarget()}>{t('action.scan')}</button>}</section>
        <section className="network-scan network-range-scan"><header><Network /><strong>{t('range.title')}</strong></header><label>CIDR<input value={rangeCidr} spellCheck={false} onChange={(event) => setRangeCidr(event.target.value)} /></label><label>{t('range.ports')}<input value={rangePorts} spellCheck={false} onChange={(event) => setRangePorts(event.target.value)} /></label>{busy === 'range' ? <button className="primary-button" type="button" onClick={() => void cancelScan()}><Square />{t('action.cancel')}</button> : <button className="primary-button" type="button" disabled={Boolean(busy) || !rangeCidr.trim()} onClick={() => void scanRange()}>{t('action.scanRange')}</button>}</section>
        <div className="network-diagnostic-results">{dnsResults.length > 0 && <article><strong>DNS</strong><code>{dnsResults.join('\n')}</code></article>}{pingResult && <article><strong>Ping · {pingResult.durationMs} ms</strong><pre>{pingResult.output}</pre></article>}{whoisResult && <article><strong>WHOIS · {whoisResult.server}</strong><pre>{whoisResult.output}</pre><small>{whoisResult.durationMs} ms</small></article>}{scanResult && <article><strong>{t('scan.openPorts', { count: scanResult.openPorts.length })}</strong><code>{scanResult.openPorts.join(', ') || '—'}</code><small>{scanResult.resolvedAddress} · {scanResult.durationMs} ms</small></article>}{rangeResult && <article><strong>{t('range.result', { count: rangeResult.reachableHosts.length, total: rangeResult.scannedHosts })}</strong><pre>{rangeResult.reachableHosts.map((host) => `${host.address}\t${host.openPorts.join(', ')}`).join('\n') || '—'}</pre><small>{rangeResult.durationMs} ms</small></article>}</div>
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
