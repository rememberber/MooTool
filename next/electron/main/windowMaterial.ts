import type { BrowserWindow } from 'electron'
import type { WindowMaterial } from '../../src/shared/contracts/settings'

export type ResolvedMaterial = Exclude<WindowMaterial, 'auto'>
export interface MaterialEnvironment {
  platform: string
  systemVersion: string
  solidRequested: boolean
  glassAvailable: boolean
}

/** Auxiliary windows always remain opaque; this policy applies to the main shell. */
export function resolveWindowMaterial(mode: WindowMaterial, env: MaterialEnvironment): ResolvedMaterial {
  if (mode === 'solid' || env.solidRequested || env.platform !== 'darwin') return 'solid'
  if (mode !== 'vibrancy' && Number(env.systemVersion.split('.')[0]) >= 26 && env.glassAvailable) return 'liquid-glass'
  return 'vibrancy'
}

interface GlassModule {
  addView(handle: Buffer, options: { cornerRadius: number }): number
}
interface MaterialDependencies {
  platform: string
  systemVersion: string
  dark: () => boolean
  highContrast: () => boolean
  loadGlass: () => Promise<GlassModule>
}

// Keep the optional, macOS-only native package outside the main-process bundle.
const importOptional = new Function('specifier', 'return import(specifier)') as
  (specifier: string) => Promise<{ default: GlassModule }>
export const loadNativeGlass = async () => (await importOptional('electron-liquid-glass')).default

/** One controller per native window. Native glass has no public removal API, so
 * the user's selected mode is fixed for this window's lifetime. Accessibility
 * changes immediately mask it with an opaque surface without losing web state. */
export class WindowMaterialController {
  private solidRequested = false
  private initialized = false
  private glassAttached = false
  private glassFailed = false
  private glass: Promise<GlassModule> | undefined
  private queue: Promise<void> = Promise.resolve()
  private effective: ResolvedMaterial = 'solid'

  constructor(private win: BrowserWindow, private mode: WindowMaterial, private deps: MaterialDependencies) {}

  get material(): ResolvedMaterial { return this.effective }

  initialize(solidRequested: boolean): Promise<void> {
    this.initialized = true
    return this.setAccessibility(solidRequested)
  }

  setAccessibility(solidRequested: boolean): Promise<void> {
    this.solidRequested = solidRequested
    return this.refresh()
  }

  refresh(): Promise<void> {
    if (!this.initialized) return Promise.resolve()
    this.queue = this.queue.then(() => this.apply()).catch((error) => {
      console.warn('Window material unavailable; using solid background', error)
      if (!this.win.isDestroyed()) this.publish('solid')
    })
    return this.queue
  }

  private resolve(): ResolvedMaterial {
    return resolveWindowMaterial(this.mode, {
      ...this.deps,
      solidRequested: this.solidRequested || this.deps.highContrast(),
      glassAvailable: !this.glassFailed
    })
  }

  private async apply(): Promise<void> {
    if (this.win.isDestroyed()) return
    let material = this.resolve()
    if (material === 'liquid-glass' && !this.glassAttached) {
      try {
        this.glass ??= this.deps.loadGlass()
        const glass = await this.glass
        if (this.win.isDestroyed()) return
        // Preferences may have changed while the native module was loading.
        material = this.resolve()
        if (material === 'liquid-glass') {
          this.win.setVibrancy(null)
          this.win.setWindowButtonVisibility(true)
          const id = glass.addView(this.win.getNativeWindowHandle(), { cornerRadius: 0 })
          if (!Number.isFinite(id) || id < 0) throw new Error('Native glass attachment failed')
          this.glassAttached = true
        }
      } catch (error) {
        console.warn('Liquid Glass unavailable; falling back to vibrancy', error)
        this.glassFailed = true
        material = this.resolve()
      }
    }
    if (this.win.isDestroyed()) return
    if (this.deps.platform === 'darwin') {
      try {
        this.win.setVibrancy(material === 'vibrancy' ? 'sidebar' : null)
      } catch {
        material = 'solid'
      }
    }
    this.publish(material)
  }

  private publish(material: ResolvedMaterial): void {
    this.effective = material
    this.win.setBackgroundColor(material === 'solid'
      ? this.deps.dark() ? '#171719' : '#f7f7f8'
      : '#00000000')
    this.win.webContents.send('window:material-changed', material)
  }
}
