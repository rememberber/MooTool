import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { cryptoMessages } from './cryptoMessages'

describe('crypto messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(cryptoMessages)).toEqual([])
  })
})
