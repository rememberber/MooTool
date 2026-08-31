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
  cpuBrand: string
  physicalCores: number
  logicalCores: number
  totalMemoryBytes: number
  availableMemoryBytes: number
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
  totalBytes: number
  availableBytes: number
  removable: boolean
}

export interface SystemNetworkInterface {
  name: string
  addresses: string[]
  macAddress: string
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
  chooseExportDirectory(): Promise<string | null>
  exportBundle(destinationDirectory: string): Promise<DiagnosticsExportResult>
}
