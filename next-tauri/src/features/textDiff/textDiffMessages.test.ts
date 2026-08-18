import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { textDiffMessages } from './textDiffMessages'

describe('Text Diff messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(textDiffMessages)).toEqual([])
  })
})
