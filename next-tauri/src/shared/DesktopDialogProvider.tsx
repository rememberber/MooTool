import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from 'react'
import { useI18n } from '../app/i18n'

type DialogRequest =
  | { kind: 'confirm'; message: string; dangerous: boolean; resolve(value: boolean): void }
  | { kind: 'prompt'; message: string; defaultValue: string; resolve(value: string | null): void }

interface DesktopDialogContextValue {
  confirm(message: string, options?: { dangerous?: boolean }): Promise<boolean>
  prompt(message: string, defaultValue?: string): Promise<string | null>
}

const DesktopDialogContext = createContext<DesktopDialogContextValue | undefined>(undefined)

export function DesktopDialogProvider({ children }: PropsWithChildren) {
  const { t } = useI18n()
  const [request, setRequest] = useState<DialogRequest>()
  const [input, setInput] = useState('')

  const close = useCallback((value?: string | boolean | null) => {
    if (!request) return
    if (request.kind === 'confirm') request.resolve(value === true)
    else request.resolve(typeof value === 'string' ? value : null)
    setRequest(undefined)
  }, [request])

  useEffect(() => {
    if (!request) return
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        close(null)
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [close, request])

  const value = useMemo<DesktopDialogContextValue>(() => ({
    confirm: (message, options) => new Promise((resolve) => {
      setRequest({ kind: 'confirm', message, dangerous: options?.dangerous ?? false, resolve })
    }),
    prompt: (message, defaultValue = '') => new Promise((resolve) => {
      setInput(defaultValue)
      setRequest({ kind: 'prompt', message, defaultValue, resolve })
    })
  }), [])

  return (
    <DesktopDialogContext.Provider value={value}>
      {children}
      {request && (
        <div className="shared-dialog-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) close(null)
        }}>
          <form className="shared-dialog" role="dialog" aria-modal="true" aria-labelledby="shared-dialog-title" onSubmit={(event) => {
            event.preventDefault()
            close(request.kind === 'prompt' ? input : true)
          }}>
            <h2 id="shared-dialog-title">{request.kind === 'prompt' ? t('dialog.inputTitle') : t('dialog.confirmTitle')}</h2>
            <p>{request.message}</p>
            {request.kind === 'prompt' && (
              <input autoFocus value={input} onChange={(event) => setInput(event.target.value)} />
            )}
            <footer>
              <button className="secondary-button" type="button" onClick={() => close(null)}>{t('dialog.cancel')}</button>
              <button className={request.kind === 'confirm' && request.dangerous ? 'danger-button' : 'primary-button'} type="submit">
                {t('dialog.confirm')}
              </button>
            </footer>
          </form>
        </div>
      )}
    </DesktopDialogContext.Provider>
  )
}

export function useDesktopDialog(): DesktopDialogContextValue {
  const value = useContext(DesktopDialogContext)
  if (!value) throw new Error('useDesktopDialog must be used inside DesktopDialogProvider')
  return value
}
