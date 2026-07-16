import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function useToolActions(funcType: string) {
  const toast = useToast()
  const { t } = useI18n()

  async function copy(value: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value)
      toast.success(t('json.notice.copied'))
    } catch {
      toast.error(t('json.notice.copyFailed'))
    }
  }

  async function saveHistory(summary: string, inputText: string, outputText: string, extraData?: string): Promise<void> {
    if (!inputText.trim() && !outputText.trim()) return
    try {
      await window.mootool.saveHistory({ funcType, summary, inputText, outputText, extraData })
    } catch {
      // Local processing should still succeed if history storage is unavailable.
    }
  }

  function reportError(error: unknown): void {
    const message = error instanceof Error ? error.message : String(error)
    toast.error(t('common.error.process', { message }))
  }

  return { copy, saveHistory, reportError, toast }
}
