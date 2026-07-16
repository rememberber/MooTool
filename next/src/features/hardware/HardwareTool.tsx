import { ClipboardCopy, RefreshCw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import type { SystemInfoSectionId, SystemInfoSnapshot } from '@/shared/contracts/system'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'

const sectionIds: SystemInfoSectionId[] = ['system', 'cpu', 'memory', 'storage', 'network']

export function HardwareTool() {
  const { t } = useI18n()
  const actions = useToolActions('hardware')
  const [active, setActive] = useState<SystemInfoSectionId>('system')
  const [snapshot, setSnapshot] = useState<SystemInfoSnapshot | null>(null)
  const [loading, setLoading] = useState(false)

  async function refresh(): Promise<void> {
    setLoading(true)
    try { setSnapshot(await window.mootool.getSystemInfo()) } catch (error) { actions.reportError(error) } finally { setLoading(false) }
  }
  useEffect(() => { void refresh() }, [])
  const groups = snapshot?.sections[active] ?? []
  const plainText = groups.flatMap((group) => [`========== ${localizeHardware(group.title, t)} ==========`, ...group.items.map((item) => `${localizeHardware(item.label, t)}: ${item.value}`), '']).join('\n')
  return (
    <section className="tool-page p5-tool hardware-tool-page">
      <ToolPageHeader title={t('hardware.title')} />
      <div className="local-tool-shell hardware-workspace">
        <header><ToolTabs tabs={sectionIds.map((id) => ({ id, label: t(`hardware.tab.${id}` as 'hardware.tab.system') }))} active={active} onChange={setActive} /><div><span>{snapshot ? new Date(snapshot.collectedAt).toLocaleTimeString() : ''}</span><button className="icon-button" type="button" aria-label={t('common.action.copy')} disabled={!groups.length} onClick={() => { void actions.copy(plainText) }}><ClipboardCopy size={14} /></button><button className="icon-button" type="button" aria-label={t('common.refresh')} disabled={loading} onClick={() => { void refresh() }}><RefreshCw size={14} className={loading ? 'spin' : undefined} /></button></div></header>
        <div className="hardware-groups">{loading && !snapshot ? <div className="history-empty">{t('hardware.loading')}</div> : groups.length === 0 ? <div className="history-empty">{t('hardware.empty')}</div> : groups.map((group, groupIndex) => <section className="hardware-group" key={`${group.title}-${groupIndex}`}><h2>{localizeHardware(group.title, t)}</h2><dl>{group.items.map((item, index) => <div key={`${item.label}-${index}`}><dt>{localizeHardware(item.label, t)}</dt><dd>{item.value}</dd></div>)}</dl></section>)}</div>
      </div>
    </section>
  )
}

const hardwareMessageKeys: Record<string, MessageKey> = {
  'Operating system': 'hardware.group.operatingSystem', Processor: 'hardware.group.processor', 'Physical memory': 'hardware.group.physicalMemory',
  Platform: 'hardware.label.platform', Distribution: 'hardware.label.distribution', Kernel: 'hardware.label.kernel', Architecture: 'hardware.label.architecture',
  'Host name': 'hardware.label.hostName', Serial: 'hardware.label.serial', Manufacturer: 'hardware.label.manufacturer', Model: 'hardware.label.model',
  Uptime: 'hardware.label.uptime', 'Time zone': 'hardware.label.timeZone', Brand: 'hardware.label.brand', Vendor: 'hardware.label.vendor', Family: 'hardware.label.family',
  'Physical cores': 'hardware.label.physicalCores', 'Logical cores': 'hardware.label.logicalCores', 'Performance cores': 'hardware.label.performanceCores',
  'Efficiency cores': 'hardware.label.efficiencyCores', 'Base speed': 'hardware.label.baseSpeed', 'Maximum speed': 'hardware.label.maximumSpeed',
  'Current load': 'hardware.label.currentLoad', Total: 'hardware.label.total', Used: 'hardware.label.used', Available: 'hardware.label.available',
  Active: 'hardware.label.active', Usage: 'hardware.label.usage', 'Swap total': 'hardware.label.swapTotal', 'Swap used': 'hardware.label.swapUsed',
  Device: 'hardware.label.device', Type: 'hardware.label.type', Interface: 'hardware.label.interface', Capacity: 'hardware.label.capacity', Filesystem: 'hardware.label.filesystem',
  IPv4: 'hardware.label.ipv4', IPv6: 'hardware.label.ipv6', MAC: 'hardware.label.mac', MTU: 'hardware.label.mtu', Speed: 'hardware.label.speed',
  Status: 'hardware.label.status', Received: 'hardware.label.received', Sent: 'hardware.label.sent'
}

function localizeHardware(value: string, t: (key: MessageKey) => string): string {
  const key = hardwareMessageKeys[value]
  return key ? t(key) : value
}
