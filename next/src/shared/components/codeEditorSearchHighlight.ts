import type { Extension } from '@codemirror/state'
import {
  Decoration,
  type DecorationSet,
  EditorView,
  MatchDecorator,
  ViewPlugin,
  type ViewUpdate
} from '@codemirror/view'

export function codeEditorSearchHighlight(query: string): Extension {
  if (!query) return []

  const matcher = new MatchDecorator({
    regexp: new RegExp(escapeRegExp(query), 'giu'),
    decoration: Decoration.mark({ class: 'cm-searchMatch' })
  })

  return ViewPlugin.fromClass(class {
    decorations: DecorationSet

    constructor(view: EditorView) {
      this.decorations = matcher.createDeco(view)
    }

    update(update: ViewUpdate): void {
      this.decorations = matcher.updateDeco(update, this.decorations)
    }
  }, {
    decorations: (value) => value.decorations
  })
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}
