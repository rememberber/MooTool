import type { Extension } from '@codemirror/state'
import {
  Decoration,
  type DecorationSet,
  EditorView,
  MatchDecorator,
  ViewPlugin,
  type ViewUpdate
} from '@codemirror/view'
import {
  buildSearchRegExp,
  defaultFindReplaceOptions,
  type FindReplaceOptions
} from './findReplace'

export function codeEditorSearchHighlight(
  query: string,
  options: FindReplaceOptions = defaultFindReplaceOptions
): Extension {
  const expression = buildSearchRegExp(query, options)
  if (!expression) return []

  const matcher = new MatchDecorator({
    regexp: expression,
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
