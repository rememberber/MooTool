import type { ToolId } from '@/app/toolRegistry'

export function isImmersiveToolId(toolId: ToolId): boolean {
  return toolId !== 'mootool'
}
