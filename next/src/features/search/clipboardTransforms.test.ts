import { describe, expect, it } from 'vitest'
import { parseClipboardTable, tableRowsToJson } from './clipboardTransforms'

describe('clipboard tabular conversion', () => {
  it('preserves strings, empty cells, tabs, quotes and line breaks in spreadsheet TSV', () => {
    const rows = parseClipboardTable('姓名\t编号\t备注\r\n张三\t001\t"第一行\r\n第二行\t""引用"""\r\n李四\t002\t\r\n')
    expect(JSON.parse(tableRowsToJson(rows))).toEqual([
      { 姓名: '张三', 编号: '001', 备注: '第一行\n第二行\t"引用"' },
      { 姓名: '李四', 编号: '002', 备注: '' }
    ])
  })
  it('detects CSV without confusing quoted delimiters with table columns', () => {
    expect(parseClipboardTable('"name, display",note\n"A, B","""quoted"""')).toEqual([['name, display', 'note'], ['A, B', '"quoted"']])
    expect(parseClipboardTable('name\tnote\nA\tx,y')).toEqual([['name', 'note'], ['A', 'x,y']])
    expect(parseClipboardTable('code\n001\n002')).toEqual([['code'], ['001'], ['002']])
  })
  it('handles special property names without modifying object prototypes', () => {
    expect(tableRowsToJson([['__proto__', 'constructor'], ['value', 'other']])).toBe('[\n  {\n    "__proto__": "value",\n    "constructor": "other"\n  }\n]')
    expect(({} as { value?: string }).value).toBeUndefined()
  })
  it('rejects malformed rows and ambiguous headers instead of dropping cells', () => {
    for (const source of ['a\tb\n1', 'a\nb\tc', 'a\nb"c', 'a\n"b', 'a\n"b"c', 'a\tb']) {
      expect(() => tableRowsToJson(parseClipboardTable(source))).toThrow('app.command.invalidTable')
    }
    for (const rows of [[['a', 'a'], ['1', '2']], [['a', ' a '], ['1', '2']], [['', 'b'], ['1', '2']]]) {
      expect(() => tableRowsToJson(rows)).toThrow('app.command.tableHeaders')
    }
  })
  it('bounds column counts and data rows', () => {
    expect(() => parseClipboardTable(Array.from({ length: 201 }, (_, i) => String(i)).join('\t'))).toThrow('app.command.tableLimit')
    expect(() => tableRowsToJson(Array.from({ length: 5002 }, () => ['a']))).toThrow('app.command.tableLimit')
  })
})
