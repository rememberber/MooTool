import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { useI18n } from '@/shared/i18n/I18nProvider'

type DialogOptions = {
  title?: string
  confirmLabel?: string
  danger?: boolean
}

type PromptOptions = DialogOptions & {
  defaultValue?: string
}

type ConfirmRequest = {
  id: number
  type: 'confirm'
  message: string
  options: DialogOptions
  resolve: (value: boolean) => void
}

type PromptRequest = {
  id: number
  type: 'prompt'
  message: string
  options: PromptOptions
  value: string
  resolve: (value: string | null) => void
}

type DesktopDialogRequest = ConfirmRequest | PromptRequest

type DesktopDialogApi = {
  confirm: (message: string, options?: DialogOptions) => Promise<boolean>
  prompt: (message: string, options?: PromptOptions) => Promise<string | null>
}

const DesktopDialogContext = createContext<DesktopDialogApi | null>(null)

export function DesktopDialogProvider({ children }: { children: ReactNode }) {
  const { t } = useI18n()
  const [request, setRequest] = useState<DesktopDialogRequest | null>(null)
  const requestRef = useRef<DesktopDialogRequest | null>(null)
  const queueRef = useRef<DesktopDialogRequest[]>([])
  const nextIdRef = useRef(1)

  const display = useCallback((nextRequest: DesktopDialogRequest) => {
    if (requestRef.current) {
      queueRef.current.push(nextRequest)
      return
    }
    requestRef.current = nextRequest
    setRequest(nextRequest)
  }, [])

  const confirm = useCallback((message: string, options: DialogOptions = {}) => new Promise<boolean>((resolve) => {
    display({ id: nextIdRef.current++, type: 'confirm', message, options, resolve })
  }), [display])

  const prompt = useCallback((message: string, options: PromptOptions = {}) => new Promise<string | null>((resolve) => {
    display({ id: nextIdRef.current++, type: 'prompt', message, options, value: options.defaultValue ?? '', resolve })
  }), [display])

  const settle = useCallback((value?: boolean | string | null) => {
    const current = requestRef.current
    if (!current) return
    if (current.type === 'confirm') current.resolve(value === true)
    else current.resolve(typeof value === 'string' ? value : null)

    const nextRequest = queueRef.current.shift() ?? null
    requestRef.current = nextRequest
    setRequest(nextRequest)
  }, [])

  useEffect(() => () => {
    const pending = [requestRef.current, ...queueRef.current].filter((item): item is DesktopDialogRequest => Boolean(item))
    pending.forEach((item) => {
      if (item.type === 'confirm') item.resolve(false)
      else item.resolve(null)
    })
    requestRef.current = null
    queueRef.current = []
  }, [])

  const api = useMemo<DesktopDialogApi>(() => ({ confirm, prompt }), [confirm, prompt])
  const title = request?.options.title ?? (request?.type === 'prompt' ? request.message : t('common.confirm'))
  const confirmLabel = request?.options.confirmLabel ?? (request?.type === 'prompt' ? t('common.action.save') : t('common.yes'))

  return (
    <DesktopDialogContext.Provider value={api}>
      {children}
      <Dialog
        title={title}
        open={Boolean(request)}
        width={420}
        onClose={() => settle(null)}
        footer={request && (
          <>
            <button className="dialog-button" type="button" onClick={() => settle(null)}>{t('common.cancel')}</button>
            <button
              className={request.options.danger ? 'dialog-button dialog-button--danger desktop-dialog__confirm' : 'dialog-button dialog-button--primary'}
              type="button"
              autoFocus={request.type === 'confirm'}
              onClick={() => settle(request.type === 'prompt' ? request.value : true)}
            >
              {confirmLabel}
            </button>
          </>
        )}
      >
        {request?.type === 'confirm' ? (
          <p className="desktop-dialog__message">{request.message}</p>
        ) : request?.type === 'prompt' ? (
          <label className="dialog-field desktop-dialog__field">
            <span className="visually-hidden">{request.message}</span>
            <input
              key={request.id}
              autoFocus
              value={request.value}
              aria-label={request.message}
              onFocus={(event) => event.currentTarget.select()}
              onChange={(event) => {
                const value = event.target.value
                setRequest((current) => {
                  if (!current || current.type !== 'prompt') return current
                  const nextRequest = { ...current, value }
                  requestRef.current = nextRequest
                  return nextRequest
                })
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
                  event.preventDefault()
                  settle(requestRef.current?.type === 'prompt' ? requestRef.current.value : null)
                }
              }}
            />
          </label>
        ) : null}
      </Dialog>
    </DesktopDialogContext.Provider>
  )
}

export function useDesktopDialog(): DesktopDialogApi {
  const context = useContext(DesktopDialogContext)
  if (!context) throw new Error('useDesktopDialog must be used within DesktopDialogProvider')
  return context
}
