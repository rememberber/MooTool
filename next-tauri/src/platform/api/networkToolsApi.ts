import { invoke } from '@tauri-apps/api/core'
import type { NetworkToolsApi, NetworkInterfaceInfo, PingResult, PortScanResult } from '../contracts/networkTools'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createNetworkToolsApi(invokeCommand: Invoke = invoke): NetworkToolsApi {
  return {
    interfaces: () => invokeCommand<NetworkInterfaceInfo[]>('list_network_interfaces'),
    resolve: (host) => invokeCommand<string[]>('resolve_network_host', { host }),
    scanPorts: (host, startPort, endPort, timeoutMs) => invokeCommand<PortScanResult>('scan_tcp_ports', { host, startPort, endPort, timeoutMs }),
    ping: (host) => invokeCommand<PingResult>('ping_network_host', { host })
  }
}

const browserApi: NetworkToolsApi = {
  interfaces: async () => [],
  resolve: async () => { throw new Error('Native DNS lookup requires the Tauri desktop app') },
  scanPorts: async () => { throw new Error('Native port scanning requires the Tauri desktop app') },
  ping: async () => { throw new Error('Native ping requires the Tauri desktop app') }
}

export const networkToolsApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createNetworkToolsApi()
  : browserApi
