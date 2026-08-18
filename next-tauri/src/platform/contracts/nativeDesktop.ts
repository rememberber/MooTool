import type { ImageAssetSummary } from './image'

export interface ScreenCaptureResult {
  assets: ImageAssetSummary[]
  monitorCount: number
}

export interface ScreenColorSample {
  hex: string
  red: number
  green: number
  blue: number
  x: number
  y: number
}

export interface DisplaySleepStatus {
  active: boolean
  owned: boolean
}

export interface NativeDesktopApi {
  captureDisplays(delayMs?: number): Promise<ScreenCaptureResult>
  sampleScreenColor(delayMs?: number): Promise<ScreenColorSample>
  getDisplaySleepStatus(owner: string): Promise<DisplaySleepStatus>
  setDisplaySleepPrevention(owner: string, enabled: boolean): Promise<DisplaySleepStatus>
}
