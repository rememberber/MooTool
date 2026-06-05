import type { LucideIcon } from 'lucide-react'

type ToolButtonProps = {
  icon: LucideIcon
  label: string
  active?: boolean
}

export function ToolButton({ icon: Icon, label, active = false }: ToolButtonProps) {
  return (
    <button className={active ? 'tool-button tool-button--active' : 'tool-button'}>
      <Icon size={18} />
      <span>{label}</span>
    </button>
  )
}
