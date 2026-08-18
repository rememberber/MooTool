import { useCallback, useEffect, useRef, useState } from 'react'
import { toolWebviewApis } from '../../platform/api/toolWebviewApi'
import {
  closedToolWebviewSnapshot,
  type ManagedToolId,
  type ToolWebviewBounds,
  type ToolWebviewSnapshot
} from '../../platform/contracts/toolWebview'

interface ToolWebviewSessionOptions {
  toolId: ManagedToolId
  active: boolean
  autoOpen?: boolean
  containerNotReadyMessage?: string
}

export function useToolWebviewSession({
  toolId,
  active,
  autoOpen = false,
  containerNotReadyMessage = 'The tool WebView container is not ready'
}: ToolWebviewSessionOptions) {
  const api = toolWebviewApis[toolId]
  const slotRef = useRef<HTMLDivElement>(null)
  const [snapshot, setSnapshot] = useState(() => closedToolWebviewSnapshot(toolId))
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')

  const readBounds = useCallback((): ToolWebviewBounds => {
    const slot = slotRef.current
    if (!slot) {
      throw new Error(containerNotReadyMessage)
    }
    const bounds = slot.getBoundingClientRect()
    return {
      x: Math.round(bounds.x),
      y: Math.round(bounds.y),
      width: Math.round(bounds.width),
      height: Math.round(bounds.height)
    }
  }, [containerNotReadyMessage])

  const run = useCallback(async (
    label: string,
    operation: () => Promise<ToolWebviewSnapshot>
  ): Promise<void> => {
    setBusy(label)
    setError('')
    try {
      setSnapshot(await operation())
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    } finally {
      setBusy('')
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    void api.getSnapshot().then(async (current) => {
      if (cancelled) return
      let next = current
      if (active && !current.exists && autoOpen) {
        next = await api.open(readBounds())
      } else if (current.exists && current.placement === 'docked') {
        if (active) {
          next = await api.updateBounds(readBounds())
          next = await api.setVisible(true)
        } else {
          next = await api.setVisible(false)
        }
      }
      if (!cancelled) setSnapshot(next)
    }).catch((cause: unknown) => {
      if (!cancelled) setError(cause instanceof Error ? cause.message : String(cause))
    })
    return () => {
      cancelled = true
    }
  }, [active, api, autoOpen, readBounds])

  useEffect(() => {
    if (!active) return
    const timer = window.setInterval(() => {
      void api.getSnapshot().then(setSnapshot).catch(() => undefined)
    }, 750)
    return () => window.clearInterval(timer)
  }, [active, api])

  useEffect(() => {
    if (!active || !slotRef.current) return
    let frame = 0
    const update = () => {
      window.cancelAnimationFrame(frame)
      frame = window.requestAnimationFrame(() => {
        void api.getSnapshot().then((current) => {
          if (current.exists && current.placement === 'docked') {
            return api.updateBounds(readBounds()).then(setSnapshot)
          }
          return undefined
        }).catch(() => undefined)
      })
    }
    const observer = new ResizeObserver(update)
    observer.observe(slotRef.current)
    window.addEventListener('resize', update)
    return () => {
      window.cancelAnimationFrame(frame)
      observer.disconnect()
      window.removeEventListener('resize', update)
    }
  }, [active, api, readBounds])

  return {
    api,
    busy,
    error,
    nativeRuntime: typeof window !== 'undefined' && Boolean(window.__TAURI_INTERNALS__),
    readBounds,
    run,
    setSnapshot,
    slotRef,
    snapshot
  }
}
