import { invoke } from '@tauri-apps/api/core'
import type { NetworkConnectionInfo, NetworkRangeScanResult, NetworkToolsApi, NetworkInterfaceInfo, PingResult, PortScanResult, WhoisResult } from '../contracts/networkTools'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createNetworkToolsApi(invokeCommand: Invoke = invoke): NetworkToolsApi {
  return {
    interfaces: () => invokeCommand<NetworkInterfaceInfo[]>('list_network_interfaces'),
    resolve: (host) => invokeCommand<string[]>('resolve_network_host', { host }),
    scanPorts: (requestId, host, startPort, endPort, timeoutMs) => invokeCommand<PortScanResult>('scan_tcp_ports', { requestId, host, startPort, endPort, timeoutMs }),
    scanRange: (requestId, cidr, ports, timeoutMs) => invokeCommand<NetworkRangeScanResult>('scan_ipv4_range', { requestId, cidr, ports, timeoutMs }),
    cancelTask: (requestId) => invokeCommand<boolean>('cancel_network_task', { requestId }),
    whois: (query) => invokeCommand<WhoisResult>('query_network_whois', { query }),
    connections: () => invokeCommand<NetworkConnectionInfo[]>('list_network_connections'),
    flushDns: () => invokeCommand<string>('flush_network_dns_cache'),
    ping: (host) => invokeCommand<PingResult>('ping_network_host', { host })
  }
}

const browserApi: NetworkToolsApi = {
  interfaces: async () => [],
  resolve: async () => { throw new Error('Native DNS lookup requires the Tauri desktop app') },
  scanPorts: async () => { throw new Error('Native port scanning requires the Tauri desktop app') },
  scanRange: async () => { throw new Error('Native network range scanning requires the Tauri desktop app') },
  cancelTask: async () => false,
  whois: async () => { throw new Error('Native WHOIS lookup requires the Tauri desktop app') },
  connections: async () => [],
  flushDns: async () => { throw new Error('DNS cache flushing requires the Tauri desktop app') },
  ping: async () => { throw new Error('Native ping requires the Tauri desktop app') }
}

export const networkToolsApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createNetworkToolsApi()
  : browserApi
