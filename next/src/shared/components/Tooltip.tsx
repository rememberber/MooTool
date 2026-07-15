import {
  cloneElement,
  useCallback,
  useEffect,
  useEffectEvent,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type ReactElement,
  type ReactNode
} from 'react'
import { createPortal } from 'react-dom'

type TooltipSide = 'top' | 'right' | 'bottom' | 'left'

type TooltipProps = {
  children: ReactElement<{ 'aria-describedby'?: string }>
  content: ReactNode
  side?: TooltipSide
  delay?: number
}

const tooltipGap = 8

export function Tooltip({ children, content, side = 'top', delay = 400 }: TooltipProps) {
  const id = useId()
  const triggerRef = useRef<HTMLSpanElement>(null)
  const timerRef = useRef<number | null>(null)
  const [open, setOpen] = useState(false)
  const [position, setPosition] = useState<CSSProperties>({})

  const clearTimer = useCallback(() => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const updatePosition = useCallback(() => {
    const trigger = triggerRef.current
    if (!trigger) {
      return
    }

    const rect = trigger.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2
    const positions: Record<TooltipSide, CSSProperties> = {
      top: { left: centerX, top: rect.top - tooltipGap },
      right: { left: rect.right + tooltipGap, top: centerY },
      bottom: { left: centerX, top: rect.bottom + tooltipGap },
      left: { left: rect.left - tooltipGap, top: centerY }
    }
    setPosition(positions[side])
  }, [side])

  function showTooltip(): void {
    clearTimer()
    timerRef.current = window.setTimeout(() => {
      updatePosition()
      setOpen(true)
      timerRef.current = null
    }, delay)
  }

  function hideTooltip(): void {
    clearTimer()
    setOpen(false)
  }

  const repositionTooltip = useEffectEvent(updatePosition)

  useEffect(() => {
    if (!open) {
      return
    }

    const reposition = () => repositionTooltip()
    window.addEventListener('resize', reposition)
    window.addEventListener('scroll', reposition, true)
    return () => {
      window.removeEventListener('resize', reposition)
      window.removeEventListener('scroll', reposition, true)
    }
  }, [open])

  useEffect(() => () => clearTimer(), [clearTimer])

  const describedBy = [children.props['aria-describedby'], id].filter(Boolean).join(' ')

  return (
    <span
      className="tooltip-trigger"
      ref={triggerRef}
      onMouseEnter={showTooltip}
      onMouseLeave={hideTooltip}
      onFocusCapture={showTooltip}
      onBlurCapture={hideTooltip}
    >
      {cloneElement(children, { 'aria-describedby': describedBy })}
      {open && createPortal(
        <span className={`tooltip tooltip--${side}`} id={id} role="tooltip" style={position}>
          {content}
        </span>,
        document.body
      )}
    </span>
  )
}
