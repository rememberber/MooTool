import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { reformatMessages } from './reformatMessages'

describe('reformat messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(reformatMessages)).toEqual([]))
})
