import type { LucideIcon } from 'lucide-react'

type ToolButtonProps = {
  icon: LucideIcon
  label: string
  active?: boolean
  onClick?: () => void
}

export function ToolButton({ icon: Icon, label, active = false, onClick }: ToolButtonProps) {
  return (
    <button className={active ? 'tool-button tool-button--active' : 'tool-button'} onClick={onClick}>
      <Icon size={17} />
      <span>{label}</span>
    </button>
  )
}
