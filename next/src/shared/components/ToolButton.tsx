import { PanelTopOpen, type LucideIcon } from 'lucide-react'

type ToolButtonProps = {
  icon: LucideIcon
  label: string
  active?: boolean
  detached?: boolean
  onClick?: () => void
}

export function ToolButton({ icon: Icon, label, active = false, detached = false, onClick }: ToolButtonProps) {
  return (
    <button
      className={active ? 'tool-button tool-button--active' : 'tool-button'}
      type="button"
      aria-label={label}
      aria-current={active ? 'page' : undefined}
      onClick={onClick}
    >
      <Icon size={17} />
      <span>{label}</span>
      {detached && <PanelTopOpen className="tool-button__detached" size={12} aria-hidden="true" />}
    </button>
  )
}
