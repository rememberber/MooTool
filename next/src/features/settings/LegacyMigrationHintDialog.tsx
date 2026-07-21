import { Settings } from 'lucide-react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function LegacyMigrationHintDialog() {
  const { t } = useI18n()
  const { ready, settings, updateSettings } = useSettings()
  const open = ready && !settings.general.legacyMigrationHintDismissed

  function dismiss(): void {
    void updateSettings({ general: { legacyMigrationHintDismissed: true } }).catch(() => undefined)
  }

  function openMigrationSettings(): void {
    dismiss()
    void window.mootool.openSettings('data')
  }

  return (
    <Dialog
      title={t('app.migrationHint.title')}
      open={open}
      width={480}
      onClose={dismiss}
      footer={(
        <>
          <button className="settings-command settings-command--quiet" type="button" onClick={dismiss}>
            {t('app.migrationHint.dismiss')}
          </button>
          <button className="settings-command" type="button" onClick={openMigrationSettings}>
            <Settings size={14} />{t('app.migrationHint.openSettings')}
          </button>
        </>
      )}
    >
      <p className="legacy-migration-confirm">{t('app.migrationHint.body')}</p>
    </Dialog>
  )
}
