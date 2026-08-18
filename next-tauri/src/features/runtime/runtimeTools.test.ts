import { describe, expect, it } from 'vitest'
import { parseRuntimeArguments } from './runtimeTools'
describe('runtime arguments', () => { it('parses quoted arguments without a shell', () => { expect(parseRuntimeArguments(`--name "Moo Tool" '中文 参数'`)).toEqual(['--name', 'Moo Tool', '中文 参数']) }); it('rejects unclosed quotes', () => expect(() => parseRuntimeArguments(`"open`)).toThrow('RUNTIME_TOOL_unclosedArguments')) })
