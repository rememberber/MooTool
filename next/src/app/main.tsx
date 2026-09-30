import React from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import '@/shared/styles/global.css'
import '@/shared/styles/sidebar.css'
import '@/shared/styles/windowMaterial.css'
import '@/shared/styles/windowChrome.css'
import '@/shared/styles/glassControls.css'

const rendererParams = new URLSearchParams(window.location.search)
document.documentElement.dataset.platform = window.mootool.platform
document.documentElement.dataset.window = rendererParams.get('window') ?? 'main'

// Start opaque until the main process confirms the actual native material.
document.documentElement.dataset.windowMaterial = 'solid'
if (['main', 'tool'].includes(document.documentElement.dataset.window)) {
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
  let fullscreenRevision = 0
  const stopFullscreen = window.mootool.onWindowFullscreenChange((fullscreen) => {
    fullscreenRevision++
    document.documentElement.dataset.fullscreen = String(fullscreen)
  })
  void window.mootool.getWindowFullscreen().then((fullscreen) => {
    if (fullscreenRevision === 0) document.documentElement.dataset.fullscreen = String(fullscreen)
  }).catch(() => {})
  import.meta.hot?.dispose(() => {
    stopFullscreen()
    unsubscribe()
    preference.removeEventListener('change', update)
  })
}

createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)
