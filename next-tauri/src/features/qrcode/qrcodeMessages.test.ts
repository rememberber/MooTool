import { expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { qrcodeMessages } from './qrcodeMessages'

it('keeps QR Code message keys and placeholders aligned across locales', () => {
  expect(validateMessageCatalog(qrcodeMessages)).toEqual([])
})
