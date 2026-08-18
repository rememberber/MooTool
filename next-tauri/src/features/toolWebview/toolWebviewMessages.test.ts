import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { toolWebviewMessages } from './toolWebviewMessages'

describe('tool WebView messages', () => {
  it('aligns locales', () => expect(validateMessageCatalog(toolWebviewMessages)).toEqual([]))
})
