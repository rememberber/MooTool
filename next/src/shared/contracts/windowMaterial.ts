import type { WindowMaterial } from './settings'

export type ResolvedMaterial = Exclude<WindowMaterial, 'auto'>
export type MaterialReason = 'selected' | 'accessibility' | 'platform' | 'system-version' | 'extension' | 'native-failure' | 'initializing'
export interface WindowMaterialStatus {
  requested: WindowMaterial
  effective: ResolvedMaterial
  reason: MaterialReason
  pendingRestart: boolean
}
