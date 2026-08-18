import { expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { imageMessages } from './imageMessages'

it('keeps Image message keys and placeholders aligned across locales', () => {
  expect(validateMessageCatalog(imageMessages)).toEqual([])
})
