import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { messageBoardMessages } from './messageBoardMessages'

describe('Message Board messages', () => {
  it('keeps locale keys and placeholders aligned', () => {
    expect(validateMessageCatalog(messageBoardMessages)).toEqual([])
  })
})
