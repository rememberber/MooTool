import { expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { colorMessages } from './colorMessages'

it('keeps Color message keys and placeholders aligned across locales', () => {
  expect(validateMessageCatalog(colorMessages)).toEqual([])
})
