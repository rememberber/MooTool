import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { variablesMessages } from './variablesMessages'
describe('variables messages', () => { it('aligns locales', () => expect(validateMessageCatalog(variablesMessages)).toEqual([])) })
