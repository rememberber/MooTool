# ADR 002 — Editor

Status: Accepted for P0, pending desktop IME evidence  
Date: 2026-09-09

## Context

Formal editors need line numbers, undo/redo, wrap, find/replace, large documents, and rectangular edits. Architecture prefers RichTextFX 0.11.7 `CodeArea`. A WebView/CodeMirror fallback is allowed only after a recorded native failure.

## Decision

- P0 implements `EditorHost` over RichTextFX `CodeArea` + `VirtualizedScrollPane` + `LineNumberFactory`.
- Undo/redo uses RichTextFX's built-in undo manager. Theme/font changes do not replace the document.
- Find/replace and rectangular edits are implemented in pure Java (`FindReplace`, `ColumnEdits`) using UTF-16 offsets and visual columns (tab size 4). Alt+drag/Alt+type is the P0 column-edit gesture; the same operations are unit-tested without the GUI.
- JSON and the component gallery share this host. The same Node is moved between docked and detached stages.
- WebView/CodeMirror is not selected. No editor ADR fallback is required until a P0 desktop IME or transfer failure is reproduced.

## Consequences

Chinese/Japanese IME, candidate window placement, and 50× window transfer must still be recorded on a real desktop session. Those checks are not implied by unit tests. If IME composition is interrupted by `replaceText`, the fix stays in `RichTextEditorHost` first; switching engines requires a new ADR revision.
