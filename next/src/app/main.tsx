import React from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import '@/shared/styles/global.css'

const rendererParams = new URLSearchParams(window.location.search)
document.documentElement.dataset.platform = window.mootool.platform
document.documentElement.dataset.window = rendererParams.get('window') ?? 'main'

// Start opaque until the main process confirms the actual native material.
document.documentElement.dataset.windowMaterial = 'solid'
if (document.documentElement.dataset.window === 'main') {
  let revision = 0
  const unsubscribe = window.mootool.onWindowMaterialChange((material) => {
    revision++
    document.documentElement.dataset.windowMaterial = material
  })
  const initialRevision = revision
  void window.mootool.getWindowMaterial().then((material) => {
    if (revision === initialRevision) document.documentElement.dataset.windowMaterial = material
  }).catch(() => {})
  const preference = matchMedia('(prefers-reduced-transparency: reduce), (prefers-contrast: more), (forced-colors: active)')
  const update = () => { void window.mootool.setMaterialAccessibility(preference.matches).catch(() => {}) }
  preference.addEventListener('change', update)
  update()
  import.meta.hot?.dispose(() => {
    unsubscribe()
    preference.removeEventListener('change', update)
  })
}

createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
