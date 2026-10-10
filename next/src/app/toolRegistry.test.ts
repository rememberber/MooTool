import { describe, expect, it } from 'vitest'
import { toolGroups, toolIds, toolRegistry } from './toolRegistry'

describe('toolRegistry', () => {
  it('contains the home entry and every registered tool exactly once', () => {
    expect(toolRegistry).toHaveLength(27)
    expect(new Set(toolRegistry.map((tool) => tool.id)).size).toBe(27)
    expect(toolRegistry.map((tool) => tool.id)).toEqual(toolIds)
  })

  it('places all 26 tools into the six built-in groups', () => {
    const groupedToolIds = toolGroups.flatMap((group) => group.toolIds)
    expect(toolGroups).toHaveLength(6)
    expect(groupedToolIds).toHaveLength(26)
    expect(new Set(groupedToolIds)).toEqual(new Set(toolIds.filter((id) => id !== 'mootool')))
  })
})
