import {
  CheckCircle2,
  CircleGauge,
  Copy,
  Cpu,
  HardDrive,
  MemoryStick,
  Monitor,
  MoonStar,
  RefreshCw,
  Server,
  TriangleAlert,
  Wifi
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { nativeDesktopApi } from '../../platform/api/nativeDesktopApi'
import type { SystemSnapshot } from '../../platform/contracts/diagnostics'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { systemMessages } from './systemMessages'

type SystemMessageKey = LocalizedMessageKey<typeof systemMessages>
type SystemNotice = { key: SystemMessageKey } | { raw: string }

export function SystemSurface() {
  const { t } = useLocalizedMessages(systemMessages)
  const [snapshot, setSnapshot] = useState<SystemSnapshot>()
  const [notice, setNotice] = useState<SystemNotice>({ key: 'notice.loading' })
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)
  const [sleepPrevention, setSleepPrevention] = useState(false)
  const [sleepBusy, setSleepBusy] = useState(false)
  const memoryUsage = snapshot?.totalMemoryBytes
    ? ((snapshot.totalMemoryBytes - snapshot.availableMemoryBytes) / snapshot.totalMemoryBytes) * 100
    : 0
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      os: snapshot?.osName,
      arch: snapshot?.architecture,
      cores: snapshot?.logicalCores,
      totalMemory: snapshot?.totalMemoryBytes
    }),
    summary: snapshot
      ? t('session.summary', { os: `${snapshot.osName} ${snapshot.osVersion}`, arch: snapshot.architecture, cores: snapshot.logicalCores })
      : t('session.pending')
  }), [snapshot, t])
  const { sessionId, reportError } = useToolSessionReport('system', session.digest, session.summary)

  useEffect(() => {
    void refresh()
    void nativeDesktopApi.getDisplaySleepStatus('system-tool')
      .then((status) => setSleepPrevention(status.owned))
      .catch(() => setSleepPrevention(false))
  }, [])

  async function refresh(): Promise<void> {
    setBusy(true)
    try {
      setSnapshot(await diagnosticsApi.system())
      setNotice({ key: 'notice.refreshed' })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    } finally {
      setBusy(false)
    }
  }

  async function toggleDisplaySleep(): Promise<void> {
    setSleepBusy(true)
    try {
      const status = await nativeDesktopApi.setDisplaySleepPrevention('system-tool', !sleepPrevention)
      setSleepPrevention(status.owned)
      setNotice({ key: status.owned ? 'notice.sleepPrevented' : 'notice.sleepRestored' })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    } finally {
      setSleepBusy(false)
    }
  }

  async function copySnapshot(): Promise<void> {
    if (!snapshot) return
    try {
      await clipboardApi.writeText(JSON.stringify(snapshot, null, 2))
      setNotice({ key: 'notice.copied' })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench system-workbench">
      <header className="utility-header">
        <div><span className="eyebrow">TAURI SYSTEM SNAPSHOT</span><h1>{t('title')}</h1></div>
        <div className="system-header-actions">
          <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
          <button className="secondary-button" type="button" disabled={busy} onClick={() => void refresh()}>
            <RefreshCw />{t('action.refresh')}
          </button>
          <button className="secondary-button" type="button" disabled={!snapshot} onClick={() => void copySnapshot()}><Copy />{t('action.copy')}</button>
        </div>
      </header>

      <section className="system-grid">
        <SystemCard icon={Monitor} title={t('card.os')} primary={`${snapshot?.osName ?? '—'} ${snapshot?.osVersion ?? ''}`}>
          <Fact label="Kernel" value={snapshot?.kernelVersion} />
          <Fact label={t('fact.architecture')} value={snapshot?.architecture} />
          <Fact label={t('fact.hostname')} value={snapshot?.hostName} />
        </SystemCard>
        <SystemCard icon={Cpu} title={t('card.cpu')} primary={snapshot?.cpuBrand ?? '—'}>
          <Fact label={t('fact.physicalCores')} value={snapshot ? String(snapshot.physicalCores) : undefined} />
          <Fact label={t('fact.logicalCores')} value={snapshot ? String(snapshot.logicalCores) : undefined} />
          <Fact label={t('fact.cpuUsage')} value={snapshot ? `${snapshot.cpuUsagePercent.toFixed(1)}%` : undefined} />
          <Fact label={t('fact.cpuFrequency')} value={snapshot ? `${snapshot.cpuFrequencyMhz} MHz` : undefined} />
        </SystemCard>
        <SystemCard icon={MemoryStick} title={t('card.memory')} primary={snapshot ? formatBytes(snapshot.totalMemoryBytes) : '—'}>
          <Meter value={memoryUsage} />
          <Fact label={t('fact.used')} value={snapshot ? formatBytes(snapshot.totalMemoryBytes - snapshot.availableMemoryBytes) : undefined} />
          <Fact label={t('fact.available')} value={snapshot ? formatBytes(snapshot.availableMemoryBytes) : undefined} />
          <Fact label={t('fact.swap')} value={snapshot ? `${formatBytes(snapshot.usedSwapBytes)} / ${formatBytes(snapshot.totalSwapBytes)}` : undefined} />
        </SystemCard>
        <SystemCard icon={HardDrive} title={t('card.storage')} primary={t('count.disks', { count: snapshot?.disks.length ?? 0 })}>
          {snapshot?.disks.slice(0, 6).map((disk) => <Fact key={`${disk.name}-${disk.mountPoint}`} label={`${disk.name || disk.mountPoint} · ${disk.fileSystem}`} value={`${formatBytes(disk.totalBytes - disk.availableBytes)} / ${formatBytes(disk.totalBytes)}`} />)}
        </SystemCard>
        <SystemCard icon={Wifi} title={t('card.network')} primary={t('count.interfaces', { count: snapshot?.networkInterfaces.length ?? 0 })}>
          {snapshot?.networkInterfaces.slice(0, 8).map((item) => <Fact key={item.name} label={`${item.name} · ${item.macAddress}`} value={item.addresses.join(', ') || '—'} />)}
        </SystemCard>
        <SystemCard icon={Server} title={t('card.process')} primary={snapshot ? formatBytes(snapshot.processMemoryBytes) : '—'}>
          <Fact label={t('fact.uptime')} value={snapshot ? formatDuration(snapshot.uptimeSeconds, t) : undefined} />
          <Fact label={t('fact.source')} value="Rust sysinfo" />
        </SystemCard>
        <article className="system-overview">
          <div><HardDrive /><span>{t('overview.product')}</span><strong>com.rememberber.mootool.next.tauri</strong></div>
          <div><CircleGauge /><span>{t('overview.policy')}</span><strong>{t('overview.policyValue')}</strong></div>
          <div>
            <MoonStar />
            <span>{t('overview.sleep')}</span>
            <button
              className={sleepPrevention ? 'secondary-button system-sleep-button system-sleep-button--active' : 'secondary-button system-sleep-button'}
              type="button"
              disabled={sleepBusy || !window.__TAURI_INTERNALS__}
              aria-pressed={sleepPrevention}
              onClick={() => void toggleDisplaySleep()}
            >
              {t(sleepPrevention ? 'sleep.preventing' : 'sleep.allowed')}
            </button>
          </div>
        </article>
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

function SystemCard({ icon: Icon, title, primary, children }: {
  icon: typeof Cpu
  title: string
  primary: string
  children: React.ReactNode
}) {
  return (
    <article className="system-card">
      <header><Icon /><span>{title}</span></header>
      <strong>{primary}</strong>
      <dl>{children}</dl>
    </article>
  )
}

function Fact({ label, value = '—' }: { label: string; value?: string }) {
  return <div><dt>{label}</dt><dd>{value}</dd></div>
}

function Meter({ value }: { value: number }) {
  return <div className="system-meter"><span style={{ width: `${Math.min(100, Math.max(0, value))}%` }} /><em>{value.toFixed(1)}%</em></div>
}

function formatBytes(value: number): string {
  if (!value) return '0 B'
  const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB']
  const exponent = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1)
  return `${(value / 1024 ** exponent).toFixed(exponent > 1 ? 2 : 0)} ${units[exponent]}`
}

function formatDuration(seconds: number, t: (key: SystemMessageKey, values?: MessageValues) => string): string {
  const days = Math.floor(seconds / 86_400)
  const hours = Math.floor((seconds % 86_400) / 3_600)
  return t('duration', { days, hours })
}
