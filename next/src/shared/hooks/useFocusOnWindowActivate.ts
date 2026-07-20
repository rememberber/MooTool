import { useEffect, useRef } from 'react'

/** Focus a target when it becomes eligible, and again when the window is activated. */
export function useFocusOnWindowActivate(focus: () => void, enabled: boolean): void {
  const focusRef = useRef(focus)
  focusRef.current = focus

  useEffect(() => {
    if (!enabled) return

    const run = () => {
      requestAnimationFrame(() => focusRef.current())
    }

    run()
    window.addEventListener('focus', run)
    return () => window.removeEventListener('focus', run)
  }, [enabled])
}
