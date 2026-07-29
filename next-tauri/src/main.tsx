import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import { ToolProbePage } from './features/webviewLab/ToolProbePage'
import './styles.css'

const surface = new URLSearchParams(window.location.search).get('surface')

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    {surface === 'tool-probe' ? <ToolProbePage /> : <App />}
  </StrictMode>
)
