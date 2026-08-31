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
  cancelled: boolean
}

export interface NetworkHostProbe {
  address: string
  openPorts: number[]
}

export interface NetworkRangeScanResult {
  cidr: string
  scannedHosts: number
  reachableHosts: NetworkHostProbe[]
  durationMs: number
  cancelled: boolean
}

export interface WhoisResult {
  query: string
  server: string
  output: string
  durationMs: number
}

export interface NetworkConnectionInfo {
  protocol: string
  localAddress: string
  remoteAddress: string
  state: string
  process: string
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
  scanPorts(requestId: string, host: string, startPort: number, endPort: number, timeoutMs: number): Promise<PortScanResult>
  scanRange(requestId: string, cidr: string, ports: number[], timeoutMs: number): Promise<NetworkRangeScanResult>
  cancelTask(requestId: string): Promise<boolean>
  whois(query: string): Promise<WhoisResult>
  connections(): Promise<NetworkConnectionInfo[]>
  flushDns(): Promise<string>
  ping(host: string): Promise<PingResult>
}
