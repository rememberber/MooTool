import { invoke } from '@tauri-apps/api/core'
import type {
  DisplaySleepStatus,
  NativeDesktopApi,
  ScreenCaptureResult,
  ScreenColorSample
} from '../contracts/nativeDesktop'

type Invoke = <T>(command: string, args?: Record<string, unknown>) => Promise<T>

export function createNativeDesktopApi(invokeCommand: Invoke = invoke): NativeDesktopApi {
  return {
    captureDisplays: (delayMs = 350) => invokeCommand<ScreenCaptureResult>('capture_display_images', { delayMs }),
    sampleScreenColor: (delayMs = 1_800) => invokeCommand<ScreenColorSample>('sample_screen_color', { delayMs }),
    getDisplaySleepStatus: (owner) => invokeCommand<DisplaySleepStatus>('get_display_sleep_status', { owner }),
    setDisplaySleepPrevention: (owner, enabled) => invokeCommand<DisplaySleepStatus>(
      'set_display_sleep_prevention',
      { owner, enabled }
    )
  }
}

const unavailable = async (): Promise<never> => {
  throw new Error('该能力只在 MooTool Next Tauri 桌面运行时可用')
}

export const nativeDesktopApi: NativeDesktopApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? createNativeDesktopApi()
  : {
      captureDisplays: unavailable,
      sampleScreenColor: unavailable,
      getDisplaySleepStatus: async () => ({ active: false, owned: false }),
      setDisplaySleepPrevention: unavailable
    }
