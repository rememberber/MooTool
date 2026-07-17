import { forwardRef } from 'react'
import {
  TextCodeEditor,
  type TextCodeEditorHandle,
  type TextCodeEditorProps
} from '@/shared/components/TextCodeEditor'

export type QuickNoteCodeEditorHandle = TextCodeEditorHandle

type QuickNoteCodeEditorProps = Omit<TextCodeEditorProps, 'className'>

export const QuickNoteCodeEditor = forwardRef<QuickNoteCodeEditorHandle, QuickNoteCodeEditorProps>(function QuickNoteCodeEditor(
  props,
  ref
) {
  return <TextCodeEditor {...props} ref={ref} className="quick-note-code-editor" />
})
