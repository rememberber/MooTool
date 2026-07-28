import { ClipboardCopy, Globe2, Network, Play, RefreshCw, Search, Square, Trash2 } from 'lucide-react'
import { useEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import type { LocalAddressSnapshot, NetworkAction } from '@/shared/contracts/system'
import { useSettings } from '@/features/settings/SettingsProvider'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { ipv4ToLong, longToIpv4 } from './netTools'

export function NetTool() {
  const { t } = useI18n()
  const { settings } = useSettings()
  const actions = useToolActions('net')
  const [output, setOutput] = useState('')
  const [running, setRunning] = useState<NetworkAction | null>(null)
  const [ipv4, setIpv4] = useState('127.0.0.1')
  const [longValue, setLongValue] = useState('2130706433')
  const [pingTarget, setPingTarget] = useState('127.0.0.1')
  const [ipRange, setIpRange] = useState('192.168.10')
  const [portScanTarget, setPortScanTarget] = useState('127.0.0.1')
  const [portSpec, setPortSpec] = useState('')
  const [hostTarget, setHostTarget] = useState('localhost')
  const [whoisTarget, setWhoisTarget] = useState('example.com')
  const [addresses, setAddresses] = useState<LocalAddressSnapshot>({ ipv4: [], ipv6: [] })
  const requestId = useRef('')

  async function run(action: NetworkAction, target?: string, ports?: string): Promise<void> {
    requestId.current = `net-${Date.now()}-${Math.random().toString(36).slice(2)}`
    setRunning(action)
    setOutput(t('net.running'))
    try {
      const result = await window.mootool.runNetworkCommand({ requestId: requestId.current, action, target, ports, timeoutMs: settings.network.requestTimeoutMs })
      setOutput(result.output || t('net.noOutput'))
      if (result.errorCode && result.errorCode !== 'ABORTED') actions.toast.error(t(`net.error.${result.errorCode}` as 'net.error.COMMAND_FAILED'))
    } catch (error) { actions.reportError(error) } finally { setRunning(null) }
  }

  async function stop(): Promise<void> {
    await window.mootool.cancelSystemCommand(requestId.current)
    setRunning(null)
  }

  async function refreshAddresses(): Promise<void> {
    try { setAddresses(await window.mootool.getLocalAddresses()) } catch (error) { actions.reportError(error) }
  }

  useEffect(() => { void refreshAddresses() }, [])

  function convertFromIp(): void {
    try { setLongValue(String(ipv4ToLong(ipv4))) } catch (error) { actions.reportError(error) }
  }

  function convertFromLong(): void {
    try { setIpv4(longToIpv4(longValue)) } catch (error) { actions.reportError(error) }
  }

  return (
    <section className="tool-page p5-tool net-tool-page">
      <ToolPageHeader title={t('net.title')} />
      <ResizableColumns className="local-tool-shell net-workspace" columns={2} defaultSizes={[1.15, 0.85]} minPaneWidths={[340, 300]} storageKey="network-workspace">
        <section className="net-output-panel"><header><button className="toolbar-button" type="button" disabled={Boolean(running)} onClick={() => { void run('interfaces') }}><Network size={14} />{window.mootool.platform === 'win32' ? 'ipconfig /all' : window.mootool.platform === 'darwin' ? 'ifconfig' : 'ip address'}</button><button className="toolbar-button" type="button" disabled={Boolean(running)} onClick={() => { void run('connections') }}><Globe2 size={14} />netstat</button><span className="p4-toolbar__spacer" />{running && <button className="toolbar-button" type="button" onClick={() => { void stop() }}><Square size={13} />{t('common.stop')}</button>}<button className="icon-button" type="button" aria-label={t('common.action.copy')} disabled={!output} onClick={() => { void actions.copy(output) }}><ClipboardCopy size={14} /></button><button className="icon-button" type="button" aria-label={t('common.action.clear')} disabled={!output} onClick={() => setOutput('')}><Trash2 size={14} /></button></header><pre data-testid="net-output">{output || t('net.outputPlaceholder')}</pre></section>
        <section className="net-actions"><NetSection title={t('net.ipv4Long')}><label><span>IPv4</span><input value={ipv4} onChange={(event) => setIpv4(event.target.value)} /></label><div className="net-inline-actions"><button className="dialog-button" type="button" onClick={convertFromIp}>{t('common.convert')} ↓</button><button className="dialog-button" type="button" onClick={convertFromLong}>↑ {t('common.convert')}</button></div><label><span>Long</span><input value={longValue} onChange={(event) => setLongValue(event.target.value)} /></label></NetSection>
          <NetSection title={t('net.ping')}><CommandRow value={pingTarget} onChange={setPingTarget} buttonLabel="PING" disabled={Boolean(running)} onRun={() => run('ping', pingTarget)} /></NetSection>
          <NetSection title={t('net.ipRangeScan')}>
            <CommandRow value={ipRange} onChange={setIpRange} buttonLabel={t('net.scan')} disabled={Boolean(running)}
              placeholder={t('net.ipRangePlaceholder')} testId="net-ip-range-scan" onRun={() => run('ping-range', ipRange)} />
            <p className="net-section-hint">{t('net.ipRangeHint')}</p>
          </NetSection>
          <NetSection title={t('net.portScan')}>
            <PortScanRow host={portScanTarget} ports={portSpec} onHostChange={setPortScanTarget} onPortsChange={setPortSpec}
              disabled={Boolean(running)} hostPlaceholder={t('net.portScanTargetPlaceholder')}
              portsPlaceholder={t('net.portScanPortsPlaceholder')} buttonLabel={t('net.scan')}
              onRun={() => run('port-scan', portScanTarget, portSpec)} />
            <p className="net-section-hint">{t('net.portScanHint')}</p>
          </NetSection>
          <NetSection title={t('net.resolve')}><CommandRow value={hostTarget} onChange={setHostTarget} buttonLabel={t('net.resolveAction')} disabled={Boolean(running)} testId="net-resolve" onRun={() => run('resolve', hostTarget)} /></NetSection>
          <NetSection title={t('net.whois')}><CommandRow value={whoisTarget} onChange={setWhoisTarget} buttonLabel={t('net.query')} disabled={Boolean(running)} onRun={() => run('whois', whoisTarget)} /></NetSection>
          <NetSection title={t('net.dns')}><button className="dialog-button" type="button" disabled={Boolean(running)} onClick={() => { void run('flush-dns') }}><RefreshCw size={14} />{t('net.flushDns')}</button></NetSection>
          <NetSection title={t('net.localAddresses')}><div className="local-address-columns"><div><span>IPv4</span><TextCodeEditor className="local-address-editor" ariaLabel="IPv4" readOnly value={addresses.ipv4.join('\n')} /></div><div><span>IPv6</span><TextCodeEditor className="local-address-editor" ariaLabel="IPv6" readOnly value={addresses.ipv6.join('\n')} /></div></div><button className="icon-button" type="button" aria-label={t('common.refresh')} onClick={() => { void refreshAddresses() }}><RefreshCw size={14} /></button></NetSection>
        </section>
      </ResizableColumns>
    </section>
  )
}

function NetSection({ title, children }: { title: string; children: ReactNode }) {
  return <section className="net-section"><h2>{title}</h2>{children}</section>
}

function CommandRow({ value, onChange, buttonLabel, disabled, placeholder, testId, onRun }: { value: string; onChange: (value: string) => void; buttonLabel: string; disabled: boolean; placeholder?: string; testId?: string; onRun: () => Promise<void> }) {
  return <div className="net-command-row"><Search size={14} /><input value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !disabled) void onRun() }} /><button className="dialog-button" data-testid={testId} type="button" disabled={disabled} onClick={() => { void onRun() }}><Play size={13} />{buttonLabel}</button></div>
}

function PortScanRow({ host, ports, onHostChange, onPortsChange, disabled, hostPlaceholder, portsPlaceholder, buttonLabel, onRun }: {
  host: string
  ports: string
  onHostChange: (value: string) => void
  onPortsChange: (value: string) => void
  disabled: boolean
  hostPlaceholder: string
  portsPlaceholder: string
  buttonLabel: string
  onRun: () => Promise<void>
}) {
  const runOnEnter = (event: KeyboardEvent<HTMLInputElement>): void => {
    if (event.key === 'Enter' && !disabled) void onRun()
  }
  return <div className="net-port-scan-row"><Search size={14} /><input value={host} placeholder={hostPlaceholder} onChange={(event) => onHostChange(event.target.value)} onKeyDown={runOnEnter} /><input value={ports} placeholder={portsPlaceholder} onChange={(event) => onPortsChange(event.target.value)} onKeyDown={runOnEnter} /><button className="dialog-button" data-testid="net-port-scan" type="button" disabled={disabled} onClick={() => { void onRun() }}><Play size={13} />{buttonLabel}</button></div>
}
