import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { jsonMessages } from './jsonMessages'

describe('JSON messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(jsonMessages)).toEqual([])
  })
})
