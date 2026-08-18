import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { systemMessages } from './systemMessages'
describe('system messages', () => { it('aligns locales', () => expect(validateMessageCatalog(systemMessages)).toEqual([])) })
