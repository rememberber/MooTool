import { PanelTopClose, PanelTopOpen, type LucideIcon } from 'lucide-react'

type ToolButtonProps = {
  icon: LucideIcon
  label: string
  active?: boolean
  detached?: boolean
  windowActionLabel?: string
  onClick?: () => void
  onWindowAction?: () => void
}

export function ToolButton({
  icon: Icon,
  label,
  active = false,
  detached = false,
  windowActionLabel,
  onClick,
  onWindowAction
}: ToolButtonProps) {
  const showWindowAction = Boolean(windowActionLabel && onWindowAction)

  return (
    <div className={showWindowAction ? 'tool-button-row tool-button-row--window-action' : 'tool-button-row'}>
      <button
        className={active ? 'tool-button tool-button--active' : 'tool-button'}
        type="button"
        aria-label={label}
        aria-current={active ? 'page' : undefined}
        onClick={onClick}
      >
        <Icon size={17} />
        <span>{label}</span>
      </button>
      {showWindowAction && (
        <button
          className={detached ? 'tool-button__window-action tool-button__window-action--detached' : 'tool-button__window-action'}
          type="button"
          aria-label={windowActionLabel}
          title={windowActionLabel}
          onClick={onWindowAction}
        >
          {detached ? <PanelTopClose size={13} /> : <PanelTopOpen size={13} />}
        </button>
      )}
    </div>
  )
}
