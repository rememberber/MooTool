import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { uaMessages } from './uaMessages'

describe('UA messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(uaMessages)).toEqual([]))
})
