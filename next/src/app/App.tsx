import { Workbench } from '@/features/workbench/Workbench'
import { ToastProvider } from '@/shared/feedback/ToastProvider'
import { I18nProvider } from '@/shared/i18n/I18nProvider'
import { useSystemTheme } from '@/shared/theme/useSystemTheme'

export function App() {
  useSystemTheme()

  return (
    <I18nProvider>
      <ToastProvider>
        <Workbench />
      </ToastProvider>
    </I18nProvider>
  )
}
