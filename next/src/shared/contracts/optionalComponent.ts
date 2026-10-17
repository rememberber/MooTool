import type { UpdateDownload } from './update'

export type ComponentFile = { path: string; bytes: number; sha256: string }
export type ComponentManifest = {
  schema: 1
  target: string
  version: string
  download: UpdateDownload
  files: ComponentFile[]
}
export type ComponentState = {
  status: 'missing' | 'downloading' | 'restart' | 'installed' | 'error'
  percent: number
  total: number
}
