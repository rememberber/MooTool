import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { historyMessages } from './historyMessages'

describe('history messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(historyMessages)).toEqual([]))
})
