import { describe, expect, it } from 'vitest'
import { toolGroups, toolIds, toolRegistry } from './toolRegistry'

describe('toolRegistry', () => {
  it('contains the home entry and every Java tool exactly once', () => {
    expect(toolRegistry).toHaveLength(25)
    expect(new Set(toolRegistry.map((tool) => tool.id)).size).toBe(25)
    expect(toolRegistry.map((tool) => tool.id)).toEqual(toolIds)
  })

  it('places all 24 tools into the six built-in groups', () => {
    const groupedToolIds = toolGroups.flatMap((group) => group.toolIds)
    expect(toolGroups).toHaveLength(6)
    expect(groupedToolIds).toHaveLength(24)
    expect(new Set(groupedToolIds)).toEqual(new Set(toolIds.filter((id) => id !== 'mootool')))
  })
})
