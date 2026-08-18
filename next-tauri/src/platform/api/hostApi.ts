import { invoke } from '@tauri-apps/api/core'
import type { HostApi, SystemHostsFile } from '../contracts/host'

export const hostApi: HostApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      readSystem: () => invoke<SystemHostsFile>('read_system_hosts'),
      writeSystem: (content, expectedContent) => invoke<SystemHostsFile>('write_system_hosts', {
        content,
        expectedContent
      }),
      resolve: (host) => invoke<string[]>('resolve_host', { host })
    }
  : {
      readSystem: async () => ({
        path: '/etc/hosts (browser preview)',
        content: '# Browser preview\n127.0.0.1 localhost\n::1 localhost\n',
        writable: false
      }),
      writeSystem: async () => { throw new Error('浏览器预览不能写入系统 hosts') },
      resolve: async (host) => host === 'localhost' ? ['127.0.0.1', '::1'] : []
    }
