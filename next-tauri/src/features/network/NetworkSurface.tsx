import {
  CheckCircle2,
  ClipboardCopy,
  Globe2,
  Network,
  Play,
  RefreshCw,
  Search,
  Square,
  Trash2,
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
import { formatOpenPorts, parseIpv4Range, parsePortSpec } from './networkCommand'
import { analyzeIpv4Cidr, integerToIpv4, ipv4ToLong, NetworkToolError, type Ipv4Category } from './networkTools'

type NetworkMessageKey = LocalizedMessageKey<typeof networkMessages>
type NetworkNotice = { key: NetworkMessageKey; values?: MessageValues } | { raw: string }
type NetworkTab = 'calculator' | 'interfaces' | 'diagnostics'

export function NetworkSurface() {
  const dialog = useDesktopDialog()
  const { t, locale } = useLocalizedMessages(networkMessages)
  const [input, setInput] = useState('192.168.1.10/24')
  const [ipv4, setIpv4] = useState('127.0.0.1')
  const [longValue, setLongValue] = useState('2130706433')
  const [output, setOutput] = useState('')
  const [pingTarget, setPingTarget] = useState('127.0.0.1')
  const [ipRange, setIpRange] = useState('192.168.10')
  const [portHost, setPortHost] = useState('127.0.0.1')
  const [portSpec, setPortSpec] = useState('')
  const [resolveHost, setResolveHost] = useState('localhost')
  const [whoisTarget, setWhoisTarget] = useState('example.com')
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

  useEffect(() => { void loadInterfaces(false) }, [])

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

  function convertAddress(): void {
    try {
      const next = String(ipv4ToLong(ipv4))
      setLongValue(next)
      setIntegerInput(next)
      succeed('notice.converted')
    } catch (cause) {
      fail(cause)
    }
  }

  function convertInteger(): void {
    try {
      const next = integerToIpv4(longValue)
      setIpv4(next)
      setIntegerResult(next)
      succeed('notice.converted')
      recordOperation(t('action.convert'), `${longValue} → ${next}`, 'success', { inputText: longValue, outputText: next, metadata: { operation: 'integer' } })
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

  async function loadInterfaces(publish = true): Promise<void> {
    setBusy('interfaces')
    try {
      const loaded = await networkToolsApi.interfaces()
      setInterfaces(loaded)
      if (publish) setOutput(loaded.map((item) => `${item.name}  ${item.addresses.join(' ')}`).join('\n') || t('layout.noOutput'))
      succeed('notice.interfaces', { count: loaded.length })
    } catch (cause) { fail(cause) } finally { setBusy('') }
  }

  async function loadConnections(): Promise<void> {
    setBusy('connections')
    try {
      const loaded = await networkToolsApi.connections()
      setConnections(loaded)
      setOutput(loaded.map((item) => `${item.protocol}  ${item.localAddress}  ${item.remoteAddress}  ${item.state}  ${item.process}`).join('\n') || t('layout.noOutput'))
      succeed('notice.connections', { count: loaded.length })
    } catch (cause) { fail(cause) } finally { setBusy('') }
  }

  async function resolveTarget(): Promise<void> {
    setBusy('dns')
    try {
      const loaded = await networkToolsApi.resolve(resolveHost.trim())
      setDnsResults(loaded)
      setOutput(loaded.join('\n') || t('layout.noOutput'))
      succeed('notice.resolved', { count: loaded.length })
      recordOperation(t('operation.dns'), `${target} · ${loaded.join(', ')}`, 'success', { inputText: target.trim(), outputText: loaded.join('\n'), metadata: { operation: 'dns' } })
    } catch (cause) { fail(cause); recordOperation(t('operation.dns'), String(cause), 'error', { inputText: target.trim(), metadata: { operation: 'dns' } }) } finally { setBusy('') }
  }

  async function runPing(): Promise<void> {
    setBusy('ping')
    try {
      const loaded = await networkToolsApi.ping(pingTarget.trim())
      setPingResult(loaded)
      setOutput(loaded.output || t('layout.noOutput'))
      succeed(loaded.success ? 'notice.pingComplete' : 'notice.pingFailed')
      recordOperation(t('operation.ping'), `${target} · ${loaded.durationMs} ms`, loaded.success ? 'success' : 'error', { inputText: target.trim(), outputText: loaded.output, metadata: { operation: 'ping', durationMs: loaded.durationMs, success: loaded.success } })
    } catch (cause) { fail(cause); recordOperation(t('operation.ping'), String(cause), 'error', { inputText: target.trim(), metadata: { operation: 'ping' } }) } finally { setBusy('') }
  }

  async function scanTarget(): Promise<void> {
    const requestId = crypto.randomUUID()
    setTaskId(requestId)
    setBusy('scan')
    setOutput(t('layout.running'))
    try {
      const ports = parsePortSpec(portSpec)
      const start = ports[0] ?? 1
      const end = ports.at(-1) ?? start
      if (end - start + 1 !== ports.length) throw new Error('Native port lists require the Tauri desktop app')
      const loaded = await networkToolsApi.scanPorts(requestId, portHost.trim(), start, end, 500)
      setScanResult(loaded)
      setOutput(formatOpenPorts(portHost.trim(), ports, loaded.openPorts))
      succeed(loaded.cancelled ? 'notice.scanCancelled' : 'notice.scanComplete', { count: loaded.openPorts.length })
    } catch (cause) {
      setOutput(cause instanceof Error ? cause.message : String(cause))
      fail(cause)
    } finally { setBusy(''); setTaskId('') }
  }

  async function probeRange(): Promise<void> {
    setBusy('range')
    setOutput(t('layout.running'))
    try {
      parseIpv4Range(ipRange)
      setOutput('Native ping range requires the Tauri desktop app')
    } catch (cause) {
      setOutput(cause instanceof Error ? cause.message : String(cause))
      fail(cause)
    } finally { setBusy('') }
  }

  async function queryWhois(): Promise<void> {
    setBusy('whois')
    try {
      const loaded = await networkToolsApi.whois(whoisTarget.trim())
      setWhoisResult(loaded)
      setOutput(loaded.output || t('layout.noOutput'))
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
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="network-workspace">
        <section className="network-output-panel">
          <header>
            <button type="button" disabled={Boolean(busy)} onClick={() => void loadInterfaces()}><Network />{navigator.userAgent.includes('Windows') ? 'ipconfig /all' : navigator.userAgent.includes('Linux') ? 'ip address' : 'ifconfig'}</button>
            <button type="button" disabled={Boolean(busy)} onClick={() => void loadConnections()}><Globe2 />netstat</button>
            {busy && <button type="button" onClick={() => void cancelScan()}><Square />{t('action.stop')}</button>}
            <button type="button" aria-label={t('action.copyOutput')} disabled={!output} onClick={() => void copy(output)}><ClipboardCopy /></button>
            <button type="button" aria-label={t('action.clearOutput')} disabled={!output} onClick={() => setOutput('')}><Trash2 /></button>
          </header>
          <pre>{output || t('layout.outputPlaceholder')}</pre>
        </section>
        <section className="network-actions">
          <section><h2>{t('layout.ipv4Long')}</h2><label><span>IPv4</span><input aria-label="IPv4" value={ipv4} onChange={(event) => setIpv4(event.target.value)} /></label><div><button type="button" onClick={convertAddress}>{t('action.convert')} ↓</button><button type="button" onClick={convertInteger}>↑ {t('action.convert')}</button></div><label><span>Long</span><input aria-label="Long" value={longValue} onChange={(event) => setLongValue(event.target.value)} /></label></section>
          <section><h2>{t('layout.ping')}</h2><div className="network-command-row"><Search /><input aria-label={t('layout.ping')} value={pingTarget} onChange={(event) => setPingTarget(event.target.value)} /><button type="button" disabled={Boolean(busy)} onClick={() => void runPing()}><Play />PING</button></div></section>
          <section><h2>{t('layout.ipRange')}</h2><div className="network-command-row"><Search /><input aria-label={t('layout.ipRange')} placeholder={t('layout.ipRangePlaceholder')} value={ipRange} onChange={(event) => setIpRange(event.target.value)} /><button type="button" disabled={Boolean(busy)} onClick={() => void probeRange()}><Play />{t('layout.scan')}</button></div><p>{t('layout.ipRangeHint')}</p></section>
          <section><h2>{t('layout.portScan')}</h2><div className="network-command-row"><Search /><input aria-label={t('layout.portHostPlaceholder')} placeholder={t('layout.portHostPlaceholder')} value={portHost} onChange={(event) => setPortHost(event.target.value)} /><input aria-label={t('layout.portSpecPlaceholder')} placeholder={t('layout.portSpecPlaceholder')} value={portSpec} onChange={(event) => setPortSpec(event.target.value)} /><button type="button" disabled={Boolean(busy)} onClick={() => void scanTarget()}><Play />{t('layout.scan')}</button></div><p>{t('layout.portScanHint')}</p></section>
          <section><h2>{t('layout.resolve')}</h2><div className="network-command-row"><Search /><input aria-label={t('layout.resolve')} value={resolveHost} onChange={(event) => setResolveHost(event.target.value)} /><button type="button" disabled={Boolean(busy)} onClick={() => void resolveTarget()}><Play />{t('layout.resolveAction')}</button></div></section>
          <section><h2>{t('layout.whois')}</h2><div className="network-command-row"><Search /><input aria-label={t('layout.whois')} value={whoisTarget} onChange={(event) => setWhoisTarget(event.target.value)} /><button type="button" disabled={Boolean(busy)} onClick={() => void queryWhois()}><Play />{t('layout.query')}</button></div></section>
          <section><h2>{t('layout.dns')}</h2><button type="button" disabled={Boolean(busy)} onClick={() => void flushDns()}><RefreshCw />{t('action.flushDns')}</button></section>
          <section><h2>{t('layout.localAddresses')}</h2><div className="network-local-addresses"><label><span>IPv4</span><textarea readOnly aria-label="IPv4" value={interfaces.flatMap((item) => item.addresses).filter((address) => address.includes('.')).join('\n')} /></label><label><span>IPv6</span><textarea readOnly aria-label="IPv6" value={interfaces.flatMap((item) => item.addresses).filter((address) => address.includes(':')).join('\n')} /></label></div><button type="button" aria-label={t('action.refresh')} onClick={() => void loadInterfaces()}><RefreshCw /></button></section>
        </section>
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

function formatBytes(value: number): string {
  if (value < 1024) return `${value} B`
  if (value < 1024 ** 2) return `${(value / 1024).toFixed(1)} KiB`
  if (value < 1024 ** 3) return `${(value / 1024 ** 2).toFixed(1)} MiB`
  return `${(value / 1024 ** 3).toFixed(2)} GiB`
}
