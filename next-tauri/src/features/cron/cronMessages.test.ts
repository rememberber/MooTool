import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { cronMessages } from './cronMessages'
describe('Cron messages', () => { it('aligns locales', () => expect(validateMessageCatalog(cronMessages)).toEqual([])) })
