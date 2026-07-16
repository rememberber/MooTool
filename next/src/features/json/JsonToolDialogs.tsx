import { Copy } from 'lucide-react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { JsonPathPicker } from './JsonPathPicker'

export type InputConversion = 'xmlToJson' | 'beanToJson'

export type OutputDialogState = {
  title: string
  value: string
}

type JsonToolDialogsProps = {
  content: string
  historyOpen: boolean
  pathPickerOpen: boolean
  inputConversion: InputConversion | null
  conversionInput: string
  outputDialog: OutputDialogState | null
  onCloseHistory: () => void
  onApplyHistory: (value: string) => void
  onClosePathPicker: () => void
  onChoosePath: (path: string) => void
  onCloseInput: () => void
  onConversionInputChange: (value: string) => void
  onRunInputConversion: () => void
  onCloseOutput: () => void
  onCopyOutput: () => void
  onUseOutput: () => void
}

export function JsonToolDialogs(props: JsonToolDialogsProps) {
  const { t } = useI18n()
  return (
    <>
      <HistoryDialog funcType="json" open={props.historyOpen} onClose={props.onCloseHistory} onApply={props.onApplyHistory} />
      <JsonPathPicker content={props.content} open={props.pathPickerOpen} onClose={props.onClosePathPicker} onChoose={props.onChoosePath} />
      <Dialog
        title={props.inputConversion === 'xmlToJson' ? t('json.action.xmlToJson') : t('json.action.beanToJson')}
        open={props.inputConversion !== null}
        onClose={props.onCloseInput}
        footer={(
          <>
            <button className="dialog-button" type="button" onClick={props.onCloseInput}>{t('common.cancel')}</button>
            <button className="dialog-button dialog-button--primary" type="button" onClick={props.onRunInputConversion}>{t('json.dialog.run')}</button>
          </>
        )}
      >
        <label className="dialog-editor-label">
          <span>{t('json.dialog.input')}</span>
          <textarea spellCheck={false} value={props.conversionInput} onChange={(event) => props.onConversionInputChange(event.target.value)} />
        </label>
      </Dialog>
      <Dialog
        title={props.outputDialog?.title ?? t('json.dialog.output')}
        open={props.outputDialog !== null}
        onClose={props.onCloseOutput}
        footer={(
          <>
            <button className="dialog-button" type="button" onClick={props.onCopyOutput}><Copy size={14} />{t('json.action.copy')}</button>
            <button className="dialog-button dialog-button--primary" type="button" onClick={props.onUseOutput}>{t('json.dialog.useOutput')}</button>
          </>
        )}
      >
        <label className="dialog-editor-label">
          <span>{t('json.dialog.output')}</span>
          <textarea readOnly spellCheck={false} value={props.outputDialog?.value ?? ''} />
        </label>
      </Dialog>
    </>
  )
}
