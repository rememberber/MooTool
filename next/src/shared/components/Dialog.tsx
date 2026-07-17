import { X } from 'lucide-react'
import { useEffect, useId, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { useToolActivity } from './ToolActivity'

type DialogProps = {
  title: string
  open: boolean
  onClose: () => void
  children: ReactNode
  footer?: ReactNode
  width?: number
}

export function Dialog({ title, open, onClose, children, footer, width = 640 }: DialogProps) {
  const titleId = useId()
  const toolActive = useToolActivity()
  const visible = open && toolActive
  useEffect(() => {
    if (!visible) return
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [onClose, visible])

  if (!visible) return null

  return createPortal(
    <div className="dialog-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        style={{ width }}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="dialog__header">
          <h2 id={titleId}>{title}</h2>
          <button className="icon-ghost" type="button" aria-label={title} onClick={onClose}><X size={16} /></button>
        </header>
        <div className="dialog__body">{children}</div>
        {footer && <footer className="dialog__footer">{footer}</footer>}
      </section>
    </div>,
    document.body
  )
}
