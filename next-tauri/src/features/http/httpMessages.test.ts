import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { httpMessages } from './httpMessages'

describe('HTTP messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(httpMessages)).toEqual([])
  })
})
