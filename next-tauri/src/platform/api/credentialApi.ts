import { invoke } from '@tauri-apps/api/core'

export interface ProxyCredentialStatus {
  secureStore: string
  stored: boolean
}

interface CredentialApi {
  getProxyStatus(): Promise<ProxyCredentialStatus>
  setProxyPassword(password: string): Promise<ProxyCredentialStatus>
}

let browserProxyPassword = ''

const browserApi: CredentialApi = {
  getProxyStatus: async () => ({ stored: Boolean(browserProxyPassword), secureStore: 'memory-only preview' }),
  setProxyPassword: async (password) => {
    if (new TextEncoder().encode(password).length > 4_096) throw new Error('proxy password cannot exceed 4096 bytes')
    browserProxyPassword = password
    return { stored: Boolean(password), secureStore: 'memory-only preview' }
  }
}

export const credentialApi: CredentialApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? {
      getProxyStatus: () => invoke<ProxyCredentialStatus>('get_proxy_credential_status'),
      setProxyPassword: (password) => invoke<ProxyCredentialStatus>('set_proxy_password', { password })
    }
  : browserApi
