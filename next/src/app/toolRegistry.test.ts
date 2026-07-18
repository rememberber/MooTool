import { describe, expect, it } from 'vitest'
import { toolGroups, toolIds, toolRegistry } from './toolRegistry'

describe('toolRegistry', () => {
  it('contains the home entry and every Java tool exactly once', () => {
    expect(toolRegistry).toHaveLength(36)
    expect(new Set(toolRegistry.map((tool) => tool.id)).size).toBe(36)
    expect(toolRegistry.map((tool) => tool.id)).toEqual(toolIds)
  })

  it('places all 35 tools into the seven built-in groups', () => {
    const groupedToolIds = toolGroups.flatMap((group) => group.toolIds)
    expect(toolGroups).toHaveLength(7)
    expect(groupedToolIds).toHaveLength(35)
    expect(new Set(groupedToolIds)).toEqual(new Set(toolIds.filter((id) => id !== 'mootool')))
  })
})
