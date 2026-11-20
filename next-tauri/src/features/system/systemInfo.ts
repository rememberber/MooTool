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
  cpuManufacturer: string
  family: string
  cpuModel: string
  baseSpeed: string
  maximumSpeed: string
  total: string
  used: string
  available: string
  active: string
  usage: string
  diskType: string
  mtu: string
  interfaceName: string
  speed: string
  status: string
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
        item(text.serial, snapshot.serial),
        item(text.manufacturer, snapshot.manufacturer),
        item(text.model, snapshot.model),
        item(text.uptime, text.uptimeValue),
        item(text.timeZone, snapshot.timeZone)
      ]
    }]
  }
  if (tab === 'cpu') {
    return [{
      title: text.processor,
      items: [
        item(text.cpuManufacturer, ''),
        item(text.brand, snapshot.cpuBrand),
        item(text.vendor, snapshot.cpuVendor),
        item(text.family, ''),
        item(text.cpuModel, ''),
        item(text.physicalCores, String(snapshot.physicalCores)),
        item(text.logicalCores, String(snapshot.logicalCores)),
        item(text.performanceCores, snapshot.performanceCores == null ? '' : String(snapshot.performanceCores)),
        item(text.efficiencyCores, snapshot.efficiencyCores == null ? '' : String(snapshot.efficiencyCores)),
        item(text.baseSpeed, ghz(snapshot.cpuFrequencyMhz)),
        item(text.maximumSpeed, ''),
        item(text.currentLoad, `${snapshot.cpuUsagePercent.toFixed(1)}%`)
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
        item(text.active, snapshot.activeMemoryBytes == null ? '' : text.bytes(snapshot.activeMemoryBytes)),
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
        item(text.filesystem, disk.fileSystem),
        item(text.diskType, disk.kind),
        item(text.total, text.bytes(disk.totalBytes)),
        item(text.used, text.bytes(Math.max(0, disk.totalBytes - disk.availableBytes))),
        item(text.available, text.bytes(disk.availableBytes)),
        item(text.usage, disk.totalBytes ? `${(Math.max(0, disk.totalBytes - disk.availableBytes) * 100 / disk.totalBytes).toFixed(1)}%` : '')
      ]
    }))
  }
  return snapshot.networkInterfaces.map((entry) => ({
    title: entry.name,
    items: [
      item(text.interfaceName, entry.name),
      item(text.diskType, ''),
      item(text.ipv4, firstAddress(entry.addresses, false)),
      item(text.ipv6, firstAddress(entry.addresses, true)),
      item(text.mac, entry.macAddress),
      item(text.mtu, entry.mtu ? String(entry.mtu) : ''),
      item(text.speed, ''),
      item(text.status, ''),
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

export function formatHardwareBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(2)} ${units[index]}`
}

export function formatHardwareUptime(seconds: number): string {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}d ${hours}h ${minutes}m`
}

function item(label: string, value: string | undefined): HardwareItem {
  const text = value?.trim() ?? ''
  return { label, value: text && text !== 'undefined' ? text : '-' }
}

function ghz(mhz: number): string {
  return mhz > 0 ? `${mhz / 1000} GHz` : ''
}

function firstAddress(addresses: string[], ipv6: boolean): string {
  return addresses
    .map((address) => address.split('/')[0] ?? address)
    .find((address) => ipv6 ? address.includes(':') : address.includes('.') && !address.includes(':')) ?? ''
}
