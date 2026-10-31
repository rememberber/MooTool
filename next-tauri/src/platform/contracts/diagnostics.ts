export interface EnvironmentVariable {
  name: string
  value: string
  sensitive: boolean
}

export interface SystemSnapshot {
  osName: string
  osVersion: string
  kernelVersion: string
  hostName: string
  architecture: string
  serial?: string
  manufacturer?: string
  model?: string
  timeZone?: string
  cpuBrand: string
  cpuVendor?: string
  physicalCores: number
  logicalCores: number
  performanceCores?: number | null
  efficiencyCores?: number | null
  totalMemoryBytes: number
  availableMemoryBytes: number
  activeMemoryBytes?: number | null
  processMemoryBytes: number
  uptimeSeconds: number
  cpuUsagePercent: number
  cpuFrequencyMhz: number
  totalSwapBytes: number
  usedSwapBytes: number
  disks: SystemDisk[]
  networkInterfaces: SystemNetworkInterface[]
}

export interface SystemDisk {
  name: string
  mountPoint: string
  fileSystem: string
  kind?: string
  totalBytes: number
  availableBytes: number
  removable: boolean
}

export interface SystemNetworkInterface {
  name: string
  addresses: string[]
  macAddress: string
  mtu?: number
  receivedBytes: number
  transmittedBytes: number
}

export interface FrontendErrorReport {
  code: string
  message: string
  context: string
  retryable: boolean
  stack?: string
}

export interface DiagnosticsExportResult {
  bundlePath: string
  logFileCount: number
  createdAt: number
}

export interface DiagnosticsApi {
  environment(revealSensitive: boolean): Promise<EnvironmentVariable[]>
  system(): Promise<SystemSnapshot>
  reportError(report: FrontendErrorReport): Promise<void>
  chooseExportDirectory(defaultDirectory?: string): Promise<string | null>
  exportBundle(destinationDirectory: string): Promise<DiagnosticsExportResult>
}
