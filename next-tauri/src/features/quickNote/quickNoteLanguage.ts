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
    case 'application/json': return [json()]
    case 'text/markdown': return [markdown()]
    case 'text/java': return [java()]
    case 'text/javascript': return [javascript()]
    case 'text/typescript': return [javascript({ typescript: true })]
    case 'text/python': return [python()]
    case 'text/html': return [html()]
    case 'text/xml': return [xml()]
    case 'text/yaml': return [yaml()]
    case 'text/sql': return [sql()]
    default: return []
  }
}
