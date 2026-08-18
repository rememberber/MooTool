import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { calculatorMessages } from './calculatorMessages'

describe('calculator messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(calculatorMessages)).toEqual([]))
})
