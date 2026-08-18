import { expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { quickNoteMessages } from './quickNoteMessages'

it('keeps Quick Note message keys and placeholders aligned across locales', () => {
  expect(validateMessageCatalog(quickNoteMessages)).toEqual([])
})
