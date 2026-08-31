export interface NetworkInterfaceInfo {
  name: string
  addresses: string[]
  macAddress: string
  mtu: number
  receivedBytes: number
  transmittedBytes: number
}

export interface PortScanResult {
  host: string
  resolvedAddress: string
  startPort: number
  endPort: number
  openPorts: number[]
  durationMs: number
}

export interface PingResult {
  host: string
  success: boolean
  output: string
  durationMs: number
}

export interface NetworkToolsApi {
  interfaces(): Promise<NetworkInterfaceInfo[]>
  resolve(host: string): Promise<string[]>
  scanPorts(host: string, startPort: number, endPort: number, timeoutMs: number): Promise<PortScanResult>
  ping(host: string): Promise<PingResult>
}
