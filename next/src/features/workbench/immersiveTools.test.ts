import { describe, expect, it } from 'vitest'
import { toolIds } from '@/app/toolRegistry'
import { isImmersiveToolId } from './immersiveTools'

describe('immersive tool classification', () => {
  it('keeps home unchanged and makes every functional tool immersive', () => {
    expect(isImmersiveToolId('mootool')).toBe(false)
    expect(toolIds.filter((toolId) => toolId !== 'mootool').every(isImmersiveToolId)).toBe(true)
  })
})
