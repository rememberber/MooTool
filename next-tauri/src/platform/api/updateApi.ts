import { Channel, invoke } from '@tauri-apps/api/core'
import type {
  ProductUpdateApi,
  ProductUpdateCheck,
  ProductUpdateEvent
} from '../contracts/update'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>
interface UpdateChannel {
  onmessage: ((event: ProductUpdateEvent) => void) | null
}
type CreateChannel = () => UpdateChannel

export function createProductUpdateApi(
  invokeCommand: Invoke = invoke,
  createChannel: CreateChannel = () => new Channel<ProductUpdateEvent>()
): ProductUpdateApi {
  return {
    check: () => invokeCommand<ProductUpdateCheck>('check_for_product_update'),
    install: (onEvent) => {
      const onEventChannel = createChannel()
      onEventChannel.onmessage = onEvent
      return invokeCommand<void>('install_product_update', { onEvent: onEventChannel })
    },
    cancel: () => invokeCommand<boolean>('cancel_product_update'),
    relaunch: () => invokeCommand<void>('relaunch_after_product_update')
  }
}

const browserProductUpdateApi: ProductUpdateApi = {
  check: async () => ({
    status: 'inactive',
    currentVersion: 'browser-preview',
    latestVersion: null,
    releaseNotes: null,
    publishedAt: null,
    releaseUrl: null
  }),
  install: async () => {
    throw new Error('浏览器预览不能安装桌面更新，请使用 Tauri 桌面应用')
  },
  cancel: async () => false,
  relaunch: async () => undefined
}

export const productUpdateApi: ProductUpdateApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createProductUpdateApi()
  : browserProductUpdateApi
