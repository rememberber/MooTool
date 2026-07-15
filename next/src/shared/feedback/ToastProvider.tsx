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
  const nextId = useRef(1)
  const timers = useRef(new Map<number, number>())

  const dismiss = useCallback((id: number) => {
    const timer = timers.current.get(id)
    if (timer !== undefined) {
      window.clearTimeout(timer)
      timers.current.delete(id)
    }
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const show = useCallback((message: ReactNode, options: ToastOptions = {}) => {
    const id = nextId.current++
    const duration = options.duration ?? 3200
    const item: ToastItem = { id, message, tone: options.tone ?? 'info' }

    setToasts((current) => {
      const next = [...current, item]
      const overflow = next.slice(0, -4)
      for (const toast of overflow) {
        const timer = timers.current.get(toast.id)
        if (timer !== undefined) {
          window.clearTimeout(timer)
          timers.current.delete(toast.id)
        }
      }
      return next.slice(-4)
    })

    if (duration > 0) {
      timers.current.set(id, window.setTimeout(() => dismiss(id), duration))
    }
    return id
  }, [dismiss])

  const api = useMemo<ToastApi>(() => ({
    show,
    info: (message, options) => show(message, { ...options, tone: 'info' }),
    success: (message, options) => show(message, { ...options, tone: 'success' }),
    error: (message, options) => show(message, { ...options, tone: 'error' }),
    dismiss
  }), [dismiss, show])

  useEffect(() => () => {
    for (const timer of timers.current.values()) {
      window.clearTimeout(timer)
    }
    timers.current.clear()
  }, [])

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
              <button
                className="toast__dismiss"
                type="button"
                aria-label={t('common.toast.dismiss')}
                onClick={() => dismiss(toast.id)}
              >
                <X size={15} />
              </button>
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
