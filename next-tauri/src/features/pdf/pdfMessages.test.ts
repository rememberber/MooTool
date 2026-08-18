import { expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { pdfMessages } from './pdfMessages'

it('keeps PDF message keys and placeholders aligned across locales', () => {
  expect(validateMessageCatalog(pdfMessages)).toEqual([])
})
