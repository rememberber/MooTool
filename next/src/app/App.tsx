import { useEffect } from 'react'
import { SettingsProvider } from '@/features/settings/SettingsProvider'
import { SettingsWindow } from '@/features/settings/SettingsWindow'
import { Workbench } from '@/features/workbench/Workbench'
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
  const isSettingsWindow = new URLSearchParams(window.location.search).get('window') === 'settings'

  return (
    <ToastProvider>
      <UpdateNotifications />
      {isSettingsWindow ? <SettingsWindow /> : <Workbench />}
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
