import { invoke } from '@tauri-apps/api/core'

export interface ProxyCredentialStatus {
  secureStore: string
  stored: boolean
}

interface CredentialApi {
  getProxyStatus(): Promise<ProxyCredentialStatus>
  setProxyPassword(password: string): Promise<ProxyCredentialStatus>
  getGitStatus(): Promise<ProxyCredentialStatus>
  setGitToken(token: string): Promise<ProxyCredentialStatus>
  getDeepLStatus(): Promise<ProxyCredentialStatus>
  setDeepLApiKey(apiKey: string): Promise<ProxyCredentialStatus>
}

let browserProxyPassword = ''
let browserGitToken = ''
let browserDeepLApiKey = ''

function rememberSecret(next: string): ProxyCredentialStatus {
  if (new TextEncoder().encode(next).length > 4_096) throw new Error('secret cannot exceed 4096 bytes')
  return { stored: Boolean(next), secureStore: 'memory-only preview' }
}

const browserApi: CredentialApi = {
  getProxyStatus: async () => ({ stored: Boolean(browserProxyPassword), secureStore: 'memory-only preview' }),
  setProxyPassword: async (password) => {
    const status = rememberSecret(password)
    browserProxyPassword = password
    return status
  },
  getGitStatus: async () => ({ stored: Boolean(browserGitToken), secureStore: 'memory-only preview' }),
  setGitToken: async (token) => {
    const status = rememberSecret(token)
    browserGitToken = token
    return status
  },
  getDeepLStatus: async () => ({ stored: Boolean(browserDeepLApiKey), secureStore: 'memory-only preview' }),
  setDeepLApiKey: async (apiKey) => {
    const status = rememberSecret(apiKey)
    browserDeepLApiKey = apiKey
    return status
  }
}

const desktopApi: CredentialApi = {
  getProxyStatus: () => invoke<ProxyCredentialStatus>('get_proxy_credential_status'),
  setProxyPassword: (password) => invoke<ProxyCredentialStatus>('set_proxy_password', { password }),
  getGitStatus: () => invoke<ProxyCredentialStatus>('get_git_credential_status'),
  setGitToken: (token) => invoke<ProxyCredentialStatus>('set_git_token', { token }),
  getDeepLStatus: () => invoke<ProxyCredentialStatus>('get_deepl_credential_status'),
  setDeepLApiKey: (apiKey) => invoke<ProxyCredentialStatus>('set_deepl_api_key', { apiKey })
}

export const credentialApi: CredentialApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? desktopApi
  : browserApi
