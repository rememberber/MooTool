import { invoke } from '@tauri-apps/api/core'
import { open } from '@tauri-apps/plugin-dialog'
import type {
  DiagnosticsApi,
  DiagnosticsExportResult,
  EnvironmentVariable,
  FrontendErrorReport,
  SystemSnapshot
} from '../contracts/diagnostics'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createDiagnosticsApi(invokeCommand: Invoke = invoke): DiagnosticsApi {
  return {
    environment: (revealSensitive) => invokeCommand<EnvironmentVariable[]>(
      'get_environment_variables',
      { revealSensitive }
    ),
    system: () => invokeCommand<SystemSnapshot>('get_system_snapshot'),
    reportError: (report: FrontendErrorReport) => invokeCommand<void>(
      'report_frontend_error',
      { report }
    ),
    chooseExportDirectory: async (defaultDirectory) => normalizeSelection(await open({
      directory: true,
      multiple: false,
      defaultPath: defaultDirectory || undefined,
      title: '选择 MooTool Next Tauri 诊断包保存目录'
    })),
    exportBundle: (destinationDirectory) => invokeCommand<DiagnosticsExportResult>(
      'export_diagnostics_bundle',
      { destinationDirectory }
    )
  }
}

function createBrowserDiagnosticsApi(): DiagnosticsApi {
  return {
    environment: async () => [
      { name: 'MODE', value: import.meta.env.MODE, sensitive: false },
      { name: 'TAURI_PREVIEW', value: 'false', sensitive: false }
    ],
    system: async () => ({
      osName: navigator.platform || 'Browser',
      osVersion: 'Preview',
      kernelVersion: 'Unavailable',
      hostName: location.hostname || 'localhost',
      architecture: navigator.userAgent.includes('arm64') ? 'arm64' : 'unknown',
      cpuBrand: 'Unavailable in browser preview',
      physicalCores: 0,
      logicalCores: navigator.hardwareConcurrency || 0,
      totalMemoryBytes: 0,
      availableMemoryBytes: 0,
      processMemoryBytes: 0,
      uptimeSeconds: Math.round(performance.now() / 1000),
      cpuUsagePercent: 0,
      cpuFrequencyMhz: 0,
      totalSwapBytes: 0,
      usedSwapBytes: 0,
      disks: [],
      networkInterfaces: []
    }),
    reportError: async () => undefined,
    chooseExportDirectory: async () => null,
    exportBundle: async () => { throw new Error('诊断导出需要在 Tauri 桌面应用中运行') }
  }
}

function normalizeSelection(value: string | string[] | null): string | null {
  return Array.isArray(value) ? value[0] ?? null : value
}

export const diagnosticsApi: DiagnosticsApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createDiagnosticsApi()
  : createBrowserDiagnosticsApi()
