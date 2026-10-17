import { CircleAlert, CircleCheck, Info, X } from 'lucide-react'
import {
  createContext,
  use,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode
} from 'react'
import { createPortal } from 'react-dom'
import { useI18n } from '@/shared/i18n/I18nProvider'

export type ToastTone = 'info' | 'success' | 'error'

export type ToastOptions = {
  duration?: number
  tone?: ToastTone
}

type ToastItem = {
  id: number
  message: ReactNode
  tone: ToastTone
}

type ToastApi = {
  show: (message: ReactNode, options?: ToastOptions) => number
  info: (message: ReactNode, options?: Omit<ToastOptions, 'tone'>) => number
  success: (message: ReactNode, options?: Omit<ToastOptions, 'tone'>) => number
  error: (message: ReactNode, options?: Omit<ToastOptions, 'tone'>) => number
  dismiss: (id: number) => void
}

const ToastContext = createContext<ToastApi | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const { t } = useI18n()
  const [toasts, setToasts] = useState<ToastItem[]>([])
  const [timers] = useState(() => new Map<number, number>())
  const nextId = useRef(1)
  const [copiedId, setCopiedId] = useState<number | null>(null)

  const dismiss = useCallback((id: number) => {
    const timer = timers.get(id)
    if (timer !== undefined) {
      window.clearTimeout(timer)
      timers.delete(id)
    }
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [timers])

  const show = useCallback((message: ReactNode, options: ToastOptions = {}) => {
    const id = nextId.current++
    const tone = options.tone ?? 'info'
    const duration = options.duration ?? (tone === 'error' ? 0 : 3200)
    const item: ToastItem = { id, message, tone: options.tone ?? 'info' }

    setToasts((current) => {
      const duplicates = tone === 'error' && typeof message === 'string'
        ? current.filter(toast => toast.tone === 'error' && toast.message === message) : []
      for (const duplicate of duplicates) {
        const timer = timers.get(duplicate.id)
        if (timer !== undefined) { window.clearTimeout(timer); timers.delete(duplicate.id) }
      }
      const next = [...current.filter(toast => !duplicates.includes(toast)), item]
      // Never evict an unread error to make room for routine notifications.
      while (next.length > 4) {
        const index = next.findIndex(toast => toast.tone !== 'error')
        if (index < 0) break
        const [removed] = next.splice(index, 1)
        const timer = timers.get(removed.id)
        if (timer !== undefined) { window.clearTimeout(timer); timers.delete(removed.id) }
      }
      return next
    })

    if (duration > 0) {
      timers.set(id, window.setTimeout(() => dismiss(id), duration))
    }
    return id
  }, [dismiss, timers])

  const api = useMemo<ToastApi>(() => ({
    show,
    info: (message, options) => show(message, { ...options, tone: 'info' }),
    success: (message, options) => show(message, { ...options, tone: 'success' }),
    error: (message, options) => show(message, { ...options, tone: 'error' }),
    dismiss
  }), [dismiss, show])

  useEffect(() => () => {
    for (const timer of timers.values()) {
      window.clearTimeout(timer)
    }
    timers.clear()
  }, [timers])

  return (
    <ToastContext.Provider value={api}>
      {children}
      {createPortal(
        <div className="toast-viewport" aria-live="polite" aria-relevant="additions">
          {toasts.map((toast) => (
            <div
              className={`toast toast--${toast.tone}`}
              key={toast.id}
              role={toast.tone === 'error' ? 'alert' : 'status'}
            >
              <ToastIcon tone={toast.tone} />
              <div className="toast__message">{toast.message}</div>
              <div className="toast__actions">
                {toast.tone === 'error' && <button className="toast__copy" type="button" onClick={async event => {
                  const message = event.currentTarget.closest('.toast')?.querySelector('.toast__message')?.textContent ?? ''
                  try { await navigator.clipboard.writeText(message); setCopiedId(toast.id) }
                  catch { /* Keep the selectable error available for manual copying. */ }
                }}>{t(copiedId === toast.id ? 'json.action.copied' : 'common.toast.copyError')}</button>}
              <button
                className="toast__dismiss"
                type="button"
                aria-label={t('common.toast.dismiss')}
                onClick={() => dismiss(toast.id)}
              >
                <X size={15} />
              </button>
              </div>
            </div>
          ))}
        </div>,
        document.body
      )}
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const context = use(ToastContext)
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider')
  }
  return context
}

function ToastIcon({ tone }: { tone: ToastTone }) {
  if (tone === 'success') {
    return <CircleCheck className="toast__icon" size={17} />
  }
  if (tone === 'error') {
    return <CircleAlert className="toast__icon" size={17} />
  }
  return <Info className="toast__icon" size={17} />
}
