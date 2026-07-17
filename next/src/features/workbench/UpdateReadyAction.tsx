import { RefreshCw } from 'lucide-react'
import { useState } from 'react'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useUpdateState } from '@/shared/hooks/useUpdateState'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function UpdateReadyAction() {
  const { t } = useI18n()
  const toast = useToast()
  const update = useUpdateState()
  const [installing, setInstalling] = useState(false)

  if (update.status !== 'ready' || !update.version) return null

  function install(): void {
    setInstalling(true)
    void window.mootool.installUpdate().catch(() => {
      setInstalling(false)
      toast.error(t('settings.update.installFailed'))
    })
  }

  return (
    <button className="sidebar-update-action" type="button" disabled={installing} onClick={install}>
      <RefreshCw size={17} aria-hidden="true" />
      <span>
        <strong>{t('settings.update.installRestart')}</strong>
        <small>{t('settings.update.ready', { version: update.version })}</small>
      </span>
    </button>
  )
}
