import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { configMessages } from './configMessages'

describe('config messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(configMessages)).toEqual([]))
})
