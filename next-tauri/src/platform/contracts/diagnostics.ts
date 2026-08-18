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
