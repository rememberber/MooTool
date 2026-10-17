import { lazy, Suspense, useEffect } from 'react'
import { SettingsProvider, useSettings } from '@/features/settings/SettingsProvider'
import { ToastProvider, useToast } from '@/shared/feedback/ToastProvider'
import { DesktopDialogProvider } from '@/shared/feedback/DesktopDialogProvider'
import { I18nProvider, useI18n } from '@/shared/i18n/I18nProvider'
import { useSystemTheme } from '@/shared/theme/useSystemTheme'

const Workbench = lazy(() => import('@/features/workbench/Workbench').then(module => ({ default: module.Workbench })))
const ToolWindow = lazy(() => import('@/features/workbench/ToolWindow').then(module => ({ default: module.ToolWindow })))
const ScreenColorPickerOverlay = lazy(() => import('@/features/color/ScreenColorPickerOverlay').then(module => ({ default: module.ScreenColorPickerOverlay })))
const ScreenCaptureOverlay = lazy(() => import('@/features/image/ScreenCaptureOverlay').then(module => ({ default: module.ScreenCaptureOverlay })))
const HttpResponseWindow = lazy(() => import('@/features/http/HttpResponseWindow').then(module => ({ default: module.HttpResponseWindow })))

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
  const isScreenOverlay = windowType === 'capture' || windowType === 'color-picker'
  const { settings, ready } = useSettings()
  useEscapeToDismissWindow(ready && !isScreenOverlay && settings.general.escapeHidesWindow)

  return (
    <ToastProvider>
      <DesktopDialogProvider>
        {windowType !== 'tool' && windowType !== 'http-response' && !isScreenOverlay && <UpdateNotifications />}
        <Suspense fallback={null}>{windowType === 'http-response' ? <HttpResponseWindow /> : windowType === 'capture' ? <ScreenCaptureOverlay /> : windowType === 'color-picker' ? <ScreenColorPickerOverlay /> : windowType === 'tool' ? <ToolWindow requestedToolId={params.get('toolId') ?? ''} /> : <Workbench />}</Suspense>
      </DesktopDialogProvider>
    </ToastProvider>
  )
}

function useEscapeToDismissWindow(enabled: boolean): void {
  useEffect(() => {
    if (!enabled) return
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape' || event.repeat || event.metaKey || event.ctrlKey || event.altKey || event.shiftKey) return

      // Dialogs, menus and other transient UI own the first Escape press. Their
      // handlers run during the same dispatch and may also call preventDefault.
      const transientUiOpen = document.querySelector('[role="dialog"], [role="menu"], [aria-modal="true"], details[open]') !== null
      if (transientUiOpen) return

      window.setTimeout(() => {
        if (!event.defaultPrevented) void window.mootool.dismissWindow()
      }, 0)
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [enabled])
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
