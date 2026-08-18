import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { networkMessages } from './networkMessages'
describe('network messages', () => { it('aligns locales', () => expect(validateMessageCatalog(networkMessages)).toEqual([])) })
