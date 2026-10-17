import { useEffect, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import type { ComponentState } from '@/shared/contracts/optionalComponent'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function OcrComponentDialog({ initial, onClose }: { initial: ComponentState; onClose: () => void }) {
  const { t } = useI18n()
  const [state, setState] = useState(initial)
  const [requestError, setRequestError] = useState(false)
  useEffect(() => {
    let active = true
    const refresh = () => { void window.mootool.getOcrStatus().then(value => {
      if (active && value.component) setState(value.component)
    }).catch(() => { if (active) setRequestError(true) }) }
    refresh()
    const timer = setInterval(refresh, 500)
    return () => { active = false; clearInterval(timer) }
  }, [])
  const downloading = state.status === 'downloading'
  const ready = state.status === 'restart'
  async function download() {
    setRequestError(false)
    setState(value => ({ ...value, status: 'downloading', percent: 0 }))
    try { setState(await window.mootool.downloadOcrComponent()) }
    catch { setRequestError(true); setState(value => ({ ...value, status: 'error' })) }
  }
  return <Dialog title={t('image.ocr.component.title')} open onClose={onClose} width={480} footer={<>
    <button className="dialog-button" type="button" onClick={onClose}>{t(ready ? 'image.ocr.component.later' : 'common.close')}</button>
    <button className="dialog-button dialog-button--primary" type="button" disabled={downloading} onClick={() => {
      if (ready) void window.mootool.restartForOcrComponent().catch(() => setRequestError(true))
      else void download()
    }}>{t(ready ? 'image.ocr.component.restart' : downloading ? 'image.ocr.component.downloading' : 'image.ocr.component.download')}</button>
  </>}>
    <p>{t('image.ocr.component.intro', { size: (state.total / 1024 / 1024).toFixed(1) })}</p>
    {downloading && <><progress style={{ width: '100%' }} max={100} value={state.percent} /><p role="status">{t('image.ocr.component.progress', { percent: String(Math.floor(state.percent)) })}</p></>}
    {ready && <p role="status">{t('image.ocr.component.ready')}</p>}
    {(state.status === 'error' || requestError) && <p className="image-ocr-error" role="alert">{t('image.ocr.component.error')}</p>}
  </Dialog>
}
