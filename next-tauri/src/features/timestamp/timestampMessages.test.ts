import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { timestampMessages } from './timestampMessages'
describe('timestamp messages', () => { it('aligns locales', () => expect(validateMessageCatalog(timestampMessages)).toEqual([])) })
