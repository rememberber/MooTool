async function reportStartupError(cause: unknown): Promise<void> {
  try {
    const { reportProductError, toProductError } = await import('./shared/errors')
    await reportProductError(toProductError(cause), 'application.bootstrap')
  } catch {
    // Startup reporting is best effort; the fallback UI must remain available.
  }
}

function renderStartupError(cause: unknown): void {
  void reportStartupError(cause)
  const root = document.getElementById('root')
  if (!root) return
  const message = cause instanceof Error
    ? `${cause.name}: ${cause.message}\n${cause.stack ?? ''}`
    : String(cause)
  document.title = 'MooTool Next Tauri · Startup Error'
  root.replaceChildren()
  const panel = document.createElement('pre')
  panel.textContent = `${startupErrorTitle()}\n\n${message}`
  Object.assign(panel.style, {
    margin: '0',
    minHeight: '100vh',
    padding: '48px',
    boxSizing: 'border-box',
    overflow: 'auto',
    background: '#17191e',
    color: '#f0a0a0',
    font: '13px/1.65 ui-monospace, SFMono-Regular, Menlo, monospace',
    whiteSpace: 'pre-wrap'
  })
  root.append(panel)
}

window.addEventListener('error', (event) => renderStartupError(event.error ?? event.message))
window.addEventListener('unhandledrejection', (event) => renderStartupError(event.reason))

void import('./main').catch(renderStartupError)
import { startupErrorTitle } from './bootstrapMessages'
