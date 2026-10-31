import { useEffect, useId, useState } from 'react'
import { globalShortcutApi } from '../../platform/api/globalShortcutApi'
import { defaultAppSettings } from '../../platform/contracts/settings'
import {
  displayShortcut,
  normalizeShortcut,
  previewGlobalShortcutStatus,
  shortcutPlatform,
  validateGlobalShortcut,
  type GlobalShortcutStatus
} from '../../platform/contracts/shortcuts'
import { useI18n } from '../../app/i18n'
import type { AppSettings } from '../../platform/contracts/settings'
import { useSettings } from './SettingsProvider'

export function GlobalShortcutSettings({ baseSettings }: { baseSettings?: () => AppSettings }) {
  const { settings, save } = useSettings()
  const { t } = useI18n()
  const inputId = useId()
  const platform = shortcutPlatform()
  const [draft, setDraft] = useState(() => displayShortcut(settings.shortcuts.globalSearch, platform))
  const [status, setStatus] = useState<GlobalShortcutStatus | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const validation = validateGlobalShortcut(draft, platform)
  const savedAccelerator = normalizeShortcut(settings.shortcuts.globalSearch, platform)

  useEffect(() => {
    setDraft(displayShortcut(settings.shortcuts.globalSearch, platform))
  }, [platform, settings.shortcuts.globalSearch])

  useEffect(() => {
    if (globalShortcutApi) return
    setStatus(previewGlobalShortcutStatus(
      settings.shortcuts.globalSearchEnabled,
      settings.shortcuts.globalSearch,
      platform
    ))
  }, [platform, settings.shortcuts.globalSearch, settings.shortcuts.globalSearchEnabled])

  useEffect(() => {
    if (!globalShortcutApi) return
    let generation = 0
    let active = true
    let unsubscribe: (() => void) | undefined
    void globalShortcutApi.subscribe((next) => {
      generation += 1
      if (active) setStatus(next)
    }).then((dispose) => {
      if (active) unsubscribe = dispose
      else dispose()
    }).catch(() => {
      if (active) setError(t('settings.saveError'))
    })
    void globalShortcutApi.getStatus().then((next) => {
      if (active && generation === 0) setStatus(next)
    }).catch(() => {
      if (active) setError(t('settings.saveError'))
    })
    return () => {
      active = false
      unsubscribe?.()
    }
  }, [t])

  async function persist(value: string, enabled = settings.shortcuts.globalSearchEnabled): Promise<void> {
    if (busy || validateGlobalShortcut(value, platform).state !== 'registered') return
    setBusy(true)
    setError('')
    try {
      const base = baseSettings?.() ?? settings
      await save({
        ...base,
        shortcuts: {
          ...base.shortcuts,
          globalSearch: value.trim(),
          globalSearchEnabled: enabled
        }
      })
      if (globalShortcutApi) setStatus(await globalShortcutApi.getStatus())
    } catch {
      setError(t('settings.saveError'))
    } finally {
      setBusy(false)
    }
  }

  async function toggle(): Promise<void> {
    if (!settings.shortcuts.globalSearchEnabled) {
      await persist(draft, true)
      return
    }
    setBusy(true)
    setError('')
    try {
      const base = baseSettings?.() ?? settings
      await save({
        ...base,
        shortcuts: { ...base.shortcuts, globalSearchEnabled: false }
      })
      if (globalShortcutApi) setStatus(await globalShortcutApi.getStatus())
    } catch {
      setError(t('settings.saveError'))
    } finally {
      setBusy(false)
    }
  }

  async function retry(): Promise<void> {
    if (!globalShortcutApi) return
    setBusy(true)
    setError('')
    try {
      setStatus(await globalShortcutApi.retry())
    } catch {
      setError(t('settings.saveError'))
    } finally {
      setBusy(false)
    }
  }

  const statusText = busy || !status
    ? t('settings.globalShortcut.loading')
    : t(`settings.globalShortcut.${status.state}`, { shortcut: displayShortcut(status.accelerator, platform) })

  return (
    <div className="global-shortcut-settings">
      <label className="settings-row">
        <span>{t('settings.globalShortcut.enable')}</span>
        <input
          type="checkbox"
          role="switch"
          aria-label={t('settings.globalShortcut.enable')}
          checked={settings.shortcuts.globalSearchEnabled}
          disabled={busy || (!settings.shortcuts.globalSearchEnabled && validation.state !== 'registered')}
          onChange={() => void toggle()}
        />
      </label>
      <p className="global-shortcut-settings__hint">{t('settings.globalShortcut.description')}</p>
      <label className="global-shortcut-settings__combination" htmlFor={inputId}>{t('settings.globalShortcut.combination')}</label>
      <input
        id={inputId}
        value={draft}
        disabled={busy}
        spellCheck={false}
        aria-invalid={validation.state !== 'registered'}
        aria-describedby={`${inputId}-hint`}
        onChange={(event) => {
          setDraft(event.target.value)
          setError('')
        }}
        onKeyDown={(event) => {
          if (event.key === 'Enter') {
            event.preventDefault()
            void persist(draft)
          }
        }}
      />
      <p id={`${inputId}-hint`} className="global-shortcut-settings__hint">{t('settings.globalShortcut.example')}</p>
      {validation.state !== 'registered' && (
        <p className="global-shortcut-settings__error" role="alert">{t(`settings.globalShortcut.${validation.state}`)}</p>
      )}
      <div className="global-shortcut-settings__actions">
        <button
          className="primary-button"
          type="button"
          disabled={busy || validation.state !== 'registered' || validation.accelerator === savedAccelerator}
          onClick={() => void persist(draft)}
        >
          {t('settings.globalShortcut.save')}
        </button>
        <button
          className="secondary-button"
          type="button"
          disabled={busy}
          onClick={() => void persist(defaultAppSettings().shortcuts.globalSearch)}
        >
          {t('settings.globalShortcut.reset')}
        </button>
        {status?.state === 'unavailable' && (
          <button className="secondary-button" type="button" disabled={busy} onClick={() => void retry()}>
            {t('settings.globalShortcut.retry')}
          </button>
        )}
      </div>
      <p
        role="status"
        className={status && ['invalid', 'conflict', 'unavailable'].includes(status.state)
          ? 'global-shortcut-settings__error'
          : 'global-shortcut-settings__hint'}
      >
        {statusText}
      </p>
      {error && <p className="global-shortcut-settings__error" role="alert">{error}</p>}
    </div>
  )
}
