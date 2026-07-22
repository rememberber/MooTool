export type ImageAssetSummary = {
  name: string
  size: number
  width: number
  height: number
  modifiedTime: string
}

export type ImageAsset = ImageAssetSummary & {
  dataUrl: string
}

export type SaveImageAssetInput = {
  name: string
  dataUrl: string
}

export type RenameImageAssetInput = {
  name: string
  nextName: string
}

export type ScreenCapture = {
  id: string
  name: string
  width: number
  height: number
  dataUrl: string
}

export type ScreenCaptureRect = {
  x: number
  y: number
  width: number
  height: number
}

export type ScreenCaptureOverlayData = {
  displayId: string
  displayName: string
  width: number
  height: number
  dataUrl: string
}

export type ScreenCaptureResult = {
  width: number
  height: number
  dataUrl: string
}
