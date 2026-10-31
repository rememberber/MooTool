import type { SystemSnapshot } from '../../platform/contracts/diagnostics'

export const hardwareTabs = ['system', 'cpu', 'memory', 'storage', 'network'] as const
export type HardwareTab = (typeof hardwareTabs)[number]

export interface HardwareItem {
  label: string
  value: string
}

export interface HardwareGroup {
  title: string
  items: HardwareItem[]
}

export interface HardwareText {
  operatingSystem: string
  processor: string
  physicalMemory: string
  platform: string
  distribution: string
  kernel: string
  architecture: string
  hostName: string
  uptime: string
  serial: string
  manufacturer: string
  model: string
  timeZone: string
  brand: string
  vendor: string
  physicalCores: string
  logicalCores: string
  performanceCores: string
  efficiencyCores: string
  currentLoad: string
  currentFrequency: string
  total: string
  used: string
  available: string
  active: string
  usage: string
  diskType: string
  mtu: string
  swapTotal: string
  swapUsed: string
  device: string
  capacity: string
  filesystem: string
  ipv4: string
  ipv6: string
  mac: string
  received: string
  sent: string
  uptimeValue: string
  bytes: (value: number) => string
}

export function hardwareGroups(snapshot: SystemSnapshot, tab: HardwareTab, text: HardwareText): HardwareGroup[] {
  if (tab === 'system') {
    return [{
      title: text.operatingSystem,
      items: [
        item(text.platform, snapshot.osName),
        item(text.distribution, snapshot.osVersion),
        item(text.kernel, snapshot.kernelVersion),
        item(text.architecture, snapshot.architecture),
        item(text.hostName, snapshot.hostName),
        ...present(text.serial, snapshot.serial),
        ...present(text.manufacturer, snapshot.manufacturer),
        ...present(text.model, snapshot.model),
        item(text.uptime, text.uptimeValue),
        ...present(text.timeZone, snapshot.timeZone)
      ]
    }]
  }
  if (tab === 'cpu') {
    return [{
      title: text.processor,
      items: [
        item(text.brand, snapshot.cpuBrand),
        ...present(text.vendor, snapshot.cpuVendor),
        item(text.physicalCores, String(snapshot.physicalCores)),
        item(text.logicalCores, String(snapshot.logicalCores)),
        ...present(text.performanceCores, snapshot.performanceCores == null ? '' : String(snapshot.performanceCores)),
        ...present(text.efficiencyCores, snapshot.efficiencyCores == null ? '' : String(snapshot.efficiencyCores)),
        item(text.currentLoad, `${snapshot.cpuUsagePercent.toFixed(1)}%`),
        item(text.currentFrequency, formatFrequency(snapshot.cpuFrequencyMhz))
      ]
    }]
  }
  if (tab === 'memory') {
    const used = Math.max(0, snapshot.totalMemoryBytes - snapshot.availableMemoryBytes)
    const usage = snapshot.totalMemoryBytes ? (used / snapshot.totalMemoryBytes) * 100 : 0
    return [{
      title: text.physicalMemory,
      items: [
        item(text.total, text.bytes(snapshot.totalMemoryBytes)),
        item(text.used, text.bytes(used)),
        item(text.available, text.bytes(snapshot.availableMemoryBytes)),
        ...present(text.active, snapshot.activeMemoryBytes == null ? '' : text.bytes(snapshot.activeMemoryBytes)),
        item(text.usage, `${usage.toFixed(1)}%`),
        item(text.swapTotal, text.bytes(snapshot.totalSwapBytes)),
        item(text.swapUsed, text.bytes(snapshot.usedSwapBytes))
      ]
    }]
  }
  if (tab === 'storage') {
    return snapshot.disks.map((disk) => ({
      title: disk.mountPoint || disk.name,
      items: [
        item(text.device, disk.name),
        ...present(text.diskType, disk.kind),
        item(text.capacity, text.bytes(disk.totalBytes)),
        item(text.filesystem, disk.fileSystem),
        item(text.used, text.bytes(Math.max(0, disk.totalBytes - disk.availableBytes))),
        item(text.available, text.bytes(disk.availableBytes))
      ]
    }))
  }
  return snapshot.networkInterfaces.map((entry) => ({
    title: entry.name,
    items: [
      item(text.ipv4, entry.addresses.filter((address) => address.includes('.')).join(', ')),
      item(text.ipv6, entry.addresses.filter((address) => address.includes(':')).join(', ')),
      item(text.mac, entry.macAddress),
      ...present(text.mtu, entry.mtu ? String(entry.mtu) : ''),
      item(text.received, text.bytes(entry.receivedBytes)),
      item(text.sent, text.bytes(entry.transmittedBytes))
    ]
  }))
}

export function hardwarePlainText(groups: HardwareGroup[]): string {
  return groups.flatMap((group) => [
    `========== ${group.title} ==========`,
    ...group.items.map((entry) => `${entry.label}: ${entry.value}`),
    ''
  ]).join('\n')
}

function item(label: string, value: string): HardwareItem {
  return { label, value: value || '—' }
}

function present(label: string, value: string | undefined): HardwareItem[] {
  return value ? [item(label, value)] : []
}

function formatFrequency(mhz: number): string {
  if (mhz >= 1000) return `${(mhz / 1000).toFixed(2)} GHz`
  return `${mhz} MHz`
}
