import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { regexMessages } from './regexMessages'
describe('Regex messages', () => { it('aligns locales', () => expect(validateMessageCatalog(regexMessages)).toEqual([])) })
