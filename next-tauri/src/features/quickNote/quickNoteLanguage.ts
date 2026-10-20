import { html } from '@codemirror/lang-html'
import { java } from '@codemirror/lang-java'
import { javascript } from '@codemirror/lang-javascript'
import { json } from '@codemirror/lang-json'
import { markdown } from '@codemirror/lang-markdown'
import { python } from '@codemirror/lang-python'
import { sql } from '@codemirror/lang-sql'
import { xml } from '@codemirror/lang-xml'
import { yaml } from '@codemirror/lang-yaml'
import type { Extension } from '@codemirror/state'
import type { QuickNote } from '../../platform/contracts/localData'

export function quickNoteLanguage(syntax: QuickNote['syntax']): Extension[] {
  switch (syntax) {
    case 'json': return [json()]
    case 'markdown': return [markdown()]
    case 'java': return [java()]
    case 'javascript': return [javascript()]
    case 'typescript': return [javascript({ typescript: true })]
    case 'python': return [python()]
    case 'html': return [html()]
    case 'xml': return [xml()]
    case 'yaml': return [yaml()]
    case 'sql': return [sql()]
    default: return []
  }
}
