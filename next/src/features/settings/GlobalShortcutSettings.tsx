import { useEffect, useId, useState } from 'react'
import { defaultAppSettings } from '@/shared/contracts/settings'
import { normalizeShortcut, validateGlobalShortcut, type GlobalShortcutStatus } from '@/shared/contracts/shortcuts'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { useSettings } from './SettingsProvider'

function displayShortcut(value: string): string {
  return (normalizeShortcut(value, window.mootool.platform) ?? value)
    .replace(/\bCommand\b/g, 'Cmd').replace(/\bControl\b/g, 'Ctrl')
}

export function GlobalShortcutSettings() {
  const { settings, updateSettings } = useSettings()
  const { t } = useI18n()
  const inputId = useId()
  const [draft, setDraft] = useState(displayShortcut(settings.shortcuts.globalSearch))
  const [status, setStatus] = useState<GlobalShortcutStatus | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const validation = validateGlobalShortcut(draft, window.mootool.platform)

  useEffect(() => { setDraft(displayShortcut(settings.shortcuts.globalSearch)) }, [settings.shortcuts.globalSearch])
  useEffect(() => {
    let generation = 0
    let active = true
    const unsubscribe = window.mootool.onGlobalShortcutStatusChange((next) => {
      generation += 1
      if (active) setStatus(next)
    })
    void window.mootool.getGlobalShortcutStatus().then((next) => {
      if (active && generation === 0) setStatus(next)
    }).catch(() => { if (active) setError(t('settings.saveFailed')) })
    return () => { active = false; unsubscribe() }
  }, [t])

  async function save(value: string, enabled = settings.shortcuts.globalSearchEnabled): Promise<void> {
    if (busy || validateGlobalShortcut(value, window.mootool.platform).state !== 'registered') return
    setBusy(true)
    setError('')
    try {
      await updateSettings({ shortcuts: { globalSearch: value.trim(), globalSearchEnabled: enabled } })
      setStatus(await window.mootool.getGlobalShortcutStatus())
    } catch { setError(t('settings.saveFailed')) }
    finally { setBusy(false) }
  }

  async function toggle(): Promise<void> {
    if (!settings.shortcuts.globalSearchEnabled) { await save(draft, true); return }
    setBusy(true)
    setError('')
    try { await updateSettings({ shortcuts: { globalSearchEnabled: false } }) }
    catch { setError(t('settings.saveFailed')) }
    finally { setBusy(false) }
  }

  async function retry(): Promise<void> {
    setBusy(true)
    setError('')
    try { setStatus(await window.mootool.retryGlobalShortcut()) }
    catch { setError(t('settings.saveFailed')) }
    finally { setBusy(false) }
  }

  return <div className="global-shortcut-settings">
    <div className="setting-row">
      <label>{t('settings.globalShortcut.enable')}</label>
      <div className="setting-row__control"><button className={settings.shortcuts.globalSearchEnabled ? 'toggle toggle--checked' : 'toggle'}
        type="button" role="switch" aria-label={t('settings.globalShortcut.enable')}
        aria-checked={settings.shortcuts.globalSearchEnabled}
        disabled={busy || (!settings.shortcuts.globalSearchEnabled && validation.state !== 'registered')}
        onClick={() => void toggle()}><span /></button></div>
    </div>
    <p className="global-shortcut-settings__hint">{t('settings.globalShortcut.description')}</p>
    <label htmlFor={inputId}>{t('settings.globalShortcut.combination')}</label>
    <input id={inputId} value={draft} disabled={busy} spellCheck={false}
      aria-invalid={validation.state !== 'registered'} aria-describedby={`${inputId}-hint`}
      onChange={(event) => { setDraft(event.target.value); setError('') }}
      onKeyDown={(event) => {
        if (event.key === 'Enter') { event.preventDefault(); void save(draft) }
      }} />
    <p id={`${inputId}-hint`} className="global-shortcut-settings__hint">{t('settings.globalShortcut.example')}</p>
    {validation.state !== 'registered' && <p className="global-shortcut-settings__error" role="alert">{t(`settings.globalShortcut.${validation.state}`)}</p>}
    <div className="global-shortcut-settings__actions">
      <button className="settings-command" type="button" disabled={busy || validation.state !== 'registered' || validation.accelerator === normalizeShortcut(settings.shortcuts.globalSearch, window.mootool.platform)}
        onClick={() => void save(draft)}>{t('common.action.save')}</button>
      <button className="settings-command settings-command--quiet" type="button" disabled={busy}
        onClick={() => void save(defaultAppSettings.shortcuts.globalSearch)}>{t('settings.globalShortcut.reset')}</button>
      {status?.state === 'unavailable' && <button className="settings-command settings-command--quiet" type="button" disabled={busy}
        onClick={() => void retry()}>{t('settings.globalShortcut.retry')}</button>}
    </div>
    <p role="status" className={status && ['invalid', 'conflict', 'unavailable'].includes(status.state) ? 'global-shortcut-settings__error' : 'global-shortcut-settings__hint'}>
      {busy || !status ? t('common.loading') : t(`settings.globalShortcut.${status.state}`, { shortcut: displayShortcut(status.accelerator) })}
    </p>
    {error && <p className="global-shortcut-settings__error" role="alert">{error}</p>}
  </div>
}
