import { useEffect } from 'react'
import { SettingsProvider } from '@/features/settings/SettingsProvider'
import { SettingsWindow } from '@/features/settings/SettingsWindow'
import { Workbench } from '@/features/workbench/Workbench'
import { ToolWindow } from '@/features/workbench/ToolWindow'
import { ToastProvider, useToast } from '@/shared/feedback/ToastProvider'
import { I18nProvider, useI18n } from '@/shared/i18n/I18nProvider'
import { useSystemTheme } from '@/shared/theme/useSystemTheme'

export function App() {
  return (
    <SettingsProvider>
      <I18nProvider>
        <ThemedApp />
      </I18nProvider>
    </SettingsProvider>
  )
}

function ThemedApp() {
  useSystemTheme()
  useEscapeToDismissWindow()
  const params = new URLSearchParams(window.location.search)
  const windowType = params.get('window')

  return (
    <ToastProvider>
      {windowType !== 'tool' && <UpdateNotifications />}
      {windowType === 'settings' ? <SettingsWindow /> : windowType === 'tool' ? <ToolWindow requestedToolId={params.get('toolId') ?? ''} /> : <Workbench />}
    </ToastProvider>
  )
}

function useEscapeToDismissWindow(): void {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape' || event.repeat || event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) return

      // Dialogs, menus and other transient UI own the first Escape press. Their
      // handlers run during the same dispatch and may also call preventDefault.
      const transientUiOpen = document.querySelector('[role="dialog"], [role="menu"], [aria-modal="true"]') !== null
      if (transientUiOpen) return

      window.setTimeout(() => {
        if (!event.defaultPrevented) void window.mootool.dismissWindow()
      }, 0)
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])
}

function UpdateNotifications() {
  const { t } = useI18n()
  const toast = useToast()
  useEffect(() => window.mootool.onUpdateCheck((event) => {
    if (event.type === 'error') {
      toast.error(t('settings.update.failed'))
      return
    }
    if (event.result.status === 'available') {
      toast.info(t('settings.update.available', { version: event.result.latestVersion }), { duration: 8000 })
    } else {
      toast.success(t('settings.update.latest'))
    }
  }), [t, toast])
  return null
}
