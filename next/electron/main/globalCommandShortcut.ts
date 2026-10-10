import { validateGlobalShortcut, type GlobalShortcutStatus } from '../../src/shared/contracts/shortcuts'

type ShortcutApi = {
  register: (accelerator: string, callback: () => void) => boolean
  unregister: (accelerator: string) => void
  isRegistered: (accelerator: string) => boolean
}

export class GlobalCommandShortcut {
  private active: string | null = null
  private status: GlobalShortcutStatus = { state: 'disabled', accelerator: '' }

  constructor(private readonly api: ShortcutApi, private readonly openPalette: () => void, private readonly platform: string) {}

  getStatus(): GlobalShortcutStatus { return { ...this.status } }

  apply(enabled: boolean, configured: string): GlobalShortcutStatus {
    const validation = validateGlobalShortcut(configured, this.platform)
    if (enabled && validation.state === 'registered' && this.active === validation.accelerator && this.api.isRegistered(this.active)) {
      this.status = validation
      return this.getStatus()
    }
    this.dispose()
    if (!enabled) this.status = { state: 'disabled', accelerator: configured }
    else if (validation.state !== 'registered') this.status = validation
    else {
      try {
        if (this.api.register(validation.accelerator, this.openPalette)) {
          this.active = validation.accelerator
          this.status = validation
        } else this.status = { ...validation, state: 'unavailable' }
      } catch { this.status = { ...validation, state: 'unavailable' } }
    }
    return this.getStatus()
  }

  dispose(): void {
    if (this.active) this.api.unregister(this.active)
    this.active = null
    this.status = { state: 'disabled', accelerator: this.status.accelerator }
  }
}
