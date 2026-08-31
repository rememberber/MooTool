import { invoke } from '@tauri-apps/api/core'
import { listen } from '@tauri-apps/api/event'
import { open } from '@tauri-apps/plugin-dialog'
import type {
  VaultApi,
  VaultChangedEvent,
  VaultDocument,
  VaultGitRequest,
  VaultGitDetails,
  VaultGitResult,
  VaultGitStatus,
  VaultSaveRequest,
  VaultSnapshot,
  VaultTrashResult
} from '../contracts/vault'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createVaultApi(invokeCommand: Invoke = invoke): Omit<VaultApi, 'chooseRootDirectory' | 'subscribe'> {
  return {
    snapshot: () => invokeCommand<VaultSnapshot>('get_vault_snapshot'),
    configure: (rootDirectory) => invokeCommand<VaultSnapshot>('configure_vault', { rootDirectory }),
    disconnect: () => invokeCommand<void>('disconnect_vault'),
    read: (relativePath) => invokeCommand<VaultDocument>('read_vault_document', { relativePath }),
    save: (request: VaultSaveRequest) => invokeCommand<VaultDocument>('save_vault_document', { request }),
    createDirectory: (relativePath) => invokeCommand<string>('create_vault_directory', { relativePath }),
    move: (relativePath, destinationPath, expectedFingerprint) => invokeCommand<string>(
      'move_vault_entry',
      { relativePath, destinationPath, expectedFingerprint }
    ),
    duplicate: (relativePath, destinationPath, expectedFingerprint) => invokeCommand<VaultDocument>(
      'duplicate_vault_document',
      { relativePath, destinationPath, expectedFingerprint }
    ),
    delete: (relativePath, expectedFingerprint) => invokeCommand<VaultTrashResult>(
      'delete_vault_document',
      { relativePath, expectedFingerprint }
    ),
    deleteEntry: (relativePath, expectedFingerprint) => invokeCommand<VaultTrashResult>(
      'delete_vault_entry',
      { relativePath, expectedFingerprint }
    ),
    gitStatus: () => invokeCommand<VaultGitStatus>('get_vault_git_status'),
    gitDetails: (relativePath) => invokeCommand<VaultGitDetails>('get_vault_git_details', { relativePath }),
    configureGitRemote: (remote) => invokeCommand<VaultGitStatus>('configure_vault_git_remote', { remote }),
    runGit: (request: VaultGitRequest) => invokeCommand<VaultGitResult>('run_vault_git', { request }),
    cancelGit: (requestId) => invokeCommand<boolean>('cancel_vault_git', { requestId })
  }
}

const browserSnapshot: VaultSnapshot = {
  rootPath: null,
  files: [],
  directories: [],
  git: {
    available: false,
    repository: false,
    branch: '',
    dirty: false,
    changedFiles: 0,
    ahead: 0,
    behind: 0,
    remote: ''
  }
}

const desktop = createVaultApi()

export const vaultApi: VaultApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      ...desktop,
      chooseRootDirectory: async () => normalizeSelection(await open({
        directory: true,
        multiple: false,
        title: '选择 MooTool Next Tauri JSON Vault'
      })),
      subscribe: async (listener) => listen<VaultChangedEvent>('mootool://vault-changed', (event) => listener(event.payload))
    }
  : {
      chooseRootDirectory: async () => null,
      snapshot: async () => browserSnapshot,
      configure: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      disconnect: async () => undefined,
      read: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      save: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      createDirectory: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      move: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      duplicate: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      delete: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      deleteEntry: async () => { throw new Error('JSON Vault 需要在 Tauri 桌面应用中运行') },
      gitStatus: async () => browserSnapshot.git,
      gitDetails: async () => ({ diff: '', commits: [] }),
      configureGitRemote: async () => { throw new Error('Vault Git 需要在 Tauri 桌面应用中运行') },
      runGit: async () => { throw new Error('Vault Git 需要在 Tauri 桌面应用中运行') },
      cancelGit: async () => false,
      subscribe: async () => () => undefined
    }

function normalizeSelection(value: string | string[] | null): string | null {
  return Array.isArray(value) ? value[0] ?? null : value
}
