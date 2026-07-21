import { html } from '@codemirror/lang-html'
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { json } from '@codemirror/lang-json'
import { markdown } from '@codemirror/lang-markdown'
import { python } from '@codemirror/lang-python'
import { sql } from '@codemirror/lang-sql'
import { xml } from '@codemirror/lang-xml'
import { yaml } from '@codemirror/lang-yaml'
import { closeBrackets } from '@codemirror/autocomplete'
import {
  HighlightStyle,
  bracketMatching,
  foldGutter,
  indentOnInput,
  syntaxHighlighting
} from '@codemirror/language'
import type { Extension } from '@codemirror/state'
import { tags } from '@lezer/highlight'

export const textCodeEditorLanguages = [
  'text',
  'json',
  'markdown',
  'java',
  'javascript',
  'typescript',
  'python',
  'xml',
  'html',
  'yaml',
  'sql'
] as const

export type TextCodeEditorLanguage = (typeof textCodeEditorLanguages)[number]

export const richCodeDocumentLimit = 1_000_000

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

export function resolveTextCodeEditorLanguage(value?: string | null): TextCodeEditorLanguage {
  const normalized = value?.trim().toLocaleLowerCase() ?? ''
  if (!normalized) return 'text'
  if (normalized === 'json' || normalized === 'application/json' || normalized.endsWith('+json')) return 'json'
  if (normalized === 'markdown' || normalized === 'md' || normalized === 'text/markdown') return 'markdown'
  if (normalized === 'java' || normalized === 'text/java') return 'java'
  if (['javascript', 'js', 'node', 'text/javascript', 'application/javascript'].includes(normalized)) return 'javascript'
  if (['typescript', 'ts', 'text/typescript', 'application/typescript'].includes(normalized)) return 'typescript'
  if (normalized === 'python' || normalized === 'py' || normalized === 'text/python') return 'python'
  if (normalized === 'xml' || normalized === 'text/xml' || normalized === 'application/xml' || normalized.endsWith('+xml')) return 'xml'
  if (normalized === 'html' || normalized === 'text/html') return 'html'
  if (['yaml', 'yml', 'text/yaml', 'application/yaml', 'application/x-yaml'].includes(normalized)) return 'yaml'
  if (normalized === 'sql' || normalized === 'text/sql' || normalized === 'application/sql') return 'sql'
  return 'text'
}

export function codeEditorLanguageExtensions(language: TextCodeEditorLanguage, enabled = true): Extension {
  if (!enabled) return [bracketMatching()]

  const languageSupport = languageExtension(language)
  return [
    bracketMatching(),
    closeBrackets(),
    ...(languageSupport ? [languageSupport, indentOnInput(), foldGutter(), syntaxHighlighting(codeHighlightStyle)] : [])
  ]
}

function languageExtension(language: TextCodeEditorLanguage): Extension | null {
  switch (language) {
    case 'json': return json()
    case 'markdown': return markdown()
    case 'java': return java()
    case 'javascript': return javascript()
    case 'typescript': return javascript({ typescript: true })
    case 'python': return python()
    case 'xml': return xml()
    case 'html': return html()
    case 'yaml': return yaml()
    case 'sql': return sql()
    default: return null
  }
}
