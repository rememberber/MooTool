import {
  CheckCircle2,
  Copy,
  MoonStar,
  RefreshCw,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { diagnosticsApi } from '../../platform/api/diagnosticsApi'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { nativeDesktopApi } from '../../platform/api/nativeDesktopApi'
import type { SystemSnapshot } from '../../platform/contracts/diagnostics'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { hardwareGroups, hardwarePlainText, hardwareTabs, type HardwareTab, type HardwareText } from './systemInfo'
import { systemMessages } from './systemMessages'

type SystemMessageKey = LocalizedMessageKey<typeof systemMessages>
type SystemNotice = { key: SystemMessageKey } | { raw: string }

export function SystemSurface() {
  const { t } = useLocalizedMessages(systemMessages)
  const [snapshot, setSnapshot] = useState<SystemSnapshot>()
  const [notice, setNotice] = useState<SystemNotice>({ key: 'notice.loading' })
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)
  const [tab, setTab] = useState<HardwareTab>('system')
  const [sleepPrevention, setSleepPrevention] = useState(false)
  const [sleepBusy, setSleepBusy] = useState(false)
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

  const text: HardwareText | null = snapshot ? {
    operatingSystem: t('card.os'), processor: t('card.cpu'), physicalMemory: t('card.memory'),
    platform: t('label.platform'), distribution: t('label.distribution'), kernel: t('label.kernel'),
    architecture: t('fact.architecture'), hostName: t('fact.hostname'), uptime: t('fact.uptime'),
    brand: t('label.brand'), physicalCores: t('fact.physicalCores'), logicalCores: t('fact.logicalCores'),
    currentLoad: t('fact.cpuUsage'), currentFrequency: t('fact.cpuFrequency'),
    total: t('label.total'), used: t('fact.used'), available: t('fact.available'), usage: t('label.usage'),
    swapTotal: t('label.swapTotal'), swapUsed: t('label.swapUsed'),
    device: t('label.device'), capacity: t('label.capacity'), filesystem: t('label.filesystem'),
    ipv4: 'IPv4', ipv6: 'IPv6', mac: 'MAC', received: t('label.received'), sent: t('label.sent'),
    uptimeValue: formatDuration(snapshot.uptimeSeconds, t),
    bytes: formatBytes
  } : null
  const groups = snapshot && text ? hardwareGroups(snapshot, tab, text) : []

  async function copySnapshot(): Promise<void> {
    if (!groups.length) return
    try {
      await clipboardApi.writeText(hardwarePlainText(groups))
      setNotice({ key: 'notice.copied' })
      setFailed(false)
    } catch (cause) {
      setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
    }
  }

  return (
    <main className="utility-workbench system-workbench" data-system-ready={snapshot ? 'true' : 'false'}>
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <div className="system-header-actions">
          <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
          <button className="secondary-button" type="button" disabled={busy} onClick={() => void refresh()}>
            <RefreshCw />{t('action.refresh')}
          </button>
          <button className="secondary-button" type="button" disabled={!groups.length} onClick={() => void copySnapshot()}><Copy />{t('action.copy')}</button>
          <button
            className={sleepPrevention ? 'secondary-button system-sleep-button system-sleep-button--active' : 'secondary-button system-sleep-button'}
            type="button"
            disabled={sleepBusy || !window.__TAURI_INTERNALS__}
            aria-pressed={sleepPrevention}
            onClick={() => void toggleDisplaySleep()}
          >
            <MoonStar />{t(sleepPrevention ? 'sleep.preventing' : 'sleep.allowed')}
          </button>
          </div>
      </header>
      <div className="hardware-tabs" role="tablist">
        {hardwareTabs.map((id) => <button key={id} type="button" role="tab" aria-selected={tab === id} onClick={() => setTab(id)}>{t(`tab.${id}`)}</button>)}
      </div>
      <section className="hardware-groups">
        {busy && !snapshot ? <p role="status">{t('notice.loading')}</p> : groups.length === 0 ? <p role="status">{t('empty')}</p> : groups.map((group) => (
          <section key={group.title}><h2>{group.title}</h2><dl>{group.items.map((item) => <div key={item.label}><dt>{item.label}</dt><dd>{item.value}</dd></div>)}</dl></section>
        ))}
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
