import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from './localizedMessages'
import { surfaceMessages } from './surfaceMessages'

describe('surface messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(surfaceMessages)).toEqual([]))
})
