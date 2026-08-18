import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { translationMessages } from './translationMessages'

describe('translation messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(translationMessages)).toEqual([])
  })
})
