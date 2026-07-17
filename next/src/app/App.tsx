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
  const params = new URLSearchParams(window.location.search)
  const windowType = params.get('window')

  return (
    <ToastProvider>
      {windowType !== 'tool' && <UpdateNotifications />}
      {windowType === 'settings' ? <SettingsWindow /> : windowType === 'tool' ? <ToolWindow requestedToolId={params.get('toolId') ?? ''} /> : <Workbench />}
    </ToastProvider>
  )
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
