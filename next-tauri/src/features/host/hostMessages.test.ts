import { expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { hostMessages } from './hostMessages'

it('keeps Host message keys and placeholders aligned across locales', () => {
  expect(validateMessageCatalog(hostMessages)).toEqual([])
})
