import { forwardRef } from 'react'
import {
  TextCodeEditor,
  type TextCodeEditorHandle
} from '@/shared/components/TextCodeEditor'
import type { CodeEditorViewState } from '@/shared/components/codeEditorViewState'
import { defaultFindReplaceOptions, type FindReplaceOptions } from '@/shared/components/findReplace'

export type JsonCodeEditorHandle = TextCodeEditorHandle

type JsonCodeEditorProps = {
  value: string
  wrap: boolean
  fontSize: number
  searchQuery: string
  searchOptions?: FindReplaceOptions
  ariaLabel: string
  initialViewState?: CodeEditorViewState
  onChange: (value: string) => void
  onKeyDown?: (event: KeyboardEvent) => void
  onViewStateChange?: (state: CodeEditorViewState) => void
}

export const JsonCodeEditor = forwardRef<JsonCodeEditorHandle, JsonCodeEditorProps>(function JsonCodeEditor(
  { value, wrap, fontSize, searchQuery, searchOptions = defaultFindReplaceOptions, ariaLabel, initialViewState, onChange, onKeyDown, onViewStateChange },
  ref
) {
  return (
    <TextCodeEditor
      ref={ref}
      className="json-editor"
      language="json"
      value={value}
      wrap={wrap}
      fontSize={fontSize}
      searchQuery={searchQuery}
      searchOptions={searchOptions}
      ariaLabel={ariaLabel}
      initialViewState={initialViewState}
      onChange={onChange}
      onKeyDown={onKeyDown}
      onViewStateChange={onViewStateChange}
    />
  )
})
