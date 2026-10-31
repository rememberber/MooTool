import { useEffect } from 'react'
import { useSettings } from '../features/settings/SettingsProvider'

export function shouldDismissWindowOnEscape(
  event: Pick<KeyboardEvent, 'key' | 'repeat' | 'metaKey' | 'ctrlKey' | 'altKey' | 'shiftKey' | 'defaultPrevented'>,
  transientUiOpen: boolean
): boolean {
  if (event.key !== 'Escape' || event.repeat || event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) return false
  if (transientUiOpen) return false
  return !event.defaultPrevented
}

export function EscapeHidesWindow() {
  const { settings } = useSettings()
  useEffect(() => {
    if (!settings.general.escapeHidesWindow) return
    const handleKeyDown = (event: KeyboardEvent) => {
      const transientUiOpen = document.querySelector('[role="dialog"], [role="menu"], [aria-modal="true"], details[open]') !== null
      if (!shouldDismissWindowOnEscape(event, transientUiOpen)) return
      window.setTimeout(() => {
        if (!shouldDismissWindowOnEscape(event, document.querySelector('[role="dialog"], [role="menu"], [aria-modal="true"], details[open]') !== null)) return
        if (!window.__TAURI_INTERNALS__) return
        void import('@tauri-apps/api/core').then(({ invoke }) => invoke('dismiss_escape_window')).catch(() => undefined)
      }, 0)
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [settings.general.escapeHidesWindow])
  return null
}
