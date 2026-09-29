import { useEffect, useRef, useState, type SetStateAction } from 'react'

/** A local write-through cache also preserves edits made while the database is loading. */
export function useToolDraft<T extends object>(kind: 'regex' | 'calculator', defaults: T) {
  const key = `mootool.${kind}.draft`
  const initial = useRef(defaults)
  const readLocal = () => {
    try { return JSON.parse(localStorage.getItem(key) ?? 'null') } catch { return null }
  }
  const [draft, setDraft] = useState<T>(() => mergeDraft(initial.current, readLocal()))
  const [ready, setReady] = useState(false)
  const revision = useRef(0)
  useEffect(() => {
    let alive = true
    const version = revision.current
    void window.mootool.getToolDraft(kind).then(value => {
      if (alive && revision.current === version) setDraft(mergeDraft(initial.current, readLocal() ?? (value ? JSON.parse(value) : null)))
    }).catch(() => undefined).finally(() => { if (alive) setReady(true) })
    return () => { alive = false }
  }, [kind])
  useEffect(() => {
    if (!ready) return
    const value = JSON.stringify(draft)
    try { localStorage.setItem(key, value) } catch { /* Database persistence remains available. */ }
    void window.mootool.saveToolDraft(kind, value).catch(() => undefined)
  }, [draft, ready, kind, key])
  function setField<K extends keyof T>(field: K, action: SetStateAction<T[K]>) {
    revision.current++
    setDraft(current => {
      const value = typeof action === 'function' ? (action as (previous: T[K]) => T[K])(current[field]) : action
      const next = { ...current, [field]: value }
      try { localStorage.setItem(key, JSON.stringify(next)) } catch { /* Database persistence remains available. */ }
      return next
    })
  }
  return { draft, setField }
}

export function mergeDraft<T extends object>(defaults: T, value: unknown): T {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return defaults
  const result = { ...defaults }
  for (const key of Object.keys(defaults) as (keyof T)[]) {
    const saved = (value as T)[key]
    const fallback = defaults[key]
    if (Array.isArray(fallback)) {
      if (Array.isArray(saved) && saved.every(item => typeof item === 'string')) result[key] = saved
    } else if (fallback && typeof fallback === 'object') result[key] = mergeDraft(fallback, saved) as T[keyof T]
    else if (typeof saved === typeof fallback) result[key] = saved
  }
  return result
}
