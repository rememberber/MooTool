import { X } from 'lucide-react'
import { createContext, useContext, useEffectEvent, useId, useLayoutEffect, useRef, type ReactNode, type RefObject } from 'react'
import { createPortal } from 'react-dom'
import { useToolActivity } from './ToolActivity'
import { useI18n } from '@/shared/i18n/I18nProvider'

const portalContext = createContext<RefObject<HTMLDivElement | null> | null>(null)
const modalRoots: HTMLElement[] = []
const previousInert = new Map<HTMLElement, boolean>()

function syncBackground() {
  for (const [element, inert] of previousInert) element.inert = inert
  previousInert.clear()
  const top = modalRoots.filter(element => element.isConnected).at(-1)
  if (!top) return
  for (const element of document.body.children) {
    if (!(element instanceof HTMLElement) || element.contains(top)) continue
    previousInert.set(element, element.inert)
    element.inert = true
  }
}

/** Menus and tooltips belonging to a dialog must remain inside its modal scope. */
export function DialogPortal({ children }: { children: ReactNode }) {
  const root = useContext(portalContext)
  return createPortal(children, root?.current ?? document.body)
}

function focusable(root: HTMLElement): HTMLElement[] {
  return [...root.querySelectorAll<HTMLElement>('button, input, select, textarea, a[href], [tabindex], [contenteditable="true"]')]
    .filter(element => element.tabIndex >= 0 && !element.matches(':disabled') && !element.closest('[inert]') && element.getClientRects().length > 0)
}

type DialogProps = {
  title: string
  open: boolean
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  width?: number
  dismissOnBackdrop?: boolean
}

export function Dialog({ title, open, onClose, children, footer, width = 640, dismissOnBackdrop = false }: DialogProps) {
  const { t } = useI18n()
  const titleId = useId()
  const dialogRef = useRef<HTMLElement>(null)
  const backdropRef = useRef<HTMLDivElement>(null)
  const openerRef = useRef<HTMLElement | null>(null)
  const wasVisible = useRef(false)
  const toolActive = useToolActivity()
  const visible = open && toolActive
  // Capture before React's autoFocus runs during the commit.
  if (visible && !wasVisible.current) openerRef.current = document.activeElement instanceof HTMLElement ? document.activeElement : null
  wasVisible.current = visible
  const close = useEffectEvent(onClose)

  useLayoutEffect(() => {
    if (!visible || !dialogRef.current || !backdropRef.current) return
    const dialog = dialogRef.current
    const root = backdropRef.current
    const opener = openerRef.current
    modalRoots.push(root)
    syncBackground()
    const isTop = () => [...document.querySelectorAll('[role="dialog"][aria-modal="true"]')].at(-1) === dialog
    const focusInitial = () => {
      const items = focusable(root)
      const preferred = items.find(element => element.hasAttribute('data-dialog-initial-focus'))
        ?? (root.contains(document.activeElement) ? document.activeElement as HTMLElement : null)
        ?? items.find(element => element.closest('.dialog__body') && element.matches('input, select, textarea, [contenteditable="true"]'))
        ?? items.find(element => element.closest('.dialog__footer')) ?? dialog
      preferred.focus({ preventScroll: true })
    }
    focusInitial()
    const keydown = (event: KeyboardEvent) => {
      if (!isTop() || event.defaultPrevented || event.isComposing) return
      if (event.key === 'Escape') {
        event.preventDefault()
        event.stopPropagation()
        close()
      } else if (event.key === 'Tab') {
        const items = focusable(root)
        const index = items.indexOf(document.activeElement as HTMLElement)
        if (!items.length || index < 0 || (event.shiftKey ? index === 0 : index === items.length - 1)) {
          event.preventDefault()
          ;(event.shiftKey ? items.at(-1) : items[0] ?? dialog)?.focus()
          if (!items.length) dialog.focus()
        }
      }
    }
    const focusin = (event: FocusEvent) => { if (isTop() && !root.contains(event.target as Node)) focusInitial() }
    window.addEventListener('keydown', keydown)
    document.addEventListener('focusin', focusin)
    return () => {
      window.removeEventListener('keydown', keydown)
      document.removeEventListener('focusin', focusin)
      const index = modalRoots.indexOf(root)
      if (index >= 0) modalRoots.splice(index, 1)
      syncBackground()
      if (opener?.isConnected && !opener.closest('[inert]') && opener.getClientRects().length) opener.focus({ preventScroll: true })
      else {
        const parent = modalRoots.filter(element => element.isConnected).at(-1)
        if (parent && !parent.contains(document.activeElement)) (focusable(parent)[0] ?? parent.querySelector<HTMLElement>('[role="dialog"]'))?.focus({ preventScroll: true })
      }
    }
  }, [visible])

  if (!visible) return null
  return createPortal(
    <div ref={backdropRef} className="dialog-backdrop" role="presentation" onMouseDown={event => {
      if (dismissOnBackdrop && event.target === event.currentTarget) onClose()
    }}>
      <portalContext.Provider value={backdropRef}>
        <section ref={dialogRef} className="dialog" role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} style={{ width }}>
          <header className="dialog__header">
            <h2 id={titleId}>{title}</h2>
            <button className="icon-ghost dialog__close" type="button" aria-label={`${t('common.close')} ${title}`} onClick={onClose}><X size={16} /></button>
          </header>
          <div className="dialog__body">{children}</div>
          {footer && <footer className="dialog__footer">{footer}</footer>}
        </section>
      </portalContext.Provider>
    </div>, document.body
  )
}
