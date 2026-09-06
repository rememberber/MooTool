import { invoke } from '@tauri-apps/api/core'
import { open } from '@tauri-apps/plugin-dialog'
import type { BackupApi, BackupExportResult, BackupImportResult } from '../contracts/backup'

export const backupApi: BackupApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      chooseExportDirectory: async (defaultDirectory) => normalizeSelection(await open({ directory: true, multiple: false, defaultPath: defaultDirectory || undefined, title: '选择 MooTool Next Tauri 备份保存目录' })),
      chooseImportDirectory: async () => normalizeSelection(await open({ directory: true, multiple: false, title: '选择 MooTool Next Tauri 备份文件夹' })),
      exportTo: (destinationDirectory) => invoke<BackupExportResult>('export_product_backup', { destinationDirectory }),
      importFrom: (sourceDirectory) => invoke<BackupImportResult>('import_product_backup', { sourceDirectory })
    }
  : {
      chooseExportDirectory: async () => null,
      chooseImportDirectory: async () => null,
      exportTo: async () => { throw new Error('完整备份需要在 Tauri 桌面应用中运行') },
      importFrom: async () => { throw new Error('完整恢复需要在 Tauri 桌面应用中运行') }
    }

function normalizeSelection(value: string | string[] | null): string | null {
  return Array.isArray(value) ? value[0] ?? null : value
}
