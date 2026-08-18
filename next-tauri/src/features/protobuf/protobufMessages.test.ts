import { describe, expect, it } from 'vitest'
import { validateMessageCatalog } from '../../app/localizedMessages'
import { protobufMessages } from './protobufMessages'
describe('Protobuf messages', () => { it('aligns locales', () => expect(validateMessageCatalog(protobufMessages)).toEqual([])) })
