import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { runtimeMessages } from './runtimeMessages'
describe('runtime messages', () => { it('aligns locales', () => expect(validateMessageCatalog(runtimeMessages)).toEqual([])) })
