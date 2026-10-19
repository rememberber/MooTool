import { HighlightStyle, syntaxHighlighting } from '@codemirror/language'
import type { Extension } from '@codemirror/state'
import { tags } from '@lezer/highlight'

const codeHighlightStyle = HighlightStyle.define([
  { tag: tags.propertyName, color: 'var(--syntax-property)' },
  { tag: [tags.string, tags.docString, tags.regexp, tags.link], color: 'var(--syntax-string)' },
  { tag: tags.number, color: 'var(--syntax-number)' },
  { tag: [tags.bool, tags.null, tags.atom], color: 'var(--syntax-literal)' },
  { tag: [tags.keyword, tags.modifier, tags.controlKeyword], color: 'var(--syntax-keyword)' },
  { tag: [tags.typeName, tags.className, tags.namespace], color: 'var(--syntax-type)' },
  { tag: [tags.function(tags.variableName), tags.definition(tags.function(tags.variableName))], color: 'var(--syntax-function)' },
  { tag: [tags.comment, tags.lineComment, tags.blockComment], color: 'var(--syntax-comment)', fontStyle: 'italic' },
  { tag: [tags.heading, tags.strong], color: 'var(--syntax-keyword)', fontWeight: '600' },
  { tag: tags.emphasis, fontStyle: 'italic' },
  { tag: [tags.operator, tags.punctuation, tags.separator], color: 'var(--syntax-operator)' },
  { tag: tags.invalid, color: 'var(--error-text)', textDecoration: 'underline wavy' }
])

export function electronCodeHighlighting(): Extension {
  return syntaxHighlighting(codeHighlightStyle, { fallback: true })
}
