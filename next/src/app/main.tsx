import React from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import '@/shared/styles/global.css'

const rendererParams = new URLSearchParams(window.location.search)
document.documentElement.dataset.platform = window.mootool.platform
document.documentElement.dataset.window = rendererParams.get('window') ?? 'main'

createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
