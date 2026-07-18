import { useEffect, useRef, useState } from 'react'
import type { AiDoctorSnapshot } from '@/shared/contracts/ai'

export function useAiInventory() {
  const [snapshot, setSnapshot] = useState<AiDoctorSnapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const scanGeneration = useRef(0)

  async function scan(projectRoot = snapshot?.projectRoot): Promise<void> {
    const generation = ++scanGeneration.current
    setLoading(true)
    setError('')
    try {
      const nextSnapshot = await window.mootool.scanAiEnvironment(projectRoot ? { projectRoot } : undefined)
      if (generation === scanGeneration.current) setSnapshot(nextSnapshot)
    } catch (scanError) {
      if (generation === scanGeneration.current) setError(scanError instanceof Error ? scanError.message : String(scanError))
    } finally {
      if (generation === scanGeneration.current) setLoading(false)
    }
  }

  async function chooseProject(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(snapshot?.projectRoot)
    if (selected) await scan(selected)
  }

  useEffect(() => {
    void scan()
    return () => { scanGeneration.current += 1 }
  }, [])

  return { snapshot, loading, error, scan, chooseProject }
}
