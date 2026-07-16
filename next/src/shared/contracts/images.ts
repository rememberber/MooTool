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
