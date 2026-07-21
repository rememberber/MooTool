/** Matches Java: Ctrl/Cmd+Shift+F, or macOS IntelliJ-style Cmd+Option+L. */
export function isFormatShortcut(event: KeyboardEvent): boolean {
  // Prefer `code` so macOS Option/Alt dead-key remapping (e.g. Option+L → ¬) still matches.
  const code = event.code
  if ((event.metaKey || event.ctrlKey) && event.shiftKey && code === 'KeyF') return true
  if (event.metaKey && event.altKey && code === 'KeyL') return true
  return false
}
