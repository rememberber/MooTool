import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { encodeMessages } from './encodeMessages'

describe('encode messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(encodeMessages)).toEqual([])
  })
})
