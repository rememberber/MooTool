import { useEffect, useState } from 'react'
import type { UpdateDownloadState } from '@/shared/contracts/update'

const initialState: UpdateDownloadState = {
  status: 'idle',
  installMode: 'automatic',
  version: null,
  fileName: null,
  percent: null,
  transferred: null,
  total: null,
  message: null,
  releaseNotes: null
}

export function useUpdateState(): UpdateDownloadState {
  const [state, setState] = useState(initialState)

  useEffect(() => {
    let cancelled = false
    const unsubscribe = window.mootool.onUpdateStateChange((nextState) => {
      if (!cancelled) setState(nextState)
    })
    void window.mootool.getUpdateState().then((nextState) => {
      if (!cancelled) setState(nextState)
    }).catch(() => undefined)
    return () => {
      cancelled = true
      unsubscribe()
    }
  }, [])

  return state
}
